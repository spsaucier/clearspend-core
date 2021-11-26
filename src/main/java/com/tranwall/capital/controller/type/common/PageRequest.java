package com.tranwall.capital.controller.type.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tranwall.capital.service.type.PageToken;
import com.tranwall.capital.service.type.PageToken.OrderBy;
import java.util.Collections;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@NoArgsConstructor
@RequiredArgsConstructor
public class PageRequest {

  @JsonProperty("pageNumber")
  @NonNull
  @NotNull(message = "pageNumber should not be null")
  private Integer pageNumber;

  @JsonProperty("pageSize")
  @NonNull
  @NotNull(message = "pageSize should not be null")
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
