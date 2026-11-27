package jp.machiriku.servlet.vote;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import jp.machiriku.bean.IndustryMaster;
import jp.machiriku.bean.PropertyVote;
import jp.machiriku.bean.VoteChoice;
import jp.machiriku.bean.VoteContext;

/** Actionのテストで使う、投票・物件・業種マスタのサンプルデータ。 */
final class VoteTestData {
    private VoteTestData() { }

    static PropertyVote vote(int voteId, int propertyId, boolean withdrawn) {
        PropertyVote vote = new PropertyVote();
        vote.setVoteId(voteId);
        vote.setPropertyId(propertyId);
        vote.setResidentUserId(42);
        vote.setLocationId(7);
        vote.setDistanceKm(new BigDecimal("0.400"));
        vote.setDistanceScaleKm(new BigDecimal("1.500"));
        vote.setVisitFrequency("週1回程度");
        vote.setRequestComment("駅前に欲しいです");
        vote.setUpdatedAt(OffsetDateTime.parse("2026-11-10T10:00:00Z"));
        if (withdrawn) vote.setWithdrawnAt(OffsetDateTime.parse("2026-11-11T10:00:00Z"));
        for (int rank = 1; rank <= 2; rank++) {
            VoteChoice choice = new VoteChoice();
            choice.setPreferenceRank(rank);
            choice.setIndustryTypeId(rank);
            choice.setIndustryCode(rank == 1 ? "I0101" : "I0102");
            choice.setIndustryName(rank == 1 ? "カフェ・喫茶店" : "レストラン");
            choice.setRankWeight(rank == 1 ? new BigDecimal("1.00") : new BigDecimal("0.60"));
            vote.getChoices().add(choice);
        }
        return vote;
    }

    static VoteContext context(int propertyId, boolean votable) {
        return new VoteContext(
                new VoteContext.PropertyInfo(propertyId, "駅前の空き店舗", "栃木県", "宇都宮市", "宇都宮市1-1", "OPEN", true, new BigDecimal("1.500")),
                votable, votable ? null : "PROPERTY_CLOSED", votable ? null : "この物件は募集を終了しています。", 3,
                List.of(new VoteContext.LocationOption(7, "HOME", "栃木県", "宇都宮市", "宇都宮市2-2", true, new BigDecimal("0.400"), true),
                        new VoteContext.LocationOption(8, "WORK", "栃木県", "宇都宮市", "宇都宮市3-3", false, null, false)),
                7);
    }

    static IndustryMaster industries() {
        return new IndustryMaster(List.of(new IndustryMaster.Category(1, "C01", "飲食", List.of(
                new IndustryMaster.Industry(1, "I0101", "カフェ・喫茶店"),
                new IndustryMaster.Industry(2, "I0102", "レストラン")))));
    }
}
