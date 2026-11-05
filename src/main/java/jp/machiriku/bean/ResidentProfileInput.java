package jp.machiriku.bean;

/** 住民登録フォームの検証済み入力。occupationは今回の保存対象外のためnullとする。 */
public record ResidentProfileInput(String displayName, String email, String ageGroup, String gender,
        String occupation, LocationInput home, LocationInput work, boolean emailNotify) { }
