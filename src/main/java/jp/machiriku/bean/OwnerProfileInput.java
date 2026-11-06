package jp.machiriku.bean;

import java.util.List;

/** 検証済みのオーナー登録内容。誘致希望業種のマスターIDとアイコン画像の参照先を保持する。 */
public record OwnerProfileInput(String representativeName, String company, String email, String phoneE164,
        List<Integer> industryTypeIds, boolean emailNotify, String iconImageUrl) {
    /** アイコン未指定の既存の呼び出しにも対応する。 */
    public OwnerProfileInput(String representativeName, String company, String email, String phoneE164,
            List<Integer> industryTypeIds, boolean emailNotify) {
        this(representativeName, company, email, phoneE164, industryTypeIds, emailNotify, null);
    }
}
