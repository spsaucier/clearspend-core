package com.tranwall.capital.controller.type.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tranwall.capital.common.typedid.data.BusinessId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.common.typedid.data.UserId;
import com.tranwall.capital.controller.type.Address;
import com.tranwall.capital.data.model.enums.UserType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;

@Data
@AllArgsConstructor
public class User {

  @JsonProperty("userId")
  @NonNull
  private TypedId<UserId> userId;

  @JsonProperty("businessId")
  @NonNull
  private TypedId<BusinessId> businessId;

  @JsonProperty("type")
  @NonNull
  private UserType type;

  @JsonProperty("firstName")
  @NonNull
  private String firstName;

  @JsonProperty("lastName")
  @NonNull
  private String lastName;

  @JsonProperty("address")
  @NonNull
  private Address address;

  @JsonProperty("email")
  @NonNull
  private String email;

  @JsonProperty("phone")
  @NonNull
  private String phone;

  public User(com.tranwall.capital.data.model.User card) {
    this.userId = card.getId();
    this.businessId = card.getBusinessId();
    this.type = card.getType();
    this.firstName = card.getFirstName().getEncrypted();
    this.lastName = card.getLastName().getEncrypted();
    this.address = new Address(card.getAddress());
    this.email = card.getEmail().getEncrypted();
    this.phone = card.getPhone().getEncrypted();
  }
}
