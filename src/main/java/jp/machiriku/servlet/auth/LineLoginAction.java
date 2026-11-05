package jp.machiriku.servlet.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jp.machiriku.auth.LineLoginClient;
import jp.machiriku.auth.LineLoginConfig;
import jp.machiriku.tool.Action;

/** 入力を検証し、認証に必要な情報をセッションへ保持してLINEの認可画面へ案内する。 */
public class LineLoginAction extends Action {
    /** LINE通信処理を作成する。テストでは実通信しないクライアントへ差し替える。 */
    protected LineLoginClient createClient() {
        return new LineLoginClient(LineLoginConfig.fromEnvironment());
    }

    @Override
    public void execute(HttpServletRequest req, HttpServletResponse res) throws Exception {
        // 認証画面をキャッシュさせず、遷移先へ参照元URLを送信しない。
        res.setHeader("Cache-Control", "no-store");
        res.setHeader("Referrer-Policy", "no-referrer");
        if (!"GET".equals(req.getMethod()) && !"POST".equals(req.getMethod())) {
            res.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }
        // 前回の登録区分が次のログインへ混入しないよう、開始済みの認証情報を破棄する。
        HttpSession session = req.getSession();
        session.removeAttribute(LineLoginSupport.ATTEMPT_KEY);
        String role = req.getParameter("regRole");
        // 登録区分や電話番号をURLへ含めないよう、新規登録はPOSTだけで受け付ける。
        if (role != null && !"POST".equals(req.getMethod())) {
            res.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }
        // 公開登録で選べる役割だけを許可し、管理者などの不正な指定を拒否する。
        if (role != null && !LineLoginSupport.REGISTRATION_ROLES.contains(role)) {
            LineLoginSupport.error(req, res, true, "利用区分を選び直してください。");
            return;
        }
        // DB制約に合わせ、事業主・オーナーは国際形式の電話番号を必須とする。
        String phone = null;
        if (role != null) {
            phone = req.getParameter("phoneE164");
            phone = phone == null || phone.isBlank() ? null : phone.trim();
            if ((phone != null && !phone.matches("\\+[1-9][0-9]{7,14}"))
                    || (!"resident".equals(role) && phone == null)) {
                LineLoginSupport.error(req, res, true,
                        "事業主・オーナーは電話番号を国際形式で入力してください（例：+819012345678）。");
                return;
            }
        }
        try {
            LineLoginClient client = createClient();
            // 登録区分と電話番号はサーバー側に保持し、認証後のリクエスト値を信用しない。
            var attempt = LineLoginSupport.begin(role, phone);
            String url = client.authorizationUrl(attempt.state(), attempt.nonce(), attempt.codeVerifier());
            session.setAttribute(LineLoginSupport.ATTEMPT_KEY, attempt);
            res.sendRedirect(url);
        } catch (IllegalStateException | IllegalArgumentException e) {
            LineLoginSupport.error(req, res, role != null,
                    "LINEログインの設定が完了していません。管理者にお問い合わせください。");
        }
    }
}
