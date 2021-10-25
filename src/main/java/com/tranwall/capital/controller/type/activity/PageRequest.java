package com.tranwall.capital.controller.type.activity;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NonNull;
import org.springframework.data.domain.Sort;

@Data
@Builder
public class PageRequest {

  @JsonProperty("pageNumber")
  @NonNull
  private Integer pageNumber;

  @JsonProperty("pageSize")
  @NonNull
  private Integer pageSize;

  @JsonProperty("orderable")
  private List<Orderable> orderable;

  @Data
  @Builder
  public static class Orderable {
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
