package jp.machiriku.tool;

import java.io.IOException;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;

// 全リクエストに適用するフィルター
@WebFilter("/scoremanager/main/*")
public class EncodingFilter implements Filter {

    // 初期化処理（今回は特に何もしない）
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    // リクエスト処理（ここがメイン）
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        // リクエストの文字コードをUTF-8に設定（これが超重要）
        request.setCharacterEncoding("UTF-8");

        // 次の処理へ進める（これ書かないと画面止まる）
        chain.doFilter(request, response);
    }

    // 終了処理（今回は特に何もしない）
    public void destroy() {
    }
}