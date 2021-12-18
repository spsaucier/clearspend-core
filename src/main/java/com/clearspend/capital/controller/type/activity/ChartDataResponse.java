package com.clearspend.capital.controller.type.activity;

import com.clearspend.capital.controller.type.Amount;
import com.clearspend.capital.data.model.enums.MerchantType;
import com.clearspend.capital.service.type.ChartData;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ChartDataResponse {

  record MerchantCategoryChartData(
      @JsonProperty("merchantType") MerchantType merchantType,
      @JsonProperty("amount") Amount amount) {}

  record AllocationChartData(
      @JsonProperty("allocation") AllocationInfo allocation,
      @JsonProperty("amount") Amount amount) {}

  record UserChartData(
      @JsonProperty("user") UserChartInfo user, @JsonProperty("amount") Amount amount) {}

  record MerchantChartData(
      @JsonProperty("merchant") MerchantInfo merchant, @JsonProperty("amount") Amount amount) {}

  @JsonProperty("merchantCategoryChartData")
  private List<MerchantCategoryChartData> merchantCategoryChartData;

  @JsonProperty("allocationChartData")
  private List<AllocationChartData> allocationChartData;

  @JsonProperty("userChartData")
  private List<UserChartData> userChartData;

  @JsonProperty("merchantChartData")
  private List<MerchantChartData> merchantChartData;

  public ChartDataResponse(ChartData chartData, ChartFilterType chartFilterType) {
    switch (chartFilterType) {
      case ALLOCATION -> this.allocationChartData =
          chartData.getAllocationChartData().stream()
              .map(
                  allocation ->
                      new AllocationChartData(
                          new AllocationInfo(
                              allocation.getAllocationId(), allocation.getAllocationName()),
                          Amount.of(allocation.getAmount())))
              .collect(Collectors.toList());
      case EMPLOYEE -> this.userChartData =
          chartData.getUserChartData().stream()
              .map(user -> new UserChartData(new UserChartInfo(user), Amount.of(user.getAmount())))
              .collect(Collectors.toList());
      case MERCHANT -> this.merchantChartData =
          chartData.getMerchantChartData().stream()
              .map(
                  merchan ->
                      new MerchantChartData(
                          new MerchantInfo(merchan), Amount.of(merchan.getAmount())))
              .collect(Collectors.toList());
      case MERCHANT_CATEGORY -> this.merchantCategoryChartData =
          chartData.getMerchantCategoryChartData().stream()
              .map(
                  merchant ->
                      new MerchantCategoryChartData(
                          merchant.getMerchantType(), Amount.of(merchant.getAmount())))
              .collect(Collectors.toList());
    }
  }
}
