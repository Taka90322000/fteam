package jp.machiriku.bean;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.List;

/** 自分の投票履歴の1ページ分。total は条件に合う全件数。 */
public record VoteHistoryPage(int total, int limit, int offset, List<Item> items) implements Serializable {
    // JSPの ${...}(EL)で読むための getXxx()。record の xxx() は ${a.xxx} では読めない。
    public int getTotal() { return total; }
    public int getLimit() { return limit; }
    public int getOffset() { return offset; }
    public List<Item> getItems() { return items; }


    /** 投票1件と、その物件の表示用情報。物件が削除されていても履歴には残す。 */
    public record Item(PropertyVote vote, String propertyName, String prefecture, String municipality,
            String addressLine, String listingStatus, boolean propertyDeleted, OffsetDateTime createdAt)
            implements Serializable {
        // JSPの ${...}(EL)で読むための getXxx()。record の xxx() は ${a.xxx} では読めない。
        public PropertyVote getVote() { return vote; }
        public String getPropertyName() { return propertyName; }
        public String getPrefecture() { return prefecture; }
        public String getMunicipality() { return municipality; }
        public String getAddressLine() { return addressLine; }
        public String getListingStatus() { return listingStatus; }
        public boolean getPropertyDeleted() { return propertyDeleted; }
        public OffsetDateTime getCreatedAt() { return createdAt; }
    }
}
