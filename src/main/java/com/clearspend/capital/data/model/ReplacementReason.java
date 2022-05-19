package com.clearspend.capital.data.model;

import lombok.Getter;

@Getter
public enum ReplacementReason {
  LOST("lost"),
  STOLEN("stolen"),
  DAMAGED("damaged"),
  EXPIRED("expired");

  private final String value;

  ReplacementReason(final String value) {
    this.value = value;
  }
}
