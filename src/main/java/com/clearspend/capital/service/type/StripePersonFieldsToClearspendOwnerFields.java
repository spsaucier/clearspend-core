package com.clearspend.capital.service.type;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum StripePersonFieldsToClearspendOwnerFields {
  address_streetLine1("address.line1"),
  address_streetLine2("address.line2"),
  address_locality("address.city"),
  address_region("address.state"),
  address_postalCode("address.postal_code"),
  address_country("address.country"),
  firstName("first_name"),
  lastName("last_name"),
  relationshipOwner("relationship.owner"),
  relationshipExecutive("relationship.executive"),
  percentageOwnership("relationship.percent_ownership"),
  title("relationship.title"),
  dateOfBirth("dob.year"),
  taxIdentificationNumber("id_number"),
  email("email"),
  phone("phone");

  final String stripeField;

  StripePersonFieldsToClearspendOwnerFields(String stripeField) {
    this.stripeField = stripeField;
  }

  private static final Map<String, StripePersonFieldsToClearspendOwnerFields>
      stripeFieldsToOwnerFields = initializeMap();

  private static Map<String, StripePersonFieldsToClearspendOwnerFields> initializeMap() {
    return Arrays.stream(StripePersonFieldsToClearspendOwnerFields.values())
        .collect(Collectors.toUnmodifiableMap(e -> e.stripeField, Function.identity()));
  }

  public static String fromStripeField(String stripeField) {
    StripePersonFieldsToClearspendOwnerFields stripePersonFieldsToClearspendOwnerFields =
        stripeFieldsToOwnerFields.get(stripeField);
    return stripePersonFieldsToClearspendOwnerFields != null
        ? stripePersonFieldsToClearspendOwnerFields.name().replace("_", ".")
        : stripeField;
  }
}
