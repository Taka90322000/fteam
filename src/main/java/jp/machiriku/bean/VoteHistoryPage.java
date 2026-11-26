package jp.machiriku.bean;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.List;

/** 自分の投票履歴の1ページ分。total は条件に合う全件数。 */
public record VoteHistoryPage(int total, int limit, int offset, List<Item> items) implements Serializable {

    /** 投票1件と、その物件の表示用情報。物件が削除されていても履歴には残す。 */
    public record Item(PropertyVote vote, String propertyName, String prefecture, String municipality,
            String addressLine, String listingStatus, boolean propertyDeleted, OffsetDateTime createdAt)
            implements Serializable { }
}
