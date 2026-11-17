package jp.machiriku.tool;

import java.text.Normalizer;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;

/** 日本の連絡先電話番号の検証と、保存・表示形式への変換を共通化する。 */
public final class PhoneNumbers {
    private static final PhoneNumberUtil PHONE_UTIL = PhoneNumberUtil.getInstance(); // 番号計画に基づく検証と整形。

    private PhoneNumbers() { }

    /** 国内形式または+81形式をE.164へ変換する。任意項目の空欄はnullを返す。 */
    public static String toE164(String value, String label) {
        if (value == null || value.isBlank()) {
            return null;
        }

        // 全角数字や括弧を半角にそろえ、ハイフンと空白を取り除く。
        String digits = Normalizer.normalize(value, Normalizer.Form.NFKC)
                .replaceAll("[\\s()ー−‐-]", ""); // 番号の解析に使う数字列と先頭の+。
        if (value.length() > 50 || !digits.matches("\\+?[0-9]+")) {
            throw invalid(label);
        }

        try {
            PhoneNumber number = PHONE_UTIL.parse(digits, "JP"); // 日本の番号として解析した結果。
            if (!PHONE_UTIL.isValidNumberForRegion(number, "JP")) {
                throw invalid(label);
            }
            return PHONE_UTIL.format(number, PhoneNumberFormat.E164);
        } catch (NumberParseException e) {
            throw invalid(label);
        }
    }

    /** 保存済みの日本の番号を国内形式へ整形する。旧データの不明な番号は元の値を表示する。 */
    public static String forDisplay(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        try {
            PhoneNumber number = PHONE_UTIL.parse(value, "JP"); // 保存済み番号の解析結果。
            if (PHONE_UTIL.isValidNumberForRegion(number, "JP")) {
                return PHONE_UTIL.format(number, PhoneNumberFormat.NATIONAL);
            }
        } catch (NumberParseException e) {
            // 既存の入力値を表示し、利用者が正しい番号へ修正できるようにする。
        }
        return value;
    }

    private static IllegalArgumentException invalid(String label) {
        return new IllegalArgumentException(label + "は日本の電話番号を入力してください（例：028-633-9988、090-1234-5678）。");
    }
}
