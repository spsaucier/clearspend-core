package com.tranwall.capital.service;

import com.tranwall.capital.common.typedid.data.AllocationId;
import com.tranwall.capital.common.typedid.data.BusinessId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.common.typedid.data.UserId;
import com.tranwall.capital.controller.type.card.SearchCardRequest;
import com.tranwall.capital.controller.type.common.PageRequest;
import com.tranwall.capital.service.type.PageToken;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CardFilterCriteria {

  private String searchText;

  private TypedId<BusinessId> businessId;

  private TypedId<UserId> userId;

  private TypedId<AllocationId> allocationId;

  private PageToken pageToken;

  public CardFilterCriteria(TypedId<BusinessId> businessId, SearchCardRequest request) {
    this.businessId = businessId;
    this.userId = request.getUserId();
    this.allocationId = request.getAllocationId();
    this.searchText = request.getSearchText();
    this.pageToken = PageRequest.toPageToken(request.getPageRequest());
  }
}
