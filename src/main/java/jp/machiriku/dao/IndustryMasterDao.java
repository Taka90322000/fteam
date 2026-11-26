package jp.machiriku.dao;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jp.machiriku.bean.IndustryMaster;

/** 業種マスタ(有効な大分類と、その中の有効な小分類)を表示順に返す。読み取りのみ。 */
public class IndustryMasterDao extends Dao {
    protected Connection openConnection() throws SQLException { return getConnection(); }

    public IndustryMaster findActive() throws SQLException {
        // display_order 列がない古い構造でも動くよう、列を直接書かずにJSONから読む。ない場合はIDの順になる。
        String sql = "SELECT c.category_id, c.category_code, c.category_name, t.industry_type_id, t.industry_code, t.industry_name "
                + "FROM industry_categories c JOIN industry_types t ON t.category_id=c.category_id "
                + "WHERE c.is_active AND t.is_active "
                + "ORDER BY COALESCE((to_jsonb(c)->>'display_order')::int, c.category_id), c.category_id, "
                + "COALESCE((to_jsonb(t)->>'display_order')::int, t.industry_type_id), t.industry_type_id";
        Map<Integer, List<IndustryMaster.Industry>> byCategory = new LinkedHashMap<>();
        Map<Integer, String[]> names = new LinkedHashMap<>();
        try (Connection connection = openConnection(); Statement statement = connection.createStatement();
                ResultSet rows = statement.executeQuery(sql)) {
            while (rows.next()) {
                int categoryId = rows.getInt(1);
                names.putIfAbsent(categoryId, new String[] {rows.getString(2), rows.getString(3)});
                byCategory.computeIfAbsent(categoryId, key -> new ArrayList<>())
                        .add(new IndustryMaster.Industry(rows.getInt(4), rows.getString(5), rows.getString(6)));
            }
        }
        List<IndustryMaster.Category> categories = new ArrayList<>();
        byCategory.forEach((categoryId, industries) ->
                categories.add(new IndustryMaster.Category(categoryId, names.get(categoryId)[0], names.get(categoryId)[1], industries)));
        return new IndustryMaster(categories);
    }
}
