package com.clearspend.capital.controller.type.user;

import com.clearspend.capital.data.model.business.BusinessOwner;
import com.clearspend.capital.data.model.business.BusinessProspect;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.fusionauth.domain.ChangePasswordReason;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.http.ResponseEntity;

@Data
@EqualsAndHashCode(callSuper = true)
public class UserLoginResponse extends User {

  @JsonProperty private String twoFactorId;

  @JsonProperty private String changePasswordId;

  @JsonProperty private ChangePasswordReason changePasswordReason;

  private UserLoginResponse() {}

  public UserLoginResponse(com.clearspend.capital.data.model.User user) {
    super(user);
  }

  public UserLoginResponse(BusinessOwner businessOwner) {
    super(businessOwner);
  }

  public UserLoginResponse(BusinessProspect businessProspect) {
    super(businessProspect);
  }

  public static ResponseEntity<UserLoginResponse> twoFactorChallenge(String twoFactorId) {
    UserLoginResponse response = new UserLoginResponse();
    response.twoFactorId = twoFactorId;
    return ResponseEntity.status(200).body(response);
  }

  public static ResponseEntity<UserLoginResponse> resetPasswordResponse(
      String changePasswordId, ChangePasswordReason changePasswordReason) {
    UserLoginResponse response = new UserLoginResponse();
    response.changePasswordId = changePasswordId;
    response.changePasswordReason = changePasswordReason;
    return ResponseEntity.status(203).body(response);
  }
}
