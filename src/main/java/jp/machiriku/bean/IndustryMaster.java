package jp.machiriku.bean;

import java.io.Serializable;
import java.util.List;

/** 業種マスタ(大分類とその中の小分類)。投票・希望業種の選択肢に使う。 */
public record IndustryMaster(List<Category> categories) implements Serializable {
    // JSPの ${...}(EL)で読むための getXxx()。record の xxx() は ${a.xxx} では読めない。
    public List<Category> getCategories() { return categories; }


    public record Category(int categoryId, String categoryCode, String categoryName, List<Industry> industries) implements Serializable {
        // JSPの ${...}(EL)で読むための getXxx()。record の xxx() は ${a.xxx} では読めない。
        public int getCategoryId() { return categoryId; }
        public String getCategoryCode() { return categoryCode; }
        public String getCategoryName() { return categoryName; }
        public List<Industry> getIndustries() { return industries; }
    }

    public record Industry(int industryTypeId, String industryCode, String industryName) implements Serializable {
    // JSPの ${...}(EL)で読むための getXxx()。record の xxx() は ${a.xxx} では読めない。
    public int getIndustryTypeId() { return industryTypeId; }
    public String getIndustryCode() { return industryCode; }
    public String getIndustryName() { return industryName; }
}
}
