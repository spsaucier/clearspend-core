package com.clearspend.capital.controller.type.plaid;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PlaidLogEntryRequest {
  public static final int DEFAULT_PAGE_NUM = 0;
  public static final int DEFAULT_PAGE_SIZE = 20;

  private Integer pageNum;
  private Integer pageSize;

  @Schema(hidden = true)
  public Pageable getPageable() {
    final int realPageNum = Optional.ofNullable(pageNum).orElse(DEFAULT_PAGE_NUM);
    final int realPageSize = Optional.ofNullable(pageSize).orElse(DEFAULT_PAGE_SIZE);
    return PageRequest.of(realPageNum, realPageSize);
  }
}
