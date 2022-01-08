package com.clearspend.capital.controller.type.user;

import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.controller.type.Address;
import com.clearspend.capital.data.model.business.BusinessOwner;
import com.clearspend.capital.data.model.business.BusinessProspect;
import com.clearspend.capital.data.model.enums.UserType;
import com.fasterxml.jackson.annotation.JsonProperty;
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
  private Address address;

  @JsonProperty("email")
  @NonNull
  private String email;

  @JsonProperty("phone")
  @NonNull
  private String phone;

  @JsonProperty("archived")
  private boolean archived;

  public User(com.clearspend.capital.data.model.User user) {
    this.userId = user.getId();
    this.businessId = user.getBusinessId();
    this.type = user.getType();
    this.firstName = user.getFirstName().getEncrypted();
    this.lastName = user.getLastName().getEncrypted();
    this.address = new Address(user.getAddress());
    this.email = user.getEmail().getEncrypted();
    this.phone = user.getPhone().getEncrypted();
    this.archived = user.isArchived();
  }

  public User(BusinessOwner businessOwner) {
    this.userId = new TypedId<>(businessOwner.getId().toUuid());
    this.businessId = businessOwner.getBusinessId();
    this.type = UserType.BUSINESS_OWNER;
    this.firstName = businessOwner.getFirstName().getEncrypted();
    this.lastName = businessOwner.getLastName().getEncrypted();
    this.address = new Address(businessOwner.getAddress());
    this.email = businessOwner.getEmail().getEncrypted();
    this.phone = businessOwner.getPhone().getEncrypted();
  }

  public User(BusinessProspect businessProspect) {
    this.userId = new TypedId<>(businessProspect.getId().toUuid());
    this.businessId = businessProspect.getBusinessId();
    this.type = UserType.BUSINESS_OWNER;
    this.firstName = businessProspect.getFirstName().getEncrypted();
    this.lastName = businessProspect.getLastName().getEncrypted();
    this.email = businessProspect.getEmail().getEncrypted();
    this.phone = businessProspect.getPhone().getEncrypted();
  }
}
