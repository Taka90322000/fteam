package jp.machiriku.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Set;

import jp.machiriku.bean.User;

/** 会員情報の保存・取得を行う。ログインの識別にはメールではなくLINEのsubjectを使う。 */
public class UserDao extends Dao {
    private static final String COLUMNS = "user_id, display_name, email, user_role, "
            + "line_subject, account_status, created_at, updated_at, withdrawn_at";
    private static final Set<String> ROLES = Set.of("RESIDENT", "BUSINESS", "OWNER");

    /** 共通の接続処理を使う。テストでは一時テーブル用の接続に差し替えられる。 */
    protected Connection openConnection() throws SQLException {
        return getConnection();
    }

    /** LINEのsubjectが一致する会員を取得し、未登録の場合はnullを返す。 */
    public User findByLineSubject(String lineSubject) throws SQLException {
        // subjectはSQLへ連結せず、プレースホルダーに値を設定してSQL注入を防ぐ。
        try (Connection connection = openConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "SELECT " + COLUMNS + " FROM users WHERE line_subject = ?")) {
            statement.setString(1, lineSubject);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? readUser(rows) : null;
            }
        }
    }

    /** 新規会員を登録し、DBが採番・設定した値を含む保存後の会員情報を返す。 */
    public User insert(User user) throws SQLException {
        validate(user);
        // 会員ID、PENDING状態、作成・更新日時は既存DBの自動採番・デフォルト値に任せる。
        try (Connection connection = openConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO users (display_name, email, user_role, line_subject) "
                        + "VALUES (?, ?, ?, ?) RETURNING " + COLUMNS)) {
            statement.setString(1, user.getDisplayName());
            // メールは任意。未取得や空白の場合は空文字でなくSQLのNULLとして保存する。
            String email = user.getEmail();
            statement.setString(2, email == null || email.isBlank() ? null : email);
            statement.setString(3, user.getUserRole());
            statement.setString(4, user.getLineSubject());
            try (ResultSet rows = statement.executeQuery()) {
                if (!rows.next()) { throw new SQLException("User insertion returned no record."); }
                return readUser(rows);
            }
        }
    }

    /** DB制約に合わせて必須項目・文字数・公開登録用の利用区分を確認する。 */
    private static void validate(User user) {
        if (user == null || !validText(user.getDisplayName(), 100)
                || (user.getEmail() != null && !user.getEmail().isBlank() && !validText(user.getEmail(), 254))
                || !validText(user.getLineSubject(), 255)
                || user.getUserRole() == null || !ROLES.contains(user.getUserRole())) {
            throw new IllegalArgumentException("User registration fields are invalid.");
        }
    }

    // サロゲートペアを2文字と数えず、DBの文字数制限に合わせてコードポイント単位で確認する。
    private static boolean validText(String value, int maximum) {
        return value != null && !value.isBlank() && value.codePointCount(0, value.length()) <= maximum;
    }

    /** DBの列をUserへ詰め替え、保存済みの役割・利用状態も呼び出し元へ返す。 */
    static User readUser(ResultSet row) throws SQLException {
        User user = new User();
        user.setUserId(row.getInt("user_id"));
        user.setDisplayName(row.getString("display_name"));
        user.setEmail(row.getString("email"));
        user.setUserRole(row.getString("user_role"));
        user.setLineSubject(row.getString("line_subject"));
        user.setAccountStatus(row.getString("account_status"));
        user.setCreatedAt(row.getObject("created_at", OffsetDateTime.class));
        user.setUpdatedAt(row.getObject("updated_at", OffsetDateTime.class));
        user.setWithdrawnAt(row.getObject("withdrawn_at", OffsetDateTime.class));
        return user;
    }
}
