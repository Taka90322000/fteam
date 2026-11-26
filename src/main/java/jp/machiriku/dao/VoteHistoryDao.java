package jp.machiriku.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jp.machiriku.bean.PropertyVote;
import jp.machiriku.bean.VoteChoice;
import jp.machiriku.bean.VoteHistoryPage;

/** 自分の投票履歴(取消済みを含む)を新しい順に返す。読み取りのみ。 */
public class VoteHistoryDao extends Dao {
    /** 履歴の絞り込み。ACTIVE=有効な投票、WITHDRAWN=取消済み、ALL=すべて。 */
    public enum Status { ACTIVE, WITHDRAWN, ALL }

    protected Connection openConnection() throws SQLException { return getConnection(); }

    public VoteHistoryPage list(int userId, Status status, int limit, int offset) throws SQLException {
        String filter = switch (status) {
            case ACTIVE -> " AND pv.withdrawn_at IS NULL";
            case WITHDRAWN -> " AND pv.withdrawn_at IS NOT NULL";
            case ALL -> "";
        };
        try (Connection connection = openConnection()) {
            int total;
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT COUNT(*) FROM property_votes pv WHERE pv.resident_user_id=?" + filter)) {
                statement.setInt(1, userId);
                try (ResultSet rows = statement.executeQuery()) {
                    rows.next();
                    total = rows.getInt(1);
                }
            }

            List<VoteHistoryPage.Item> items = new ArrayList<>();
            Map<Integer, PropertyVote> votesById = new HashMap<>();
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT pv.vote_id, pv.property_id, pv.location_id, pv.distance_km, pv.distance_scale_km, pv.visit_frequency, "
                    + "pv.request_comment, pv.created_at, pv.updated_at, pv.withdrawn_at, "
                    + "p.property_name, p.prefecture, p.municipality, p.address_line, p.listing_status, p.deleted_at "
                    + "FROM property_votes pv JOIN properties p ON p.property_id=pv.property_id "
                    + "WHERE pv.resident_user_id=?" + filter + " ORDER BY pv.updated_at DESC, pv.vote_id DESC LIMIT ? OFFSET ?")) {
                statement.setInt(1, userId);
                statement.setInt(2, limit);
                statement.setInt(3, offset);
                try (ResultSet rows = statement.executeQuery()) {
                    while (rows.next()) {
                        PropertyVote vote = new PropertyVote();
                        vote.setVoteId(rows.getInt(1));
                        vote.setPropertyId(rows.getInt(2));
                        vote.setResidentUserId(userId);
                        vote.setLocationId(rows.getInt(3));
                        vote.setDistanceKm(rows.getBigDecimal(4));
                        vote.setDistanceScaleKm(rows.getBigDecimal(5));
                        vote.setVisitFrequency(rows.getString(6));
                        vote.setRequestComment(rows.getString(7));
                        vote.setUpdatedAt(toOffset(rows.getTimestamp(9)));
                        vote.setWithdrawnAt(toOffset(rows.getTimestamp(10)));
                        votesById.put(vote.getVoteId(), vote);
                        items.add(new VoteHistoryPage.Item(vote, rows.getString(11), rows.getString(12), rows.getString(13),
                                rows.getString(14), rows.getString(15), rows.getTimestamp(16) != null, toOffset(rows.getTimestamp(8))));
                    }
                }
            }

            if (!votesById.isEmpty()) {
                Integer[] ids = votesById.keySet().toArray(Integer[]::new);
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT c.vote_id, c.preference_rank, c.industry_type_id, t.industry_code, t.industry_name, c.rank_weight "
                        + "FROM vote_choices c JOIN industry_types t ON t.industry_type_id=c.industry_type_id "
                        + "WHERE c.vote_id = ANY(?) ORDER BY c.vote_id, c.preference_rank")) {
                    statement.setArray(1, connection.createArrayOf("integer", ids));
                    try (ResultSet rows = statement.executeQuery()) {
                        while (rows.next()) {
                            VoteChoice choice = new VoteChoice();
                            choice.setPreferenceRank(rows.getInt(2));
                            choice.setIndustryTypeId(rows.getInt(3));
                            choice.setIndustryCode(rows.getString(4));
                            choice.setIndustryName(rows.getString(5));
                            choice.setRankWeight(rows.getBigDecimal(6));
                            votesById.get(rows.getInt(1)).getChoices().add(choice);
                        }
                    }
                }
            }
            return new VoteHistoryPage(total, limit, offset, items);
        }
    }

    private static OffsetDateTime toOffset(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant().atOffset(ZoneOffset.UTC);
    }
}
