package com.tranwall.capital.controller.type.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.common.typedid.data.UserId;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;

@Data
@AllArgsConstructor
public class UpdateUserResponse {

  @JsonProperty("userId")
  @NonNull
  @NotNull(message = "userId required")
  private TypedId<UserId> userId;

  @JsonProperty("errorMessage")
  @Schema(title = "Error message for any records that failed. Will be null if successful")
  private String errorMessage;
}
