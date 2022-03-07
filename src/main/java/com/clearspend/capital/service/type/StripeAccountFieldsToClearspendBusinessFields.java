package com.clearspend.capital.service.type;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum StripeAccountFieldsToClearspendBusinessFields {
  legalName("name"),
  employerIdentificationNumber("tax_id"),
  businessPhone("phone"),
  address_streetLine1("address.line1"),
  address_streetLine2("address.line2"),
  address_locality("address.city"),
  address_region("address.state"),
  address_postalCode("address.postal_code"),
  address_country("address.country"),
  merchantType("mcc"),
  description("product_description"),
  url("url");

  String stripeField;

  StripeAccountFieldsToClearspendBusinessFields(String stripeField) {
    this.stripeField = stripeField;
  }

  private static final Map<String, StripeAccountFieldsToClearspendBusinessFields>
      stripeFieldsToBusinessFields = initializeMap();

  private static Map<String, StripeAccountFieldsToClearspendBusinessFields> initializeMap() {
    return Arrays.stream(StripeAccountFieldsToClearspendBusinessFields.values())
        .collect(Collectors.toUnmodifiableMap(e -> e.stripeField, Function.identity()));
  }

  public static String fromStripeField(String stripeField) {
    StripeAccountFieldsToClearspendBusinessFields field =
        stripeFieldsToBusinessFields.get(stripeField);
    return field != null ? field.name().replace("_", ".") : stripeField;
  }
}
