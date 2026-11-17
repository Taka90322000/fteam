package jp.machiriku.bean;

import java.util.Map;
import java.util.Set;

/** 編集画面へ復元する保存済みの入力値と、業種・資格の選択ID。 */
public record ProfileSettings(Map<String, String> fields, Set<String> selectedIndustries,
        Set<String> selectedQualifications) { }
