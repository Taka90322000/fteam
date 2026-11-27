package jp.machiriku.servlet.vote;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jp.machiriku.bean.User;

/**
 * Actionのテスト用の、偽のリクエスト・セッション・レスポンス。サーブレットコンテナもDBも使わない。
 * forward先・リダイレクト先・エラーコード・リクエスト属性を記録して、後から確認できる。
 */
final class PageTestHarness {
    final Map<String, String> parameters = new HashMap<>();
    final Map<String, String> responseHeaders = new HashMap<>();
    final Map<String, Object> sessionValues = new HashMap<>();
    final Map<String, Object> attributes = new HashMap<>();
    boolean hasSession = true;
    int status = 200;
    Integer errorCode;
    String redirect;
    String forwardedTo;
    final HttpServletRequest request;
    final HttpServletResponse response;

    /** ログインしていない呼び出し。 */
    static PageTestHarness anonymous(String method) {
        PageTestHarness h = new PageTestHarness(method);
        h.hasSession = false;
        return h;
    }

    static PageTestHarness as(String method, String role, String accountStatus) {
        PageTestHarness h = new PageTestHarness(method);
        User user = new User();
        user.setUserId(42);
        user.setUserRole(role);
        user.setAccountStatus(accountStatus);
        h.sessionValues.put("user", user);
        return h;
    }

    /** 画面を表示した(forwardした)かどうか。 */
    boolean rendered() { return forwardedTo != null; }

    /** 本人確認の段階で止まった(リダイレクトかエラー)かどうか。 */
    boolean stopped() { return redirect != null || errorCode != null; }

    PageTestHarness(String method) {
        HttpSession session = proxy(HttpSession.class, (ignored, call, args) -> {
            switch (call.getName()) {
                case "getAttribute": return sessionValues.get(args[0]);
                case "setAttribute": sessionValues.put((String) args[0], args[1]); return null;
                default: return null;
            }
        });
        ServletContext context = proxy(ServletContext.class, (ignored, call, args) -> null);
        request = proxy(HttpServletRequest.class, (ignored, call, args) -> {
            switch (call.getName()) {
                case "getMethod": return method;
                case "getContextPath": return "/machiriku";
                case "getParameter": return parameters.get(args[0]);
                case "getSession": return hasSession ? session : null;
                case "getServletContext": return context;
                case "setAttribute": attributes.put((String) args[0], args[1]); return null;
                case "getAttribute": return attributes.get(args[0]);
                case "getRequestDispatcher":
                    String path = (String) args[0];
                    return proxy(RequestDispatcher.class, (d, dispatch, dargs) -> {
                        if ("forward".equals(dispatch.getName())) forwardedTo = path;
                        return null;
                    });
                default: return null;
            }
        });
        response = proxy(HttpServletResponse.class, (ignored, call, args) -> {
            switch (call.getName()) {
                case "setStatus": status = (Integer) args[0]; return null;
                case "setHeader": responseHeaders.put((String) args[0], (String) args[1]); return null;
                case "sendRedirect": redirect = (String) args[0]; return null;
                case "sendError": errorCode = (Integer) args[0]; return null;
                case "isCommitted": return redirect != null || errorCode != null;
                default: return null;
            }
        });
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, java.lang.reflect.InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, handler);
    }
}
