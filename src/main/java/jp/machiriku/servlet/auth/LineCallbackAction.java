package jp.machiriku.servlet.auth;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Locale;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jp.machiriku.auth.LineLoginClient;
import jp.machiriku.auth.LineLoginClient.Profile;
import jp.machiriku.auth.LineLoginConfig;
import jp.machiriku.bean.User;
import jp.machiriku.dao.UserDao;
import jp.machiriku.tool.Action;

/** LINEからの認証結果を検証し、会員登録または既存会員のログインを完了する。 */
public class LineCallbackAction extends Action {
    private static final String UNIQUE_VIOLATION = "23505"; // DBの一意制約違反を表すSQLSTATE。

    @Override
    public void execute(HttpServletRequest req, HttpServletResponse res) throws Exception {
        // URLに含まれる認可コードを、キャッシュやリファラーに残さない。
        res.setHeader("Cache-Control", "no-store");
        res.setHeader("Referrer-Policy", "no-referrer");

        // LINEからのコールバックはGETだけを受け付ける。
        if (!"GET".equals(req.getMethod())) {
            res.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }

        // 開始時のstateと期限を確認し、一致した認証情報を一度だけ取り出す。
        // attemptは照合に成功した認証情報。期限切れや不一致ならnull。
        LineLoginSupport.Attempt attempt = LineLoginSupport.consume(req.getSession(false), req.getParameter("state"));
        if (attempt == null) {
            LineLoginSupport.error(req, res, false, "認証の有効期限が切れたか、認証情報が一致しません。ログインからやり直してください。");
            return;
        }

        // 開始時の利用区分で登録経由かを判定し、LINEからのエラーを処理する。
        String registrationRole = attempt.registrationRole(); // 認証開始時に保存した利用区分。
        boolean registration = registrationRole != null; // 新規登録として開始した認証かどうか。
        if (req.getParameter("error") != null) {
            LineLoginSupport.error(req, res, registration, "LINEでの認証がキャンセルされました。もう一度お試しください。");
            return;
        }

        // 本人確認に使う認可コードが届いているか確認する。
        String code = req.getParameter("code"); // LINEがコールバックに付けた認可コード。
        if (code == null || code.isBlank()) {
            LineLoginSupport.error(req, res, registration, "認証コードを取得できませんでした。もう一度お試しください。");
            return;
        }

        try {
            // 認可コードを交換し、検証済みのLINEプロフィールを取得する。
            // プロフィールにはLINEの識別子、表示名、任意のメールが入る。
            Profile profile = createClient().authenticate(code, attempt.nonce(), attempt.codeVerifier());

            // LINEのユーザー識別子（sub）で会員を検索する。
            UserDao dao = createUserDao(); // 会員の検索・登録・更新に使うDAO。
            User member = dao.findByLineSubject(profile.lineSubject()); // 照合した会員。未登録ならnull。

            // ログイン経由で会員が見つからなければ、登録画面へ案内する。
            if (member == null && !registration) {
                LineLoginSupport.error(req, res, true,
                        "このLINEアカウントは未登録です。利用区分を選んで新規会員登録してください。");
                return;
            }
            // 登録経由の未登録者は会員を作成する。別会員とのメール重複は登録画面で案内する。
            if (member == null) {
                member = registerOrFindMember(dao, profile, registrationRole);
                if (member == null) {
                    LineLoginSupport.error(req, res, true,
                            "このメールアドレスは既に登録されています。既存の登録情報をご確認ください。");
                    return;
                }
            }

            // 登録経由の本登録前会員には、今回選んだ利用区分を反映する。
            if (registration && LineLoginSupport.available(member) && "PENDING".equals(member.getAccountStatus())) {
                String databaseRole = registrationRole.toUpperCase(Locale.ROOT); // DB用の大文字の利用区分。
                member = dao.updatePendingRole(profile.lineSubject(), databaseRole);
            }
            // 区分の更新後も、会員がログイン可能な状態か確認する。
            if (!LineLoginSupport.available(member)) {
                LineLoginSupport.error(req, res, false, "このアカウントは利用できません。管理者にお問い合わせください。");
                return;
            }

            // 確認済みの会員情報でセッションを作り直す。
            LineLoginSupport.signIn(req, member, profile.displayName(), profile.email());
            // 登録経由なら会員の状態に応じた画面へ、通常ログインならトップへ進む。
            // destinationはアプリのコンテキストパスを除いた遷移先。
            String destination = registration ? LineLoginSupport.registrationDestination(member) : "/index.jsp";
            res.sendRedirect(req.getContextPath() + destination);
        } catch (InterruptedException e) {
            // 通信中断の状態を上位の処理にも伝えられるよう、割り込みフラグを戻す。
            Thread.currentThread().interrupt();
            LineLoginSupport.error(req, res, registration, "LINEとの通信が中断されました。もう一度お試しください。");
        } catch (IOException e) {
            LineLoginSupport.error(req, res, registration,
                    "LINE認証に失敗しました。もう一度お試しください。");
        } catch (SQLException e) {
            // DBの例外詳細にはメールや電話番号が含まれる場合があるため、SQLStateだけを記録する。
            req.getServletContext().log("LINE login database operation failed (SQLState=" + e.getSQLState() + ").");
            LineLoginSupport.error(req, res, registration, "会員情報の保存・取得に失敗しました。時間をおいて再度お試しください。");
        } catch (IllegalStateException | IllegalArgumentException e) {
            LineLoginSupport.error(req, res, registration, "LINEログインの設定または登録情報を確認してください。");
        }
    }

    /** 新規会員を登録する。同じLINEユーザーの登録が先行していれば、その会員を取得する。 */
    private static User registerOrFindMember(UserDao dao, Profile profile, String registrationRole) throws SQLException {
        // LINEで確認した本人情報と、開始時に選んだ利用区分を登録用にまとめる。
        User newMember = new User(); // DBへ登録する前の会員情報。
        newMember.setDisplayName(profile.displayName());
        newMember.setEmail(profile.email());
        newMember.setUserRole(registrationRole.toUpperCase(Locale.ROOT));
        newMember.setLineSubject(profile.lineSubject());

        try {
            return dao.insert(newMember);
        } catch (SQLException e) {
            if (!UNIQUE_VIOLATION.equals(e.getSQLState())) {
                throw e;
            }
            // 別の会員とのメール重複を区別するため、本人と同じsubjectだけを検索する。
            return dao.findByLineSubject(profile.lineSubject());
        }
    }

    /** テストでは実通信しないクライアントへ差し替える。 */
    protected LineLoginClient createClient() {
        return new LineLoginClient(LineLoginConfig.fromEnvironment());
    }

    /** テストではDBを使わないDAOに差し替える。 */
    protected UserDao createUserDao() {
        return new UserDao();
    }
}
