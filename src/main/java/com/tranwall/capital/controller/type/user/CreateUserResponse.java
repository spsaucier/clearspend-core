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
public class CreateUserResponse {

  @JsonProperty("userId")
  @NonNull
  @NotNull(message = "userId required")
  private TypedId<UserId> userId;

  @JsonProperty("password")
  @Schema(title = "Flag to indicate whether a password should be created for the user")
  private String password;

  @JsonProperty("errorMessage")
  @Schema(title = "Error message for any records that failed. Will be null if successful")
  private String errorMessage;
}
