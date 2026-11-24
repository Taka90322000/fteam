package jp.machiriku.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;
import jp.machiriku.bean.User;
import jp.machiriku.dao.ProfileDao;
import jp.machiriku.dao.ProfileDao.IconChange;
import jp.machiriku.dao.ProfileDao.SavedProfile;
import jp.machiriku.profile.ProfileIconStore;
import jp.machiriku.registration.RegistrationForms;

/** 事業主とオーナーの両フォームで、ファイルとDB参照先をまとめて更新する。 */
public final class ProfileIconSupport {
    /** アイコンと地図の座標、フォームの入力値を合わせたmultipart全体の上限。 */
    public static final long MAX_REQUEST_BYTES = 2L * 1024 * 1024;

    /** 共通処理のみを提供する。 */
    private ProfileIconSupport() { }

    /**
     * フォームが画像を含められるmultipart形式で送信されたか調べる。
     * @param request フォームのリクエスト
     * @return multipart/form-dataならtrue
     */
    public static boolean isMultipart(HttpServletRequest request) {
        // 境界文字列の指定が後ろに続くため、メディアタイプの先頭部分で判定する。
        String type = request.getContentType();
        return type != null && type.toLowerCase(Locale.ROOT).startsWith("multipart/form-data");
    }

    /**
     * 業種・資格の選択値を、通常のフォーム送信とmultipartの両方から読み取る。
     * @param request フォームのリクエスト
     * @param name 読み取る選択欄の名前
     * @return 保存時のID検証に渡す値の一覧
     */
    public static List<String> selectionValues(HttpServletRequest request, String name) {
        // チェックボックスを1パートにまとめ、Tomcatのパート数上限で選択を失わないようにする。
        String[] values = request.getParameterValues(name);
        if (values == null) { return List.of(); }
        if (values.length == 1 && isMultipart(request)) {
            return Arrays.asList(values[0].split(",", -1));
        }
        return Arrays.asList(values);
    }

    /**
     * 画像を受け取り、保存済みの参照先の保持・差し替え・解除をプロフィールと一緒に確定する。
     * @param request 認証とCSRF検証を済ませたフォームのリクエスト
     * @param user 更新する会員
     * @param save アイコンの変更指定を含めてプロフィールをDBに保存する処理
     * @return コミットされた会員情報とアイコン参照先
     * @throws IOException ファイルを読み込めないか、保存先に書き込めない場合
     * @throws SQLException プロフィールをDBに保存できなかった場合
     * @throws IllegalArgumentException アップロードされた画像が正しくない場合
     */
    public static SavedProfile save(HttpServletRequest request, User user, SaveProfile save) throws IOException, SQLException {
        // 画像の指定がない場合は、DBで現在の参照先を保持する。
        Part part = readPart(request);
        boolean remove = "1".equals(request.getParameter("removeIcon"));
        if (part == null && !remove) {
            return save.run(new IconChange(true, null));
        }

        // 新しい画像の参照先は、利用者の入力から取得せず、サーバーで発行する。
        ProfileIconStore store = ProfileIconStore.configured();
        String replacement = null;
        if (part != null) {
            try (InputStream data = part.getInputStream()) {
                replacement = store.save(user.getUserId(), data, part.getSize());
            }
        }

        // 先に画像を書き込み、DBへの参照先の保存が失敗しても以前のアイコンを表示できるようにする。
        SavedProfile saved;
        try {
            saved = save.run(new IconChange(false, replacement));
        } catch (SQLException error) {
            // 接続断などでコミット結果が不明な場合は、DBが参照している可能性がある画像を残す。
            String state = error.getSQLState();
            if (state != null && !state.startsWith("08") && !"40003".equals(state)) {
                discard(request, store, user.getUserId(), replacement);
            }
            throw error;
        } catch (RuntimeException error) {
            // 保存前の入力検証などで取り消された場合は、新しい画像を片付ける。
            discard(request, store, user.getUserId(), replacement);
            throw error;
        }
        if (!Objects.equals(saved.previousIconImageUrl(), saved.iconImageUrl())) {
            discard(request, store, user.getUserId(), saved.previousIconImageUrl());
        }
        return saved;
    }

