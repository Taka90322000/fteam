package jp.machiriku.bean;

import java.util.List;

/** 検証済みの事業主情報。希望範囲は閉じたGeoJSON、業種・資格はマスターIDで受け取る。 */
public record BusinessProfileInput(String businessName, String representativeName, String email,
        String phoneE164, String preferredArea, int employeeCount, String managementExperience,
        String industryExperience, List<Integer> industryTypeIds, List<Integer> qualificationIds,
        String areaGeoJson, boolean emailNotify) { }
