package com.clearspend.capital.data.model.enums;

import java.time.ZoneId;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TimeZone {
  US_EASTERN(ZoneId.of("US/Eastern")),
  US_CENTRAL(ZoneId.of("US/Central")),
  US_MOUNTAIN(ZoneId.of("US/Mountain")),
  US_PACIFIC(ZoneId.of("US/Pacific")),
  US_ALASKA(ZoneId.of("US/Alaska")),
  US_HAWAII(ZoneId.of("US/Hawaii"));

  private final ZoneId zoneId;
}
