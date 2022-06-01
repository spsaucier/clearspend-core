package com.clearspend.capital.controller.type.allocation;

import static com.clearspend.capital.service.type.JobContextPropertyName.AMOUNT;

import com.clearspend.capital.common.typedid.data.JobConfigId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.controller.type.Amount;
import com.clearspend.capital.data.model.JobConfig;
import com.clearspend.capital.data.model.enums.Currency;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.hibernate.validator.constraints.Range;
import org.jobrunr.scheduling.cron.CronExpression;

@Data
@RequiredArgsConstructor
public class AllocationAutoTopUpConfigResponse {

  @JsonProperty("id")
  @NonNull
  private TypedId<JobConfigId> id;

  @JsonProperty("monthlyDay")
  @NonNull
  @Range(max = 28, min = 1)
  private Integer monthlyDay;

  @JsonProperty("amount")
  @NonNull
  private Amount amount;

  @JsonProperty("active")
  private boolean active;

  public static AllocationAutoTopUpConfigResponse of(JobConfig allocationAutoTopUp) {
    AllocationAutoTopUpConfigResponse allocationAutoTopUpConfigResponse =
        new AllocationAutoTopUpConfigResponse(
            allocationAutoTopUp.getId(),
            CronExpression.create(allocationAutoTopUp.getCron())
                .next(Instant.now(), ZoneId.systemDefault())
                .atZone(ZoneId.systemDefault())
                .getDayOfMonth(),
            new Amount(
                Currency.USD,
                BigDecimal.valueOf(
                    Double.parseDouble(
                        allocationAutoTopUp.getJobContext().get(AMOUNT).toString()))));
    allocationAutoTopUpConfigResponse.setActive(allocationAutoTopUp.getActive());
    return allocationAutoTopUpConfigResponse;
  }
}
