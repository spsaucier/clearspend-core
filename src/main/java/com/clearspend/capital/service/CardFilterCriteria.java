package com.clearspend.capital.service;

import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.BusinessId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.controller.type.card.SearchCardRequest;
import com.clearspend.capital.controller.type.common.PageRequest;
import com.clearspend.capital.service.type.PageToken;
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
