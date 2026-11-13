package jp.machiriku.bean;

import java.util.List;

/** 検証済みの事業主情報。希望範囲は閉じたGeoJSON、業種・資格はマスターIDで受け取る。 */
public record BusinessProfileInput(String businessName, String representativeName, String email,
        String phoneE164, String preferredArea, int employeeCount, String managementExperience,
        String industryExperience, List<Integer> industryTypeIds, List<Integer> qualificationIds,
        String areaGeoJson, boolean emailNotify, String iconImageUrl) {
    /** アイコン未指定の既存の呼び出しにも対応する。 */
    public BusinessProfileInput(String businessName, String representativeName, String email,
            String phoneE164, String preferredArea, int employeeCount, String managementExperience,
            String industryExperience, List<Integer> industryTypeIds, List<Integer> qualificationIds,
            String areaGeoJson, boolean emailNotify) {
        this(businessName, representativeName, email, phoneE164, preferredArea, employeeCount, managementExperience,
                industryExperience, industryTypeIds, qualificationIds, areaGeoJson, emailNotify, null);
    }
}
