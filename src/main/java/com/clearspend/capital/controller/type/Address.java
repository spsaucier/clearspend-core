package com.clearspend.capital.controller.type;

import com.clearspend.capital.common.data.model.ClearAddress;
import com.clearspend.capital.crypto.data.model.embedded.EncryptedString;
import com.clearspend.capital.data.model.enums.Country;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.persistence.Embeddable;
import javax.persistence.MappedSuperclass;
import javax.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;

@Embeddable
@Data
@AllArgsConstructor
@MappedSuperclass
public class Address {

  @JsonProperty("streetLine1")
  @Size(min = 1, max = 100, message = "Minimum 1 character, Maximum 100 characters")
  private String streetLine1;

  @JsonProperty("streetLine2")
  private String streetLine2;

  @JsonProperty("locality")
  @Size(min = 1, max = 100, message = "Minimum 1 character, Maximum 100 characters")
  private String locality;

  @JsonProperty("region")
  @Size(min = 1, max = 100, message = "Minimum 1 character, Maximum 100 characters")
  private String region;

  @JsonProperty("postalCode")
  @Size(min = 5, max = 11, message = "Minimum 5 character, Maximum 11 characters")
  private String postalCode;

  @JsonProperty("country")
  @Size(min = 3, max = 3, message = "3 characters")
  private Country country;

  public com.clearspend.capital.common.data.model.Address toAddress() {
    return new com.clearspend.capital.common.data.model.Address(
        new EncryptedString(streetLine1),
        new EncryptedString(streetLine2),
        locality,
        region,
        new EncryptedString(postalCode),
        country);
  }

  public Address(com.clearspend.capital.common.data.model.Address address) {
    if (address == null) {
      return;
    }

    if (address.getStreetLine1() != null) {
      streetLine1 = address.getStreetLine1().getEncrypted();
    }
    if (address.getStreetLine2() != null) {
      streetLine2 = address.getStreetLine2().getEncrypted();
    }
    locality = address.getLocality();
    region = address.getRegion();
    if (address.getPostalCode() != null) {
      postalCode = address.getPostalCode().getEncrypted();
    }
    country = address.getCountry();
  }

  public Address(ClearAddress address) {
    if (address == null) {
      return;
    }

    if (address.getStreetLine1() != null) {
      streetLine1 = address.getStreetLine1();
    }
    if (address.getStreetLine2() != null) {
      streetLine2 = address.getStreetLine2();
    }
    locality = address.getLocality();
    region = address.getRegion();
    if (address.getPostalCode() != null) {
      postalCode = address.getPostalCode();
    }
    country = address.getCountry();
  }
}
