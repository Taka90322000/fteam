package jp.machiriku.servlet.management;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jp.machiriku.bean.User;
import jp.machiriku.dao.ProfileDao;
import jp.machiriku.dao.management.ManagementDao;
import jp.machiriku.servlet.MemberSession;

/** 管理・サポートActionで共通の認可、CSRF検証、画面遷移を扱う。 */
final class ManagementActionSupport {
    /** 画面の共通処理を静的に利用する。 */
    private ManagementActionSupport() { }

    /** サポート画面ごとの閲覧・送信に必要な会員状態。 */
    enum Access {
        /** ゲストにも公開する画面。 */
        PUBLIC,
        /** 本登録前の会員も利用できる画面。 */
        MEMBER,
        /** 本登録済みの会員だけが利用できる画面。 */
        ACTIVE_MEMBER
    }

    /** 表示に必要なデータを読み込み、JSP用のモデルを組み立てる処理。 */
    @FunctionalInterface
    interface PageReader {
        /**
         * 画面のModelを取得する。
         * @param db 画面全体で共用するDAO接続。
         * @param input 検索条件や表示対象のリクエストパラメーター。
         * @param viewer 認証済みの閲覧者。公開画面のゲストはnull。
         * @return JSPで描画する画面情報。
         * @throws SQLException 必要なレコードを取得できなかった場合。
         */
        ManagementView read(ManagementDao db, ManagementRequest input, User viewer) throws SQLException;
    }

    /** フォームで指定した変更を、認可済みの会員として保存する処理。 */
    @FunctionalInterface
    interface PageWriter {
        /**
         * 入力を検証して書き込み、変更を確認できる画面のURLを返す。
         * @param db トランザクション中のDAO接続。
         * @param input CSRF検証済みのフォーム入力。
         * @param actorId 操作する会員のID。
         * @return コンテキストパスを除いたAction URL。
         * @throws SQLException 変更を保存できなかった場合。
         */
        String write(ManagementDao db, ManagementRequest input, int actorId) throws SQLException;
    }

    /**
     * LINE会員に付与された現在の管理権限を確認してから、各管理Actionの処理を実行する。
     * @param req FrontControllerから受け取ったリクエスト。
     * @param res JSPの表示または遷移に使うレスポンス。
     * @param reader 各Actionが指定する画面の表示処理。
     * @param writer 各Actionが指定する更新処理。表示専用画面はnull。
     * @throws IOException リクエストやレスポンスの入出力に失敗した場合。
     * @throws ServletException JSPを実行できなかった場合。
     */
    static void admin(HttpServletRequest req, HttpServletResponse res, PageReader reader, PageWriter writer)
            throws IOException, ServletException {
        res.setHeader("Cache-Control", "no-store");
        req.setCharacterEncoding(StandardCharsets.UTF_8.name());
        // LINEで本人確認済みのセッションだけから管理操作の会員IDを取得する。
        HttpSession session = req.getSession(false);
        User member = MemberSession.signedInUser(session);
        if (member == null) {
            if (hasDemoRole(session)) {
                res.sendError(HttpServletResponse.SC_FORBIDDEN, "管理画面には権限のあるLINE会員でログインしてください。");
            } else {
                MemberSession.redirectToLogin(req, res);
            }
            return;
        }

        // 表示と更新で共通のリクエスト入力と、DB接続を閉じた後に描画する画面モデル。
        var input = new ManagementRequest(req);
        ManagementView page;
        try (var db = ManagementDao.open()) {
            // 表示権限の付与と現在の会員状態を確認する。
            int actorId = member.getUserId();
            if (!db.isAdmin(actorId, false)) {
                res.sendError(HttpServletResponse.SC_FORBIDDEN, "このアカウントには管理権限がありません。");
                return;
            }
            // 書き込みはPOSTかつ更新を定義したActionだけで受け付ける。
            String method = req.getMethod();
            if ("POST".equals(method) && writer != null) {
                if (!input.validCsrf(session)) {
                    res.sendError(HttpServletResponse.SC_FORBIDDEN, "管理画面を開き直してから送信してください。");
                    return;
                }
                // 権限を再確認し、変更と操作履歴を確定させてから再表示する。
                String destination = db.transaction(() -> {
                    if (!db.isAdmin(actorId, true)) {
                        throw new ManagementProblem(403, "このアカウントには管理権限がありません。");
                    }
                    return writer.write(db, input, actorId);
                });
                saved(req, res, destination);
                return;
            }
            if (!"GET".equals(method) && !"HEAD".equals(method)) {
                res.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
                return;
            }
            page = reader.read(db, input, member);
        } catch (ManagementProblem error) {
            res.sendError(error.status(), error.getMessage());
            return;
        } catch (IllegalArgumentException error) {
            res.sendError(HttpServletResponse.SC_BAD_REQUEST, error.getMessage());
            return;
        } catch (SQLException error) {
            sqlError(req, res, error);
            return;
        }
        render(req, res, page, true, true);
    }

