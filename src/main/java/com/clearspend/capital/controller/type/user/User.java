package com.clearspend.capital.controller.type.user;

import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.controller.type.Address;
import com.clearspend.capital.crypto.data.model.embedded.NullableEncryptedStringWithHash;
import com.clearspend.capital.data.model.business.BusinessOwner;
import com.clearspend.capital.data.model.business.BusinessProspect;
import com.clearspend.capital.data.model.enums.UserType;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class User {

  protected User() {}

  @JsonProperty("userId")
  private TypedId<UserId> userId;

  @JsonProperty("businessId")
  private TypedId<BusinessId> businessId;

  @JsonProperty("type")
  private UserType type;

  @JsonProperty("firstName")
  private String firstName;

  @JsonProperty("lastName")
  private String lastName;

  @JsonProperty("address")
  private Address address;

  @JsonProperty("email")
  private String email;

  @JsonProperty("phone")
  private String phone;

  @JsonProperty("archived")
  private boolean archived;

  @JsonProperty("relationshipToBusiness")
  private RelationshipToBusiness relationshipToBusiness;

  public User(com.clearspend.capital.data.model.User user) {
    this.userId = user.getId();
    this.businessId = user.getBusinessId();
    this.type = user.getType();
    this.firstName = user.getFirstName().getEncrypted();
    this.lastName = user.getLastName().getEncrypted();
    this.address = new Address(user.getAddress());
    this.email = user.getEmail().getEncrypted();
    this.phone =
        Optional.ofNullable(user.getPhone())
            .map(NullableEncryptedStringWithHash::getEncrypted)
            .orElse(null);
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
    this.relationshipToBusiness =
        new RelationshipToBusiness(
            businessOwner.getRelationshipOwner(),
            businessOwner.getRelationshipExecutive(),
            businessOwner.getRelationshipRepresentative(),
            businessOwner.getRelationshipDirector());
  }

  public User(BusinessProspect businessProspect) {
    this.userId = new TypedId<>(businessProspect.getId().toUuid());
    this.businessId = businessProspect.getBusinessId();
    this.type = UserType.BUSINESS_OWNER;
    this.firstName = businessProspect.getFirstName().getEncrypted();
    this.lastName = businessProspect.getLastName().getEncrypted();
    this.email = businessProspect.getEmail().getEncrypted();
    this.phone = businessProspect.getPhone().getEncrypted();
    this.relationshipToBusiness =
        new RelationshipToBusiness(
            businessProspect.getRelationshipOwner(),
            businessProspect.getRelationshipExecutive(),
            businessProspect.getRelationshipRepresentative(),
            businessProspect.getRelationshipDirector());
  }
}
