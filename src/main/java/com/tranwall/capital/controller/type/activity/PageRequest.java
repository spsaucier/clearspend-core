package com.tranwall.capital.controller.type.activity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tranwall.capital.service.type.PageToken;
import com.tranwall.capital.service.type.PageToken.OrderBy;
import java.util.Collections;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

@Data
@Builder
public class PageRequest {

  @JsonProperty("pageNumber")
  @NonNull
  private Integer pageNumber;

  @JsonProperty("pageSize")
  @NonNull
  private Integer pageSize;

  @JsonProperty("orderBy")
  private List<OrderBy> orderBy;

  public static PageToken toPageToken(PageRequest pageRequest) {
    if (pageRequest == null) {
      return new PageToken(0, 20, Collections.emptyList());
    }

    return new PageToken(pageRequest.pageNumber, pageRequest.pageSize, pageRequest.orderBy);
  }
}
