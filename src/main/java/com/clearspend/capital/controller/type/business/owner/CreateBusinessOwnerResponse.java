package com.clearspend.capital.controller.type.business.owner;

import com.clearspend.capital.common.typedid.data.BusinessOwnerId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
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

  @JsonProperty("errorMessage")
  @Schema(title = "Error message for any records that failed. Will be null if successful")
  private String errorMessage;
}
