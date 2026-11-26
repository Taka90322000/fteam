package jp.machiriku.bean.management;

import java.time.OffsetDateTime;

/** 管理者の操作履歴一覧に表示する1行分のデータ。 */
public class AdminAuditLogItem {
    /** 操作日時。 */
    private OffsetDateTime occurredAt;
    /** 操作した管理者の会員ID。 */
    private int actorUserId;
    /** 操作した管理者の表示名。 */
    private String actorName;
    /** 操作コード。 */
    private String action;
    /** 操作対象の区分。 */
    private String targetType;
    /** 操作対象の識別子（JSON文字列）。 */
    private String targetKey;
    /** 操作理由。 */
    private String reason;
    /** 変更内容（JSON文字列）。 */
    private String changes;

    /** 空の履歴データを生成する。 */
    public AdminAuditLogItem() { }

    /**
     * 操作日時を取得する。
     * @return 操作日時。
     */
    public OffsetDateTime getOccurredAt() {
        return occurredAt;
    }

    /**
     * 操作日時を設定する。
     * @param occurredAt 操作日時。
     */
    public void setOccurredAt(OffsetDateTime occurredAt) {
        this.occurredAt = occurredAt;
    }

    /**
     * 操作者の会員IDを取得する。
     * @return 操作者の会員ID。
     */
    public int getActorUserId() {
        return actorUserId;
    }

    /**
     * 操作者の会員IDを設定する。
     * @param actorUserId 操作者の会員ID。
     */
    public void setActorUserId(int actorUserId) {
        this.actorUserId = actorUserId;
    }

    /**
     * 操作者の表示名を取得する。
     * @return 操作者の表示名。
     */
    public String getActorName() {
        return actorName;
    }

    /**
     * 操作者の表示名を設定する。
     * @param actorName 操作者の表示名。
     */
    public void setActorName(String actorName) {
        this.actorName = actorName;
    }

    /**
     * 操作コードを取得する。
     * @return 操作コード。
     */
    public String getAction() {
        return action;
    }

    /**
     * 操作コードを設定する。
     * @param action 操作コード。
     */
    public void setAction(String action) {
        this.action = action;
    }

    /**
     * 対象区分を取得する。
     * @return 対象区分。
     */
    public String getTargetType() {
        return targetType;
    }

    /**
     * 対象区分を設定する。
     * @param targetType 対象区分。
     */
    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    /**
     * 対象ID文字列を取得する。
     * @return 対象IDのJSON文字列。
     */
    public String getTargetKey() {
        return targetKey;
    }

    /**
     * 対象ID文字列を設定する。
     * @param targetKey 対象IDのJSON文字列。
     */
    public void setTargetKey(String targetKey) {
        this.targetKey = targetKey;
    }

    /**
     * 操作理由を取得する。
     * @return 操作理由。
     */
    public String getReason() {
        return reason;
    }

    /**
     * 操作理由を設定する。
     * @param reason 操作理由。
     */
    public void setReason(String reason) {
        this.reason = reason;
    }

    /**
     * 変更内容を取得する。
     * @return 変更内容のJSON文字列。空の場合はnull。
     */
    public String getChanges() {
        return changes;
    }

    /**
     * 変更内容を設定する。
     * @param changes 変更内容のJSON文字列。
     */
    public void setChanges(String changes) {
        this.changes = changes;
    }
}
