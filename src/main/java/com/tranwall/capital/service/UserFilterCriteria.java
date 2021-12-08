package com.tranwall.capital.service;

import com.tranwall.capital.common.typedid.data.AllocationId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.controller.type.common.PageRequest;
import com.tranwall.capital.controller.type.user.SearchUserRequest;
import com.tranwall.capital.service.type.PageToken;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserFilterCriteria {

  private List<TypedId<AllocationId>> allocations;
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
    this.pageToken = PageRequest.toPageToken(request.getPageRequest());
  }
}
