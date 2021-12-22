package com.clearspend.capital.service;

import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.controller.type.common.PageRequest;
import com.clearspend.capital.controller.type.user.SearchUserRequest;
import com.clearspend.capital.service.type.PageToken;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserFilterCriteria {

  private List<TypedId<AllocationId>> allocations;
  private Boolean includeArchived;
  private Boolean hasVirtualCard;
  private Boolean hasPhysicalCard;
  private Boolean withoutCard;
  private String searchText;
  private PageToken pageToken;

  public UserFilterCriteria(SearchUserRequest request) {
    allocations = request.getAllocations();
    hasPhysicalCard = request.getHasPhysicalCard();
    hasVirtualCard = request.getHasVirtualCard();
    withoutCard = request.getWithoutCard();
    searchText = request.getSearchText();
    includeArchived = request.getIncludeArchived();
    this.pageToken = PageRequest.toPageToken(request.getPageRequest());
  }
}
