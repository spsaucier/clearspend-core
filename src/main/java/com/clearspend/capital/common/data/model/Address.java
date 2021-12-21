package com.clearspend.capital.common.data.model;

import com.clearspend.capital.common.masking.annotation.Sensitive;
import com.clearspend.capital.crypto.data.model.embedded.EncryptedString;
import com.clearspend.capital.data.model.enums.Country;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.MappedSuperclass;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.logging.log4j.util.Strings;

@Embeddable
@Data
@NoArgsConstructor // required for Hibernate but shouldn't be used otherwise
@AllArgsConstructor
@MappedSuperclass
public class Address {

  @Embedded @Sensitive private EncryptedString streetLine1;

  @Embedded @Sensitive private EncryptedString streetLine2;

  // typically, a city or town
  private String locality;

  // typically, a state or prefecture
  private String region;

  // zip code in the US
  @Embedded @Sensitive private EncryptedString postalCode;

  @Enumerated(EnumType.STRING)
  private Country country;

  public static String getSingleStreetLine(String... streetLines) {
    return Stream.of(streetLines)
        .filter(s -> s != null && !s.isBlank())
        .collect(Collectors.joining(" "));
  }

  public static String getSingleStreetLine(Address address) {
    if (address == null) {
      return "";
    }

    String result =
        String.join(
            " ",
            (address.getStreetLine1() != null
                    && Strings.isNotBlank(address.getStreetLine1().getEncrypted())
                ? address.getStreetLine1().getEncrypted()
                : ""),
            (address.getStreetLine2() != null
                    && Strings.isNotBlank(address.getStreetLine2().getEncrypted())
                ? address.getStreetLine2().getEncrypted()
                : ""));

    if (Strings.isBlank(result)) {
      return "";
    }

    return result;
  }

  public com.stripe.model.Address toStripeAddress() {
    com.stripe.model.Address address = new com.stripe.model.Address();

    if (streetLine1 != null) {
      address.setLine1(streetLine1.getEncrypted());
    }
    if (streetLine2 != null) {
      address.setLine2(streetLine2.getEncrypted());
    }
    address.setCity(locality);
    address.setState(region);
    if (postalCode != null) {
      address.setPostalCode(postalCode.getEncrypted());
    }
    if (country != null) {
      address.setCity(country.name());
    }

    return address;
  }
}