    /**
     * エラーでフォームを再表示するときに、DBに保存済みのアイコンを表示する。
     * @param request 再表示するフォームのリクエスト
     * @param dao 対象のプロフィールを読み取るDAO
     * @param user 認証済みの会員
     * @param fields 入力値を保持するフォーム用マップ
     */
    public static void restorePreview(HttpServletRequest request, ProfileDao dao, User user, Map<String, String> fields) {
        try {
            fields.put("iconImageUrl", dao.findProfileContact(user.getUserId(), user.getUserRole()).getOrDefault("iconImageUrl", ""));
        } catch (SQLException error) {
            // 元の保存エラーを表示したまま、参照先を読み込めなかったことをログに残す。
            request.getServletContext().log("Profile icon lookup failed (SQLState=" + error.getSQLState() + ").");
        }
        try {
            request.setAttribute("iconNeedsReselection", readPart(request) != null);
        } catch (IOException | IllegalArgumentException error) {
            // ファイルを読み取れなかった場合も、選び直す必要があることを表示する。
            request.setAttribute("iconNeedsReselection", true);
        }
    }

    /**
     * 保存済みのアイコンをプレビュー用のURLに変換する。
     * @param contextPath アプリのコンテキストパス
     * @param reference DBに保存された画像参照先
     * @return プレビューで利用できるURL。未設定や不正な参照先の場合は空文字列
     */
    public static String previewSource(String contextPath, String reference) {
        try {
            // 旧画面で登録済みのHTTP(S)とアプリ内の参照先を引き続き表示する。
            String source = RegistrationForms.iconImageUrl(reference);
            if (source == null) { return ""; }
            if (source.regionMatches(true, 0, "http:", 0, 5) || source.regionMatches(true, 0, "https:", 0, 6)) {
                return source;
            }
            return contextPath + (source.startsWith("/") ? source : "/" + source);
        } catch (IllegalArgumentException error) {
            return "";
        }
    }

    /**
     * multipartから、切り抜き画面が確定した画像のパートだけを取り出す。
     * @param request フォームのリクエスト
     * @return 画像のパート。画像が指定されていない場合はnull
     * @throws IOException リクエストの読み込みに失敗した場合
     * @throws IllegalArgumentException multipartが不正か、上限を超えた場合
     */
    private static Part readPart(HttpServletRequest request) throws IOException {
        if (!isMultipart(request)) { return null; }
        try {
            return request.getPart("profileIcon");
        } catch (ServletException | IllegalStateException error) {
            throw new IllegalArgumentException("アイコン画像を受け取れませんでした。画像を選び直してください。", error);
        }
    }

    /**
     * 保存済みプロフィールから参照されなくなった画像を片付ける。
     * @param request ログを残すためのリクエスト
     * @param store アイコンの管理先
     * @param userId 画像を保存した会員ID
     * @param reference 削除するファイルの参照先
     */
    private static void discard(HttpServletRequest request, ProfileIconStore store, int userId, String reference) {
        try {
            store.delete(userId, reference);
        } catch (IOException error) {
            // 削除失敗で確定済みのプロフィール更新が失敗扱いにならないようにする。
            request.getServletContext().log("Could not delete an unused profile icon.");
        }
    }

    /** 事業主またはオーナーのプロフィールをDBに保存する処理。 */
    @FunctionalInterface
    public interface SaveProfile {
        /**
         * アイコンの変更内容を確定し、プロフィールとともに保存する。
         * @param icon 画像を保持・差し替え・解除する指定
         * @return コミットされたプロフィールの情報
         * @throws SQLException DBで保存処理に失敗した場合
         */
        SavedProfile run(IconChange icon) throws SQLException;
    }
}
