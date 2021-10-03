package com.tranwall.capital.common.data.model;

import com.tranwall.capital.data.model.enums.Country;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.persistence.Embeddable;
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

  public static String getSingleStreetLine(String... streetLines) {
    return Stream.of(streetLines)
        .filter(s -> s != null && !s.isBlank())
        .collect(Collectors.joining(" "));
  }

  public static String getSingleStreetLine(ClearAddress clearAddress) {
    if (clearAddress == null) {
      return "";
    }

    String result =
        String.join(
            " ",
            (clearAddress.getStreetLine1() != null && Strings.isNotBlank(clearAddress.getStreetLine1())
                ? clearAddress.getStreetLine1()
                : ""),
            (clearAddress.getStreetLine2() != null && Strings.isNotBlank(clearAddress.getStreetLine2())
                ? clearAddress.getStreetLine2()
                : ""));

    if (Strings.isBlank(result)) {
      return "";
    }

    return result;
  }
}
