package jp.machiriku.tool;

import java.io.IOException;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;

// 下記URLパターンに一致するリクエストへ適用する文字コード設定フィルター。
@WebFilter("/*")
public class EncodingFilter implements Filter {

    // 初期化処理（今回は特に何もしない）
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    // 後続の画面処理がフォームの日本語を読み取る前に文字コードを設定する。
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        // リクエストの本文をUTF-8として解釈する。
        request.setCharacterEncoding("UTF-8");

        // フィルター処理だけで終了せず、次のフィルターまたはServletへ渡す。
        chain.doFilter(request, response);
    }

    // 終了処理（今回は特に何もしない）
    public void destroy() {
    }
}
