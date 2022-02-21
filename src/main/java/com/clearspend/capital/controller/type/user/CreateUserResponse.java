package com.clearspend.capital.controller.type.user;

import com.clearspend.capital.common.masking.annotation.Sensitive;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.fasterxml.jackson.annotation.JsonProperty;
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

  @Sensitive
  @JsonProperty("password")
  @Schema(title = "Flag to indicate whether a password should be created for the user")
  private String password;

  @JsonProperty("errorMessage")
  @Schema(title = "Error message for any records that failed. Will be null if successful")
  private String errorMessage;
}
