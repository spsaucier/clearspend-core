package com.clearspend.capital.controller.type.ledger;

import com.clearspend.capital.data.model.embedded.UserDetails;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class LedgerUser {

  public static LedgerUser SYSTEM_USER = new LedgerUser(LedgerUserType.SYSTEM);
  public static LedgerUser EXTERNAL_USER = new LedgerUser(LedgerUserType.EXTERNAL);

  @NonNull
  @JsonProperty("type")
  private LedgerUserType type;

  @JsonProperty("userInfo")
  private UserInfo userInfo;

  public LedgerUser(UserDetails userDetails) {
    type = LedgerUserType.USER;
    if (userDetails != null) {
      userInfo =
          new UserInfo(
              userDetails.getId(),
              userDetails.getFirstName().getEncrypted(),
              userDetails.getLastName().getEncrypted(),
              userDetails.getEmail().getEncrypted());
    }
  }
}
