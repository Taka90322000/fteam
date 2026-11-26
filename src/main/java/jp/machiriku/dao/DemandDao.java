package jp.machiriku.dao;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import jp.machiriku.bean.DemandReport;
import jp.machiriku.bean.DemandReport.CategoryDemand;
import jp.machiriku.bean.DemandReport.DemandComment;
import jp.machiriku.bean.DemandReport.FrequencyCount;
import jp.machiriku.bean.DemandReport.IndustryDemand;
import jp.machiriku.bean.DemandReport.TopProperty;

/**
 * 物件投票の需要集計(読み取りのみ)。有効な投票(withdrawn_at が空)だけを数え、
 * 明細スコア = rank_weight / (1 + distance_km / distance_scale_km) を業種ごとに合計する(定義書「設計補足」)。
 * 削除済み・運営が非表示にした物件は集計に含めない。
 */
public class DemandDao extends Dao {
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    protected Connection openConnection() throws SQLException { return getConnection(); }

    /**
     * 物件1件の需要。物件がない・削除済み・運営が非表示のときは空。
     *
     * @param industryLimit 業種ランキングの最大件数
     * @param commentLimit 返す要望コメントの最大件数(0なら返さない)
     */
    public Optional<DemandReport> propertyDemand(int propertyId, int industryLimit, int commentLimit) throws SQLException {
        Scope scope = new Scope("p.property_id=? AND " + VISIBLE, List.of(propertyId));
        try (Connection connection = openConnection()) {
            String name;
            String prefecture;
            String municipality;
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT p.property_name, p.prefecture, p.municipality FROM properties p WHERE " + scope.where)) {
                bind(statement, scope.params);
                try (ResultSet rows = statement.executeQuery()) {
                    if (!rows.next()) { return Optional.empty(); }
                    name = rows.getString(1);
                    prefecture = rows.getString(2);
                    municipality = rows.getString(3);
                }
            }
            Totals totals = totals(connection, scope);
            List<DemandComment> comments = commentLimit > 0 ? comments(connection, scope, commentLimit) : List.of();
            return Optional.of(new DemandReport(DemandReport.SCOPE_PROPERTY, propertyId, name, prefecture, municipality,
                    1, totals.voteCount, totals.voterCount, totals.averageDistanceKm, scaled(totals.totalScore),
                    industries(connection, scope, totals.totalScore, industryLimit), categories(connection, scope, totals.totalScore),
                    frequencies(connection, scope), comments, List.of()));
        }
    }

    /**
     * エリア全体の需要(掲載中の物件が対象)。prefecture・municipality が null なら、その条件では絞らない。
     *
     * @param topPropertyLimit 需要の高い物件の最大件数(0なら返さない)
     */
    public DemandReport areaDemand(String prefecture, String municipality, int industryLimit, int topPropertyLimit) throws SQLException {
        StringBuilder where = new StringBuilder("p.listing_status='OPEN' AND " + VISIBLE);
        List<Object> params = new ArrayList<>();
        if (prefecture != null) { where.append(" AND p.prefecture=?"); params.add(prefecture); }
        if (municipality != null) { where.append(" AND p.municipality=?"); params.add(municipality); }
        Scope scope = new Scope(where.toString(), params);
        try (Connection connection = openConnection()) {
            Totals totals = totals(connection, scope);
            return new DemandReport(DemandReport.SCOPE_AREA, null, null, prefecture, municipality,
                    totals.propertyCount, totals.voteCount, totals.voterCount, totals.averageDistanceKm, scaled(totals.totalScore),
                    industries(connection, scope, totals.totalScore, industryLimit), categories(connection, scope, totals.totalScore),
                    frequencies(connection, scope), List.of(),
                    topPropertyLimit > 0 ? topProperties(connection, scope, topPropertyLimit) : List.of());
        }
    }

    /**
     * 複数の物件の投票数と需要の要約(物件一覧用)。削除済み・運営が非表示の物件、存在しない物件は結果に含めない。
     * 投票がない物件は voteCount が0で、topIndustry は null。結果は物件IDの昇順。
     */
    public List<DemandReport.PropertySummary> summaries(List<Integer> propertyIds) throws SQLException {
        if (propertyIds == null || propertyIds.isEmpty()) { return List.of(); }
        try (Connection connection = openConnection()) {
            Scope scope = new Scope("p.property_id = ANY(?) AND " + VISIBLE,
                    List.of(connection.createArrayOf("integer", propertyIds.toArray(Integer[]::new))));
            List<int[]> counts = new ArrayList<>();
            List<BigDecimal> scores = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement(cte(scope)
                    + "SELECT s.property_id, COUNT(DISTINCT v.vote_id), COALESCE(SUM(sc.score), 0) FROM scoped s "
                    + "LEFT JOIN votes v ON v.property_id=s.property_id LEFT JOIN scores sc ON sc.vote_id=v.vote_id "
                    + "GROUP BY s.property_id ORDER BY s.property_id")) {
                bind(statement, scope.params);
                try (ResultSet rows = statement.executeQuery()) {
                    while (rows.next()) {
                        counts.add(new int[] {rows.getInt(1), rows.getInt(2)});
                        scores.add(rows.getBigDecimal(3));
                    }
                }
            }
            java.util.Map<Integer, String[]> top = new java.util.HashMap<>();
            try (PreparedStatement statement = connection.prepareStatement(cte(scope)
                    + "SELECT DISTINCT ON (x.property_id) x.property_id, t.industry_code, t.industry_name FROM "
                    + "(SELECT property_id, industry_type_id, SUM(score) AS total FROM scores GROUP BY property_id, industry_type_id) x "
                    + "JOIN industry_types t ON t.industry_type_id=x.industry_type_id ORDER BY x.property_id, x.total DESC, x.industry_type_id")) {
                bind(statement, scope.params);
                try (ResultSet rows = statement.executeQuery()) {
                    while (rows.next()) { top.put(rows.getInt(1), new String[] {rows.getString(2), rows.getString(3)}); }
                }
            }
            List<DemandReport.PropertySummary> result = new ArrayList<>();
            for (int i = 0; i < counts.size(); i++) {
                String[] industry = top.get(counts.get(i)[0]);
                result.add(new DemandReport.PropertySummary(counts.get(i)[0], counts.get(i)[1], scaled(scores.get(i)),
                        industry == null ? null : industry[0], industry == null ? null : industry[1]));
            }
            return result;
        }
    }

    // ---- 集計のSQL ------------------------------------------------------------------------------

    /**
     * 投票できる物件として扱う条件。削除済みと、運営が非表示にした物件(admin_hidden_at)を除く。
     * admin_hidden_at 列がまだない環境でも動くよう、列を直接書かずに行をJSONにして読む。
     */
    private static final String VISIBLE = "p.deleted_at IS NULL AND to_jsonb(p)->>'admin_hidden_at' IS NULL";

    private record Scope(String where, List<Object> params) { }

    private record Totals(int propertyCount, int voteCount, int voterCount, BigDecimal averageDistanceKm, BigDecimal totalScore) { }

    /** 対象物件(scoped)→有効投票(votes)→投票明細ごとのスコア(scores)の共通部分。 */
    private static String cte(Scope scope) {
        return "WITH scoped AS (SELECT p.property_id, p.property_name, p.municipality FROM properties p WHERE " + scope.where + "), "
                + "votes AS (SELECT v.vote_id, v.property_id, v.resident_user_id, v.distance_km, v.visit_frequency, "
                + "v.request_comment, v.updated_at FROM property_votes v JOIN scoped s ON s.property_id=v.property_id "
                + "WHERE v.withdrawn_at IS NULL), "
                + "scores AS (SELECT v.property_id, v.vote_id, v.resident_user_id, c.industry_type_id, c.preference_rank, "
                + "c.rank_weight / (1 + v.distance_km / pv.distance_scale_km) AS score "
                + "FROM votes v JOIN property_votes pv ON pv.vote_id=v.vote_id JOIN vote_choices c ON c.vote_id=v.vote_id) ";
    }

    private static Totals totals(Connection connection, Scope scope) throws SQLException {
        String sql = cte(scope) + "SELECT (SELECT COUNT(*) FROM scoped), (SELECT COUNT(*) FROM votes), "
                + "(SELECT COUNT(DISTINCT resident_user_id) FROM votes), (SELECT AVG(distance_km) FROM votes), "
                + "(SELECT COALESCE(SUM(score), 0) FROM scores)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, scope.params);
            try (ResultSet rows = statement.executeQuery()) {
                rows.next();
                BigDecimal average = rows.getBigDecimal(4);
                return new Totals(rows.getInt(1), rows.getInt(2), rows.getInt(3),
                        average == null ? null : average.setScale(3, RoundingMode.HALF_UP), rows.getBigDecimal(5));
            }
        }
    }

    private static List<IndustryDemand> industries(Connection connection, Scope scope, BigDecimal totalScore, int limit) throws SQLException {
        String sql = cte(scope) + "SELECT t.industry_type_id, t.industry_code, t.industry_name, SUM(s.score) AS total, "
                + "COUNT(DISTINCT s.resident_user_id), COUNT(*) FILTER (WHERE s.preference_rank=1), "
                + "COUNT(*) FILTER (WHERE s.preference_rank=2), COUNT(*) FILTER (WHERE s.preference_rank=3) "
                + "FROM scores s JOIN industry_types t ON t.industry_type_id=s.industry_type_id "
                + "GROUP BY t.industry_type_id, t.industry_code, t.industry_name ORDER BY total DESC, t.industry_type_id LIMIT ?";
        List<IndustryDemand> result = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, scope.params, limit);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    BigDecimal score = rows.getBigDecimal(4);
                    result.add(new IndustryDemand(result.size() + 1, rows.getInt(1), rows.getString(2), rows.getString(3),
                            scaled(score), share(score, totalScore), rows.getInt(5), rows.getInt(6), rows.getInt(7), rows.getInt(8)));
                }
            }
        }
        return result;
    }

    private static List<CategoryDemand> categories(Connection connection, Scope scope, BigDecimal totalScore) throws SQLException {
        String sql = cte(scope) + "SELECT c.category_code, c.category_name, SUM(s.score) AS total, COUNT(DISTINCT s.resident_user_id) "
                + "FROM scores s JOIN industry_types t ON t.industry_type_id=s.industry_type_id "
                + "JOIN industry_categories c ON c.category_id=t.category_id "
                + "GROUP BY c.category_id, c.category_code, c.category_name ORDER BY total DESC, c.category_id";
        List<CategoryDemand> result = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, scope.params);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    BigDecimal score = rows.getBigDecimal(3);
                    result.add(new CategoryDemand(result.size() + 1, rows.getString(1), rows.getString(2),
                            scaled(score), share(score, totalScore), rows.getInt(4)));
                }
            }
        }
        return result;
    }

    private static List<FrequencyCount> frequencies(Connection connection, Scope scope) throws SQLException {
        String sql = cte(scope) + "SELECT visit_frequency, COUNT(*) AS n FROM votes WHERE visit_frequency IS NOT NULL "
                + "GROUP BY visit_frequency ORDER BY n DESC, visit_frequency LIMIT 10";
        List<FrequencyCount> result = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, scope.params);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) { result.add(new FrequencyCount(rows.getString(1), rows.getInt(2))); }
            }
        }
        return result;
    }

    private static List<DemandComment> comments(Connection connection, Scope scope, int limit) throws SQLException {
        String sql = cte(scope) + "SELECT request_comment, visit_frequency, updated_at FROM votes "
                + "WHERE request_comment IS NOT NULL AND btrim(request_comment) <> '' ORDER BY updated_at DESC, vote_id DESC LIMIT ?";
        List<DemandComment> result = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, scope.params, limit);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    Timestamp at = rows.getTimestamp(3);
                    result.add(new DemandComment(rows.getString(1), rows.getString(2),
                            at == null ? null : at.toInstant().atOffset(ZoneOffset.UTC)));
                }
            }
        }
        return result;
    }

    private static List<TopProperty> topProperties(Connection connection, Scope scope, int limit) throws SQLException {
        String sql = cte(scope) + "SELECT s.property_id, s.property_name, s.municipality, COUNT(DISTINCT v.vote_id), SUM(sc.score) AS total "
                + "FROM scoped s JOIN votes v ON v.property_id=s.property_id JOIN scores sc ON sc.vote_id=v.vote_id "
                + "GROUP BY s.property_id, s.property_name, s.municipality ORDER BY total DESC, s.property_id LIMIT ?";
        List<Object[]> found = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, scope.params, limit);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    found.add(new Object[] {rows.getInt(1), rows.getString(2), rows.getString(3), rows.getInt(4), rows.getBigDecimal(5)});
                }
            }
        }
        if (found.isEmpty()) { return List.of(); }

        // 物件ごとに、スコアが最も高い業種の名前を1回の問い合わせで取得する。
        Integer[] ids = found.stream().map(row -> (Integer) row[0]).toArray(Integer[]::new);
        java.util.Map<Integer, String> topIndustry = new java.util.HashMap<>();
        String topSql = cte(scope) + "SELECT DISTINCT ON (x.property_id) x.property_id, t.industry_name FROM "
                + "(SELECT property_id, industry_type_id, SUM(score) AS total FROM scores WHERE property_id = ANY(?) "
                + "GROUP BY property_id, industry_type_id) x JOIN industry_types t ON t.industry_type_id=x.industry_type_id "
                + "ORDER BY x.property_id, x.total DESC, x.industry_type_id";
        try (PreparedStatement statement = connection.prepareStatement(topSql)) {
            bind(statement, scope.params, connection.createArrayOf("integer", ids));
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) { topIndustry.put(rows.getInt(1), rows.getString(2)); }
            }
        }
        List<TopProperty> result = new ArrayList<>();
        for (Object[] row : found) {
            result.add(new TopProperty(result.size() + 1, (Integer) row[0], (String) row[1], (String) row[2],
                    (Integer) row[3], scaled((BigDecimal) row[4]), topIndustry.get((Integer) row[0])));
        }
        return result;
    }

    // ---- 小物 -----------------------------------------------------------------------------------

    private static void bind(PreparedStatement statement, List<Object> params, Object... extra) throws SQLException {
        int index = 1;
        for (Object value : params) { statement.setObject(index++, value); }
        for (Object value : extra) { statement.setObject(index++, value); }
    }

    /** スコアは表示用に小数第3位で丸める(丸めは表示時に行う、という設計補足の初期案)。 */
    private static BigDecimal scaled(BigDecimal value) {
        return value.setScale(3, RoundingMode.HALF_UP);
    }

    /** 全体に対する割合(%)。小数第1位まで。全体が0なら0。 */
    private static BigDecimal share(BigDecimal part, BigDecimal whole) {
        if (whole == null || whole.signum() == 0) { return BigDecimal.ZERO.setScale(1); }
        return part.multiply(HUNDRED).divide(whole, 1, RoundingMode.HALF_UP);
    }
}
