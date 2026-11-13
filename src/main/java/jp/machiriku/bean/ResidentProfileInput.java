package jp.machiriku.bean;

import java.util.List;

/** 住民登録フォームの検証済み入力。職業は保存対象外で、欲しい業種はマスターのコードで受け取る。 */
public record ResidentProfileInput(String displayName, String email, String ageGroup, String gender,
        String occupation, LocationInput home, LocationInput work, boolean emailNotify, List<String> preferredIndustryCodes) {
    public ResidentProfileInput {
        preferredIndustryCodes = preferredIndustryCodes == null ? List.of() : List.copyOf(preferredIndustryCodes);
    }

    /** 希望業種を指定しない既存の呼び出しにも対応する。 */
    public ResidentProfileInput(String displayName, String email, String ageGroup, String gender,
            String occupation, LocationInput home, LocationInput work, boolean emailNotify) {
        this(displayName, email, ageGroup, gender, occupation, home, work, emailNotify, List.of());
    }
}
