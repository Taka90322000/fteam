package jp.machiriku.bean;

import java.io.Serializable;
import java.util.List;

/** 業種マスタ(大分類とその中の小分類)。投票・希望業種の選択肢に使う。 */
public record IndustryMaster(List<Category> categories) implements Serializable {

    public record Category(int categoryId, String categoryCode, String categoryName, List<Industry> industries) implements Serializable { }

    public record Industry(int industryTypeId, String industryCode, String industryName) implements Serializable { }
}
