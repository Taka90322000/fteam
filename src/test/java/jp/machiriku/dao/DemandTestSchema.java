package jp.machiriku.dao;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.Statement;

/**
 * 需要集計・投票履歴のテスト用に、定義書の列を持つ一時テーブルと検証用データを作る。
 * 一時テーブルは検索パスの先頭に入るため、DAOの修飾なしSQLはこちらを参照し、実テーブルには触れない。
 */
final class DemandTestSchema {
    private DemandTestSchema() { }

    /** 一時テーブル(定義書の列・制約)と、集計の期待値を計算しやすい投票データを作る。 */
    static void create(Connection connection) throws Exception {
        try (Statement sql = connection.createStatement()) {
            sql.execute("CREATE TEMP TABLE users (user_id integer PRIMARY KEY) ON COMMIT PRESERVE ROWS");
            sql.execute("CREATE TEMP TABLE user_locations (location_id integer PRIMARY KEY, user_id integer NOT NULL REFERENCES pg_temp.users(user_id), "
                    + "UNIQUE (location_id, user_id)) ON COMMIT PRESERVE ROWS");
            sql.execute("CREATE TEMP TABLE industry_categories (category_id integer PRIMARY KEY, category_code varchar(3) NOT NULL, "
                    + "category_name varchar(50) NOT NULL) ON COMMIT PRESERVE ROWS");
            sql.execute("CREATE TEMP TABLE industry_types (industry_type_id integer PRIMARY KEY, category_id integer NOT NULL REFERENCES pg_temp.industry_categories(category_id), "
                    + "industry_code varchar(10) NOT NULL UNIQUE, industry_name varchar(50) NOT NULL) ON COMMIT PRESERVE ROWS");
            sql.execute("CREATE TEMP TABLE properties (property_id integer PRIMARY KEY, property_name varchar(150) NOT NULL, prefecture varchar(10) NOT NULL, "
                    + "municipality varchar(50) NOT NULL, address_line varchar(200) NOT NULL, listing_status varchar(16) NOT NULL, "
                    + "distance_scale_km numeric(8,3) NOT NULL DEFAULT 10, deleted_at timestamptz, admin_hidden_at timestamptz) ON COMMIT PRESERVE ROWS");
            sql.execute("CREATE TEMP TABLE property_votes (vote_id integer PRIMARY KEY, property_id integer NOT NULL REFERENCES pg_temp.properties(property_id), "
                    + "resident_user_id integer NOT NULL, location_id integer NOT NULL, distance_km numeric(10,3) NOT NULL CHECK (distance_km >= 0), "
                    + "distance_scale_km numeric(8,3) NOT NULL CHECK (distance_scale_km > 0), visit_frequency varchar(30), request_comment text, "
                    + "created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP, withdrawn_at timestamptz, "
                    + "UNIQUE (property_id, resident_user_id), FOREIGN KEY (location_id, resident_user_id) REFERENCES pg_temp.user_locations(location_id, user_id)) ON COMMIT PRESERVE ROWS");
            sql.execute("CREATE TEMP TABLE vote_choices (vote_id integer NOT NULL REFERENCES pg_temp.property_votes(vote_id), "
                    + "preference_rank smallint NOT NULL CHECK (preference_rank BETWEEN 1 AND 3), industry_type_id integer NOT NULL REFERENCES pg_temp.industry_types(industry_type_id), "
                    + "rank_weight numeric(3,2) NOT NULL CHECK (rank_weight IN (1.00, 0.60, 0.30)), PRIMARY KEY (vote_id, preference_rank), UNIQUE (vote_id, industry_type_id)) ON COMMIT PRESERVE ROWS");

            sql.execute("INSERT INTO users VALUES (1),(2),(3),(4),(5),(6)");
            sql.execute("INSERT INTO user_locations SELECT 100+user_id, user_id FROM users");
            sql.execute("INSERT INTO industry_categories VALUES (1,'C01','飲食'),(2,'C02','小売')");
            sql.execute("INSERT INTO industry_types VALUES (10,1,'I0102','カフェ'),(11,1,'I0104','食堂'),(12,2,'I0301','雑貨店')");
            // 物件1・2: 宇都宮市の掲載中。3: 足利市の掲載中。4: 削除済み。5: 運営が非表示。7: 下書き。
            sql.execute("INSERT INTO properties (property_id, property_name, prefecture, municipality, address_line, listing_status, deleted_at, admin_hidden_at) VALUES "
                    + "(1,'駅前の空き店舗','栃木県','宇都宮市','宇都宮市1-1','OPEN',NULL,NULL),"
                    + "(2,'大通りの角地','栃木県','宇都宮市','宇都宮市2-2','OPEN',NULL,NULL),"
                    + "(3,'足利の路面店','栃木県','足利市','足利市3-3','OPEN',NULL,NULL),"
                    + "(4,'削除された物件','栃木県','宇都宮市','宇都宮市4-4','OPEN',now(),NULL),"
                    + "(5,'非表示の物件','栃木県','宇都宮市','宇都宮市5-5','OPEN',NULL,now()),"
                    + "(7,'下書きの物件','栃木県','宇都宮市','宇都宮市7-7','DRAFT',NULL,NULL)");

            // 距離0・尺度10なら係数1.0、距離10・尺度10なら係数0.5。
            vote(sql, 1, 1, 1, 0, "週1回程度", "駅前に欲しい", "2026-11-10 10:00+00", null, 10, 11);          // 物件1・住民1: カフェ1.00+食堂0.60
            vote(sql, 2, 1, 2, 10, "月1回以下", "静かな食堂希望", "2026-11-11 10:00+00", null, 11, 10);       // 物件1・住民2: 食堂0.50+カフェ0.30
            vote(sql, 3, 1, 3, 0, "週1回程度", "取消した人のコメント", "2026-11-12 10:00+00", "2026-11-13 10:00+00", 10); // 取消済みは集計に入らない
            vote(sql, 4, 2, 1, 0, null, null, "2026-11-09 10:00+00", null, 12);                              // 物件2・住民1: 雑貨店1.00
            vote(sql, 5, 3, 4, 0, "毎日", "  ", "2026-11-08 10:00+00", null, 10);                           // 物件3・住民4: カフェ1.00
            vote(sql, 6, 4, 5, 0, null, null, "2026-11-07 10:00+00", null, 10);                              // 削除済み物件
            vote(sql, 7, 5, 6, 0, null, null, "2026-11-06 10:00+00", null, 10);                              // 非表示の物件
            vote(sql, 8, 3, 1, 0, null, "取消した投票", "2026-11-05 10:00+00", "2026-11-05 11:00+00", 10);     // 住民1が足利の物件に投票して取消
        }
    }

