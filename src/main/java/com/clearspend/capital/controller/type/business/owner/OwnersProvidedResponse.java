package com.clearspend.capital.controller.type.business.owner;

import com.clearspend.capital.controller.type.business.Business;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;

@Data
@AllArgsConstructor
public class OwnersProvidedResponse {

  @JsonProperty("business")
  @NonNull
  @NotNull(message = "business required")
  private Business business;

  @JsonProperty("errorMessages")
  @Schema(title = "Error message for any records that failed. Will be null if successful")
  private List<String> errorMessages;
}
