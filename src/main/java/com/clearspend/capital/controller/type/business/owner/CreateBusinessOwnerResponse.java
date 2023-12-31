package com.clearspend.capital.controller.type.business.owner;

import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessOwnerId;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;

@Data
@AllArgsConstructor
public class CreateBusinessOwnerResponse {

  @JsonProperty("businessOwnerId")
  @NonNull
  @NotNull(message = "businessOwnerId required")
  private TypedId<BusinessOwnerId> businessOwnerId;

  @JsonProperty("errorMessages")
  @Schema(title = "Error message for any records that failed. Will be null if successful")
  private List<String> errorMessages;
}
