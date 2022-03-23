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
import java.util.ArrayList;
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
  private Boolean includeActiveCards;
  private Boolean includeFrozenCards;
  private Boolean includeCancelledCards;
  private Boolean includePhysicalCards;
  private Boolean includeVirtualCards;

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
    List<CardStatus> statusCollection = new ArrayList<>();
    if (includeActiveCards) {
      statusCollection.add(CardStatus.ACTIVE);
    }
    if (includeFrozenCards) {
      statusCollection.add(CardStatus.INACTIVE);
    }
    if (includeCancelledCards) {
      statusCollection.add(CardStatus.CANCELLED);
    }
    return statusCollection.stream()
        .map(status -> String.format("'%s'", status.toString()))
        .collect(Collectors.joining(","));
  }

  public String getTypeString() {
    List<CardType> types = new ArrayList<>();
    if (includePhysicalCards) {
      types.add(CardType.PHYSICAL);
    }
    if (includeVirtualCards) {
      types.add(CardType.VIRTUAL);
    }
    return types.stream()
        .map(type -> String.format("'%s'", type.toString()))
        .collect(Collectors.joining(","));
  }

  public String getSearchStringHash() {
    if (StringUtils.hasLength(searchText)) {
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
        request.getSearchText(),
        request.getBalanceRange() == null ? null : request.getBalanceRange().getMin(),
        request.getBalanceRange() == null ? null : request.getBalanceRange().getMax(),
        request.getIncludeActiveCards(),
        request.getIncludeFrozenCards(),
        request.getIncludeCancelledCards(),
        request.getIncludePhysicalCards(),
        request.getIncludeVirtualCards(),
        PageRequest.toPageToken(request.getPageRequest()));
  }
}
