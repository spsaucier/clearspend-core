package com.tranwall.capital.data.model.enums;

import java.time.Duration;

public enum LimitPeriod {
  INSTANT(Duration.ofDays(0)),
  DAILY(Duration.ofDays(1)),
  WEEKLY(Duration.ofDays(7)),
  MONTHLY(Duration.ofDays(30));

  private Duration duration;

  LimitPeriod(Duration duration) {
    this.duration = duration;
  }

  public Duration getDuration() {
    return duration;
  }
}
