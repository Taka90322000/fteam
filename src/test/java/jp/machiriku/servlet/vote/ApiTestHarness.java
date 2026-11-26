package jp.machiriku.servlet.vote;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jp.machiriku.bean.User;

/** Actionのテスト用の、偽のリクエスト・セッション・レスポンス。サーブレットコンテナもDBも使わない。 */
final class ApiTestHarness {
    private static final ObjectMapper JSON = new ObjectMapper();

    final Map<String, String> parameters = new HashMap<>();
    final Map<String, String> responseHeaders = new HashMap<>();
    final Map<String, Object> sessionValues = new HashMap<>();
    final StringWriter body = new StringWriter();
    boolean hasSession = true;
    int status = 200;
    final HttpServletRequest request;
    final HttpServletResponse response;

    /** ログインしていない呼び出し。 */
    static ApiTestHarness anonymous(String method) {
        ApiTestHarness h = new ApiTestHarness(method);
        h.hasSession = false;
        return h;
    }

    static ApiTestHarness as(String method, String role, String accountStatus) {
        ApiTestHarness h = new ApiTestHarness(method);
        User user = new User();
        user.setUserId(42);
        user.setUserRole(role);
        user.setAccountStatus(accountStatus);
        h.sessionValues.put("user", user);
        return h;
    }

    ApiTestHarness(String method) {
        HttpSession session = proxy(HttpSession.class, (ignored, call, args) ->
                "getAttribute".equals(call.getName()) ? sessionValues.get(args[0]) : null);
        ServletContext context = proxy(ServletContext.class, (ignored, call, args) -> null);
        request = proxy(HttpServletRequest.class, (ignored, call, args) -> {
            switch (call.getName()) {
                case "getMethod": return method;
                case "getParameter": return parameters.get(args[0]);
                case "getSession": return hasSession ? session : null;
                case "getServletContext": return context;
                default: return null;
            }
        });
        response = proxy(HttpServletResponse.class, (ignored, call, args) -> {
            switch (call.getName()) {
                case "setStatus": status = (Integer) args[0]; return null;
                case "setHeader": responseHeaders.put((String) args[0], (String) args[1]); return null;
                case "getWriter": return new PrintWriter(body, true);
                default: return null;
            }
        });
    }

    JsonNode json() throws Exception { return JSON.readTree(body.toString()); }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, java.lang.reflect.InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, handler);
    }
}
