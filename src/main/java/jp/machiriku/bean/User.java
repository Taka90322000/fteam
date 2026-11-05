package jp.machiriku.bean;

import java.io.Serializable;
import java.time.OffsetDateTime;

/** DBから取得した会員情報を画面やセッションへ渡すためのデータクラス。 */
public class User implements Serializable {

    // usersテーブルの主キー。ログイン中の会員を特定するときに使用する。
    private int userId;
    private String displayName;
    // メールアドレスはLINEから取得できない場合があるため、nullを許容する。
    private String email;
    // DBではRESIDENT・BUSINESS・OWNERの大文字で保持する。
    private String userRole;
    private String phoneE164;
    // 検証済みIDトークンのsub。メールアドレスではなく、この値で会員を照合する。
    private String lineSubject;
    // 会員の利用状態と日時。認証時の利用可否判定にも使う。
    private String accountStatus;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime withdrawnAt;


    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUserRole() {
        return userRole;
    }

    public void setUserRole(String userRole) {
        this.userRole = userRole;
    }

    public String getPhoneE164() {
        return phoneE164;
    }

    public void setPhoneE164(String phoneE164) {
        this.phoneE164 = phoneE164;
    }

    public String getLineSubject() {
        return lineSubject;
    }

    public void setLineSubject(String lineSubject) {
        this.lineSubject = lineSubject;
    }

    public String getAccountStatus() {
        return accountStatus;
    }

    public void setAccountStatus(String accountStatus) {
        this.accountStatus = accountStatus;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public OffsetDateTime getWithdrawnAt() {
        return withdrawnAt;
    }

    public void setWithdrawnAt(OffsetDateTime withdrawnAt) {
        this.withdrawnAt = withdrawnAt;
    }
    
}
