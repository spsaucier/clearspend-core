package com.clearspend.capital.service;

import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.controller.type.card.SearchCardRequest;
import com.clearspend.capital.controller.type.common.PageRequest;
import com.clearspend.capital.crypto.HashUtil;
import com.clearspend.capital.data.model.enums.card.CardStatus;
import com.clearspend.capital.data.model.enums.card.CardType;
import com.clearspend.capital.service.type.CurrentUser;
import com.clearspend.capital.service.type.PageToken;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.xml.bind.DatatypeConverter;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.util.StringUtils;

@Data
@AllArgsConstructor
public class CardFilterCriteria {

  private TypedId<BusinessId> businessId;
  private TypedId<UserId> invokingUser;
  private String permission;
  private List<TypedId<UserId>> cardHolders;
  private List<TypedId<AllocationId>> allocationIds;
  private String searchText;
  private BigDecimal minimumBalance;
  private BigDecimal maximumBalance;
  private List<CardStatus> statuses;
  private List<CardType> types;

  private PageToken pageToken;

  public String getUserIdsString() {
    return cardHolders.stream()
        .map(it -> String.format("'%s'", it.toString()))
        .collect(Collectors.joining(","));
  }

  public String getAllocationsString() {
    return allocationIds.stream()
        .map(it -> String.format("'%s'", it.toString()))
        .collect(Collectors.joining(","));
  }

  public String getStatusString() {
    // If no Statuses are supplied, we should search for all Statuses
    if (statuses != null) {
      return statuses.stream()
          .map(status -> String.format("'%s'", status.toString()))
          .collect(Collectors.joining(","));
    }
    return Arrays.stream(CardStatus.values())
        .map(status -> String.format("'%s'", status.toString()))
        .collect(Collectors.joining(","));
  }

  public String getTypeString() {
    if (types != null) {
      return types.stream()
          .map(type -> String.format("'%s'", type.toString()))
          .collect(Collectors.joining(","));
    }
    return Arrays.stream(CardType.values())
        .map(type -> String.format("'%s'", type.toString()))
        .collect(Collectors.joining(","));
  }

  public String getSearchStringHash() {
    if (StringUtils.hasText(searchText)) {
      return DatatypeConverter.printHexBinary(HashUtil.calculateHash(searchText));
    }
    return null;
  }

  public static CardFilterCriteria fromSearchRequest(SearchCardRequest request) {
    return new CardFilterCriteria(
        CurrentUser.getBusinessId(),
        CurrentUser.getUserId(),
        "",
        request.getUsers(),
        request.getAllocations(),
        StringUtils.hasText(request.getSearchText())
            ? request.getSearchText()
            : null, // scrub empty strings
        request.getBalanceRange() == null ? null : request.getBalanceRange().getMin(),
        request.getBalanceRange() == null ? null : request.getBalanceRange().getMax(),
        request.getStatuses(),
        request.getTypes(),
        PageRequest.toPageToken(request.getPageRequest()));
  }
}
