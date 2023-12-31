package com.clearspend.capital.common.data.model;

import com.clearspend.capital.crypto.data.model.embedded.EncryptedString;
import com.clearspend.capital.data.model.enums.Country;
import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.MappedSuperclass;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@NoArgsConstructor // required for Hibernate but shouldn't be used otherwise
@AllArgsConstructor
@MappedSuperclass
public class ClearAddress {

  private String streetLine1;

  private String streetLine2;

  // typically, a city or town
  private String locality;

  // typically, a state or prefecture
  private String region;

  // zip code in the US
  private String postalCode;

  @Enumerated(EnumType.STRING)
  private Country country;

  public static ClearAddress of(Address address) {
    return new ClearAddress(
        address.getStreetLine1().getEncrypted(),
        address.getStreetLine2().getEncrypted(),
        address.getLocality(),
        address.getRegion(),
        address.getPostalCode().getEncrypted(),
        address.getCountry());
  }

  public Address toAddress() {
    return new Address(
        new EncryptedString(streetLine1),
        new EncryptedString(streetLine2),
        locality,
        region,
        new EncryptedString(postalCode),
        country);
  }
}
