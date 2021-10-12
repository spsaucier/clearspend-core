package com.tranwall.capital.controller.type;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tranwall.capital.crypto.data.model.embedded.EncryptedString;
import com.tranwall.capital.data.model.enums.Country;
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

  public com.tranwall.capital.common.data.model.Address toAddress() {
    return new com.tranwall.capital.common.data.model.Address(
        new EncryptedString(streetLine1),
        new EncryptedString(streetLine2),
        locality,
        region,
        new EncryptedString(postalCode),
        country);
  }
}
