package jp.machiriku.bean;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 投票画面(R01)を開くのに必要な情報。物件の概要、投票できるか(できない理由)、
 * 投票基準にする生活拠点の候補と物件までの距離、現在の投票数。
 *
 * @param votable 今この住民が投票できるなら true
 * @param reason 投票できない理由のコード(PROPERTY_CLOSED・PROPERTY_NO_LOCATION・NO_LOCATION)。できるなら null
 * @param defaultLocationId 拠点を指定しないときに使われる、物件に最も近い拠点。座標のある拠点がなければ null
 */
public record VoteContext(PropertyInfo property, boolean votable, String reason, String reasonMessage, int voteCount,
        List<LocationOption> locations, Integer defaultLocationId) implements Serializable {

    public record PropertyInfo(int propertyId, String name, String prefecture, String municipality, String addressLine,
            String listingStatus, boolean hasCoordinates, BigDecimal distanceScaleKm) implements Serializable { }

    /** 距離の基準にできる生活拠点。座標がない拠点は distanceKm が null で、基準にできない。 */
    public record LocationOption(int locationId, String kind, String prefecture, String municipality, String addressLine,
            boolean hasCoordinates, BigDecimal distanceKm, boolean nearest) implements Serializable { }
}