    /**
     * 公開案内の表示、または会員からの通報・問い合わせの送信を処理する。
     * @param req FrontControllerから受け取ったリクエスト。
     * @param res JSPの表示または遷移に使うレスポンス。
     * @param access この画面に必要な会員の状態。
     * @param reader 各Actionが指定する画面の表示処理。
     * @param writer 各Actionが指定する書き込み処理。表示専用画面はnull。
     * @throws IOException リクエストやレスポンスの入出力に失敗した場合。
     * @throws ServletException JSPを実行できなかった場合。
     */
    static void support(HttpServletRequest req, HttpServletResponse res, Access access, PageReader reader, PageWriter writer)
            throws IOException, ServletException {
        res.setHeader("Cache-Control", "no-store");
        req.setCharacterEncoding(StandardCharsets.UTF_8.name());
        // 更新処理と通常の閲覧をHTTPメソッドで分ける。
        String method = req.getMethod();
        if ("POST".equals(method) && writer != null) {
            submitSupport(req, res, access, writer);
            return;
        }
        if (!"GET".equals(method) && !"HEAD".equals(method)) {
            res.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }

        // 会員専用画面では現在の利用状態をDBで確認する。
        HttpSession session = req.getSession(false);
        User viewer = access == Access.PUBLIC ? MemberSession.signedInUser(session)
                : MemberSession.requireCurrentUser(req, res, session, ProfileDao::new);
        if (access != Access.PUBLIC && viewer == null) {
            return;
        }
        if (access == Access.ACTIVE_MEMBER && !"ACTIVE".equals(viewer.getAccountStatus())) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN, "通報するには本登録を完了してください。");
            return;
        }

        // DB接続は画面の読み込みにだけ使い、描画前に解放する。
        ManagementView page;
        try (var db = ManagementDao.open()) {
            page = reader.read(db, new ManagementRequest(req), viewer);
        } catch (ManagementProblem error) {
            res.sendError(error.status(), error.getMessage());
            return;
        } catch (SQLException error) {
            sqlError(req, res, error);
            return;
        }
        render(req, res, page, false, viewer != null);
    }

    /**
     * サポートのフォームを送信した会員の本人確認とCSRF検証を行い、内容を保存する。
     * @param req 通報または問い合わせのフォーム。
     * @param res 保存後の遷移に使うレスポンス。
     * @param access 送信時に必要な会員状態。
     * @param writer 通報・問い合わせを保存する処理。
     * @throws IOException レスポンスの出力に失敗した場合。
     */
    private static void submitSupport(HttpServletRequest req, HttpServletResponse res, Access access, PageWriter writer)
            throws IOException {
        // 送信者は既存のLINE会員セッションで確定する。
        HttpSession session = req.getSession(false);
        User viewer = MemberSession.signedInUser(session);
        if (viewer == null) {
            MemberSession.redirectToLogin(req, res);
            return;
        }
        // 他のセッションからの送信を拒否してからDBを開く。
        var input = new ManagementRequest(req);
        if (!input.validCsrf(session)) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN, "サポート画面を開き直してから送信してください。");
            return;
        }
        try (var db = ManagementDao.open()) {
            // 送信と同時に会員状態が変更されないようにし、受付と本文を一括保存する。
            String destination = db.transaction(() -> {
                int actorId = viewer.getUserId();
                if (!db.lockEligibleMember(actorId, access == Access.ACTIVE_MEMBER)) {
                    throw new ManagementProblem(403, "このアカウントでは操作できません。");
                }
                return writer.write(db, input, actorId);
            });
            saved(req, res, destination);
        } catch (ManagementProblem error) {
            res.sendError(error.status(), error.getMessage());
        } catch (SQLException error) {
            sqlError(req, res, error);
        }
    }

    /**
     * LINE会員情報を持たないセッションがデモ表示中か確認する。
     * @param session 現在のセッション。未作成の場合はnull。
     * @return デモ用の利用区分を保持している場合はtrue。
     */
    private static boolean hasDemoRole(HttpSession session) {
        try {
            return session != null && session.getAttribute("userRole") instanceof String;
        } catch (IllegalStateException expired) {
            // 失効したデモセッションはゲストとしてログイン画面へ案内する。
            return false;
        }
    }

    /**
     * 選択されたJSPに表示データを渡し、会員向けフォームにトークンを付ける。
     * @param req 現在のHTTPリクエスト。
     * @param res 現在のHTTPレスポンス。
     * @param page 表示先のJSPと、描画に使うデータ。
     * @param admin 管理画面を表示する場合はtrue。
     * @param member 認証済みの会員向けに表示する場合はtrue。
     * @throws ServletException JSPを実行できなかった場合。
     * @throws IOException 応答を出力できなかった場合。
     */
    private static void render(HttpServletRequest req, HttpServletResponse res, ManagementView page, boolean admin, boolean member)
            throws ServletException, IOException {
        req.setAttribute("managementTitle", page.title());
        req.setAttribute("managementModel", page.model());
        req.setAttribute("managementAdmin", admin);
        req.setAttribute("managementRoute", req.getServletPath());
        if (member && req.getSession(false) != null) {
            req.setAttribute("managementCsrf", ManagementRequest.csrfToken(req.getSession(false)));
        }
        req.getRequestDispatcher(page.path()).forward(req, res);
    }

    /**
     * 保存先をGETで開き、ブラウザーの再読み込みによる再送信を防ぐ。
     * @param req 現在のHTTPリクエスト。
     * @param res 現在のHTTPレスポンス。
     * @param path 保存処理が組み立てた同一サイトの画面パス。
     */
    private static void saved(HttpServletRequest req, HttpServletResponse res, String path) {
        res.setStatus(HttpServletResponse.SC_SEE_OTHER);
        res.setHeader("Location", req.getContextPath() + path + (path.contains("?") ? "&" : "?") + "saved=1");
    }

    /**
     * SQL文や識別情報を含めずにDBエラーを記録し、利用者に案内を返す。
     * @param req ログを書き込むための現在のリクエスト。
     * @param res エラーのHTTPレスポンス。
     * @param error DB接続またはSQL処理の例外。
     * @throws IOException エラー応答を送信できなかった場合。
     */
    private static void sqlError(HttpServletRequest req, HttpServletResponse res, SQLException error) throws IOException {
        // DAOで対象が見つからなかった場合は、存在や閲覧範囲を個別に表示しない。
        String state = error.getSQLState() == null ? "" : error.getSQLState();
        if (ManagementDao.NO_DATA.equals(state)) {
            res.sendError(HttpServletResponse.SC_NOT_FOUND, "指定された情報が見つかりません。");
            return;
        }
        req.getServletContext().log("Management database operation failed (SQLState=" + state + ").");
        if (state.startsWith("23")) {
            res.sendError(409, "同じ情報がすでに登録されているか、他のデータから参照されています。入力と現在の状態を確認してください。");
        } else if (state.startsWith("40") || "55P03".equals(state)) {
            res.sendError(409, "別の操作と重なりました。画面を開き直して現在の状態を確認してください。");
        } else if ("42P01".equals(state) || "42703".equals(state) || "42501".equals(state)) {
            res.sendError(503, "管理・サポート機能のデータベース設定を確認してください。");
        } else {
            res.sendError(503, "現在データベースを利用できません。時間をおいて再度お試しください。");
        }
    }
}
