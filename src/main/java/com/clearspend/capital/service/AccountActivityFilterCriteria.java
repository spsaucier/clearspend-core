package com.clearspend.capital.service;

import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.CardId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.controller.type.common.PageRequest;
import com.clearspend.capital.data.model.enums.AccountActivityIntegrationSyncStatus;
import com.clearspend.capital.data.model.enums.AccountActivityStatus;
import com.clearspend.capital.data.model.enums.AccountActivityType;
import com.clearspend.capital.service.type.PageToken;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AccountActivityFilterCriteria {

  private TypedId<BusinessId> businessId;
  private TypedId<AllocationId> allocationId;
  private TypedId<UserId> userId;
  private TypedId<CardId> cardId;
  private List<AccountActivityType> types;
  private String searchText;
  private OffsetDateTime from;
  private OffsetDateTime to;
  private List<AccountActivityStatus> statuses;
  private BigDecimal min;
  private BigDecimal max;
  private List<String> categories;
  private Boolean withReceipt;
  private Boolean withoutReceipt;
  private List<AccountActivityIntegrationSyncStatus> syncStatuses;
  private Boolean missingExpenseCategory;
  private PageToken pageToken;

  public AccountActivityFilterCriteria(
      TypedId<CardId> cardId,
      List<AccountActivityType> types,
      OffsetDateTime dateFrom,
      OffsetDateTime dateTo,
      PageRequest pageRequest) {
    this.cardId = cardId;
    this.types = types;
    this.from = dateFrom;
    this.to = dateTo;
    this.pageToken = PageRequest.toPageToken(pageRequest);
  }

  public AccountActivityFilterCriteria(
      TypedId<AllocationId> allocationId,
      List<AccountActivityType> types,
      OffsetDateTime from,
      OffsetDateTime to,
      String searchText,
      PageToken pageToken) {
    this.allocationId = allocationId;
    this.types = types;
    this.from = from;
    this.to = to;
    this.searchText = searchText;
    this.pageToken = pageToken;
  }

  public AccountActivityFilterCriteria(
      TypedId<BusinessId> businessId,
      List<AccountActivityType> types,
      List<AccountActivityStatus> statuses,
      OffsetDateTime from,
      OffsetDateTime to,
      PageToken pageToken) {
    this.businessId = businessId;
    this.types = types;
    this.statuses = statuses;
    this.from = from;
    this.to = to;
    this.pageToken = pageToken;
  }

  public String getTypesString() {
    return types.stream()
        .map(s -> String.format("'%s'", s.name()))
        .collect(Collectors.joining(","));
  }

  public String getStatusesString() {
    return statuses.stream()
        .map(s -> String.format("'%s'", s.name()))
        .collect(Collectors.joining(","));
  }

  public String getCategoriesString() {
    return categories.stream().map(s -> String.format("'%s'", s)).collect(Collectors.joining(","));
  }
}
