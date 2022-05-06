package com.clearspend.capital.controller.type.user;

import static com.clearspend.capital.controller.type.Constants.EMAIL_PATTERN;
import static com.clearspend.capital.controller.type.Constants.PHONE_PATTERN;

import com.clearspend.capital.common.masking.annotation.Sensitive;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.controller.type.Address;
import com.clearspend.capital.data.model.OwnerRelated;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UpdateUserRequest implements OwnerRelated {

  @JsonIgnore private TypedId<UserId> userId;
  @JsonIgnore private TypedId<BusinessId> businessId;

  @Sensitive
  @JsonProperty("firstName")
  @NotNull(message = "firstName required")
  @Schema(title = "The first name of the person", required = true, example = "John")
  @Size(max = 100, message = "The first name should not be more than 100 characters.")
  private String firstName;

  @Sensitive
  @JsonProperty("lastName")
  @NotNull(message = "lastName required")
  @Schema(title = "The last name of the person", required = true, example = "Wick")
  @Size(max = 100, message = "The last name should not be more than 100 characters.")
  private String lastName;

  private Address address;

  @Sensitive
  @JsonProperty("email")
  @NotNull(message = "email required")
  @Schema(title = "Email address of the person", required = true, example = "johnw@hightable.com")
  @Pattern(regexp = EMAIL_PATTERN, message = "Incorrect email format.")
  @Size(max = 100, message = "The email should not be more than 100 characters.")
  private String email;

  @Sensitive
  @JsonProperty("phone")
  @Schema(title = "Phone number in e.164 format", example = "+1234567890")
  @Pattern(regexp = PHONE_PATTERN, message = "Incorrect phone format.")
  private String phone;

  @JsonProperty("generatePassword")
  @Schema(title = "Flag to indicate whether a password should be created for the user")
  private boolean generatePassword;

  @Override
  public TypedId<UserId> getOwnerId() {
    return getUserId();
  }
}
