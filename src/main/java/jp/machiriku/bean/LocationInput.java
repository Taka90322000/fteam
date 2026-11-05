package jp.machiriku.bean;

import java.math.BigDecimal;

/** 検証済みの居住地・職場所在地。住所と緯度経度を同じ入力として扱う。 */
public record LocationInput(String kind, String prefecture, String municipality, String addressLine,
        BigDecimal latitude, BigDecimal longitude) { }
