package com.clearspend.capital.client.i2c;

import com.clearspend.capital.client.i2c.util.I2CEnumSerializable;
import java.util.EnumSet;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum CardStatusCode implements I2CEnumSerializable<CardStatusCode> {

  /** Card has been issued but not yet activated */
  ISSUED_INACTIVE("A"),

  /** All transactions allowed */
  OPEN("B"),

  /** Lost card */
  LOST_CARD("C"),

  /** Stolen card */
  STOLEN_CARD("D"),

  /** No withdrawals are allowed to be made on card. (Restricted) */
  NO_WITHDRAWALS("E"),

  /** Card account has been closed */
  CLOSED("F"),

  /** Lost card not captured */
  LOST_NOT_CAPTURED("G"),

  /** Stolen card not captured */
  STOLEN_NOT_CAPTURED("H"),

  /** Card is inactive or blocked */
  INACTIVE("I"),

  /** Card reissue */
  CARD_REISSUE("R"),

  /** Fraud block */
  FRAUD_BLOCK("S");

  private static final Map<String, CardStatusCode> codesMap =
      EnumSet.allOf(CardStatusCode.class).stream()
          .collect(Collectors.toUnmodifiableMap(CardStatusCode::getCode, Function.identity()));
  @Getter private final String code;

  public static CardStatusCode fromCode(String code) {
    return codesMap.get(code);
  }

  @Override
  public String serialize() {
    return code;
  }
}
