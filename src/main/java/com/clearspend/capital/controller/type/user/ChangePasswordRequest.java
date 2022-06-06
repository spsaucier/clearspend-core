package com.clearspend.capital.controller.type.user;

import com.clearspend.capital.common.masking.annotation.Sensitive;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.service.FusionAuthService;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.Nullable;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Data
@NoArgsConstructor
@JsonInclude(Include.NON_ABSENT)
public class ChangePasswordRequest {

  public ChangePasswordRequest(@NonNull String currentPassword, @NonNull String newPassword) {
    this.currentPassword = currentPassword;
    this.newPassword = newPassword;
  }

  public ChangePasswordRequest(
      @Nullable TypedId<UserId> userId,
      @Nullable String currentPassword,
      @Nullable String newPassword,
      @Nullable String trustChallenge,
      @Nullable String twoFactorId,
      @Nullable String twoFactorCode) {
    this.userId = userId;
    this.currentPassword = currentPassword;
    this.newPassword = newPassword;
    this.trustChallenge = trustChallenge;
    this.twoFactorId = twoFactorId;
    this.twoFactorCode = twoFactorCode;
  }

  @Nullable private TypedId<UserId> userId;

  @Sensitive
  @JsonProperty("currentPassword")
  @Nullable
  private String currentPassword;

  @Sensitive
  @JsonProperty("newPassword")
  @Nullable
  private String newPassword;

  /**
   * Trust challenge is generated when the 2 factor step up process is started, and is required only
   * if the user has 2FA enabled, and distinguishes this step-up sequence from any other.
   */
  @JsonProperty("trustChallenge")
  @Nullable
  private String trustChallenge;

  /**
   * The twoFactorId is generated when the code is generated and distinguishes this 2FA sequence
   * from any other.
   */
  @JsonProperty("twoFactorId")
  @Nullable
  private String twoFactorId;

  /** This is the code the user received by another channel. */
  @JsonProperty("twoFactorCode")
  @Nullable
  private String twoFactorCode;

  public FusionAuthService.ChangePasswordRequest toFusionAuthRequest() {
    return new FusionAuthService.ChangePasswordRequest(
        currentPassword, newPassword, trustChallenge, twoFactorId, twoFactorCode);
  }
}
