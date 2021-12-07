package com.tranwall.capital.service.type;

import com.tranwall.capital.data.model.enums.AccountActivityStatus;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Data;

@Data
public class AccountActivity {

  private AccountActivityStatus accountActivityStatus;

  private OffsetDateTime hideAfter;

  private OffsetDateTime visibleAfter;

  private OffsetDateTime activityTime;

  private String merchantLogoUrl;

  private BigDecimal merchantLatitude;

  private BigDecimal merchantLongitude;
}