    /** 投票1件と、その希望明細(引数の小分類IDを順に第1〜第3希望)を入れる。 */
    private static void vote(Statement sql, int voteId, int propertyId, int userId, double distanceKm, String frequency, String comment,
            String updatedAt, String withdrawnAt, int... typeIds) throws Exception {
        sql.execute("INSERT INTO property_votes (vote_id, property_id, resident_user_id, location_id, distance_km, distance_scale_km, "
                + "visit_frequency, request_comment, created_at, updated_at, withdrawn_at) VALUES (" + voteId + "," + propertyId + "," + userId + ","
                + (100 + userId) + "," + distanceKm + ",10," + literal(frequency) + "," + literal(comment) + ",'" + updatedAt + "','" + updatedAt + "',"
                + (withdrawnAt == null ? "NULL" : "'" + withdrawnAt + "'") + ")");
        String[] weights = {"1.00", "0.60", "0.30"};
        for (int i = 0; i < typeIds.length; i++) {
            sql.execute("INSERT INTO vote_choices VALUES (" + voteId + "," + (i + 1) + "," + typeIds[i] + "," + weights[i] + ")");
        }
    }

    private static String literal(String value) { return value == null ? "NULL" : "'" + value + "'"; }

    /** close を無視する接続。テストの一時テーブルを、DAOが接続を閉じても残すために使う。 */
    static Connection borrowed(Connection connection) {
        return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(), new Class<?>[] {Connection.class},
                (proxy, method, arguments) -> {
                    if (method.getName().equals("close")) return null;
                    try { return method.invoke(connection, arguments); }
                    catch (InvocationTargetException e) { throw e.getCause(); }
                });
    }
}
