package com.tranwall.capital.service;

import com.tranwall.capital.controller.type.common.PageRequest;
import com.tranwall.capital.controller.type.user.SearchUserRequest;
import com.tranwall.capital.service.type.PageToken;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserFilterCriteria {

  private PageToken pageToken;

  public UserFilterCriteria(SearchUserRequest request) {
    this.pageToken = PageRequest.toPageToken(request.getPageRequest());
  }
}
