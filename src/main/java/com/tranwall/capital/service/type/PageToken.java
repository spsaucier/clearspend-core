package com.tranwall.capital.service.type;

import java.util.List;
import javax.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NonNull;
import org.springframework.data.domain.Sort;

@Data
@AllArgsConstructor
public class PageToken {

  @NonNull private Integer pageNumber;

  @NonNull private Integer pageSize;

  private List<OrderBy> orderBy;

  @Data
  @Builder
  public static class OrderBy {
    @Pattern(regexp = "[a-zA-Z0-9_\\-]*")
    private OrderItem item;

    private Sort.Direction direction;
  }

  @Getter
  public enum OrderItem {
    DATE("activityTime"),
    AMOUNT("amount");

    String name;

    OrderItem(String item) {
      this.name = item;
    }
  }
}
