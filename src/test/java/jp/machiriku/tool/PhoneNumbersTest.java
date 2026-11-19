package jp.machiriku.tool;

import static org.junit.Assert.*;

import java.util.Map;

import org.junit.Test;

/** 携帯・固定・IP電話の入力、保存形式、再編集用の国内形式を確認する。 */
public class PhoneNumbersTest {
    @Test
    public void normalizesDomesticAndInternationalNumbersIncludingFullWidthInput() {
        Map<String, String> examples = Map.of(
                "090-1234-5678", "+819012345678",
                "028-633-9988", "+81286339988",
                "03-1234-5678", "+81312345678",
                "050-1234-5678", "+815012345678",
                " +81 28 633 9988 ", "+81286339988",
                "０２８（６３３）９９８８", "+81286339988");
        examples.forEach((input, expected) -> assertEquals(expected, PhoneNumbers.toE164(input, "電話番号")));
    }

    @Test
    public void optionalBlankNumbersBecomeNull() {
        for (String value : new String[] { null, "", " \t", "　" }) {
            assertNull(PhoneNumbers.toE164(value, "追加の電話番号"));
        }
    }

    @Test
    public void rejectsInvalidJapaneseNumbersAndPreservesTheFieldLabel() {
        for (String value : new String[] { "090123", "0286339988000", "090abc12345678",
                "+1 650-253-0000", "09012345678 内線123", "028/633/9988", "0".repeat(51) }) {
            IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                    () -> PhoneNumbers.toE164(value, "追加の電話番号"));
            assertTrue(error.getMessage().contains("追加の電話番号"));
        }
    }

    @Test
    public void formatsStoredNumbersForEditingAndKeepsUnknownLegacyValuesVisible() {
        assertEquals("028-633-9988", PhoneNumbers.forDisplay("+81286339988"));
        assertEquals("090-1234-5678", PhoneNumbers.forDisplay("+819012345678"));
        assertEquals("050-1234-5678", PhoneNumbers.forDisplay("+815012345678"));
        assertEquals("", PhoneNumbers.forDisplay(null));
        assertEquals("", PhoneNumbers.forDisplay(" "));
        assertEquals("old-number", PhoneNumbers.forDisplay("old-number"));
        assertEquals("+16502530000", PhoneNumbers.forDisplay("+16502530000"));
    }
}
