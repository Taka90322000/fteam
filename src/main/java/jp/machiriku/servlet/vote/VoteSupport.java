package jp.machiriku.servlet.vote;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jp.machiriku.bean.PropertyVote;
import jp.machiriku.bean.User;
import jp.machiriku.bean.VoteChoice;
import jp.machiriku.logic.DemandScoreCalculator;

/** 投票API共通の、本人確認・JSON応答・投票内容のJSON変換。 */
final class VoteSupport {
    private static final ObjectMapper JSON = new ObjectMapper();

    private VoteSupport() { }

    /** ログイン中の有効な地域住民を返す。条件を満たさない場合はJSONでエラーを返して null。 */
    static User requireResident(HttpServletRequest req, HttpServletResponse res) throws IOException {
        res.setHeader("Cache-Control", "no-store");
        HttpSession session = req.getSession(false);
        if (session == null || !(session.getAttribute("user") instanceof User user)) {
            error(res, 401, "UNAUTHENTICATED", "ログインが必要です。");
            return null;
        }
        if (user.getWithdrawnAt() != null) {
            error(res, 403, "FORBIDDEN", "この会員は利用できません。");
            return null;
        }
        if ("PENDING".equals(user.getAccountStatus())) {
            error(res, 403, "SETUP_REQUIRED", "初期設定を完了してください。");
            return null;
        }
        if (!"RESIDENT".equals(user.getUserRole()) || !"ACTIVE".equals(user.getAccountStatus())) {
            error(res, 403, "NOT_RESIDENT", "投票できるのは地域住民の会員だけです。");
            return null;
        }
        return user;
    }

    /**
     * 他サイトからのフォーム送信(CSRF)を防ぐため、更新系は画面のJavaScriptからだけ受け付ける。
     * ブラウザは別サイトからカスタムヘッダー付きの送信を許可しないため、このヘッダーの有無で判別できる。
     */
    static boolean hasAjaxHeader(HttpServletRequest req) {
        return "XMLHttpRequest".equals(req.getHeader("X-Requested-With"));
    }

    static void error(HttpServletResponse res, int status, String code, String message) throws IOException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", false);
        body.put("code", code);
        body.put("message", message);
        write(res, status, body);
    }

    static void write(HttpServletResponse res, int status, Map<String, Object> body) throws IOException {
        res.setStatus(status);
        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");
        res.getWriter().write(JSON.writeValueAsString(body));
    }

    static void logDatabaseError(HttpServletRequest req, SQLException error) {
        req.getServletContext().log("Vote failed (SQLState=" + error.getSQLState() + ").");
    }

    /** 投票1件を画面へ返す形に変換する。score は明細スコアの合計(小数第3位まで)。 */
    static Map<String, Object> toMap(PropertyVote vote) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("voteId", vote.getVoteId());
        map.put("propertyId", vote.getPropertyId());
        map.put("withdrawn", vote.isWithdrawn());
        map.put("distanceKm", vote.getDistanceKm());
        map.put("distanceScaleKm", vote.getDistanceScaleKm());
        map.put("frequency", vote.getVisitFrequency());
        map.put("comment", vote.getRequestComment());
        map.put("updatedAt", vote.getUpdatedAt() == null ? null : vote.getUpdatedAt().toString());

        List<Map<String, Object>> choices = new ArrayList<>();
        double total = 0;
        for (VoteChoice choice : vote.getChoices()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("rank", choice.getPreferenceRank());
            item.put("industryCode", choice.getIndustryCode());
            item.put("industryName", choice.getIndustryName());
            item.put("rankWeight", choice.getRankWeight());
            choices.add(item);
            total += DemandScoreCalculator.score(choice.getPreferenceRank(),
                    vote.getDistanceKm().doubleValue(), vote.getDistanceScaleKm().doubleValue());
        }
        map.put("choices", choices);
        map.put("score", BigDecimal.valueOf(total).setScale(3, RoundingMode.HALF_UP));
        return map;
    }
}
