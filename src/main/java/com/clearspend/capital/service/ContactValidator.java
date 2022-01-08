package com.clearspend.capital.service;

import com.clearspend.capital.data.model.business.BusinessOwner;
import com.plaid.client.model.Owner;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.NonNull;
import org.springframework.stereotype.Component;

@Component
public class ContactValidator {

  public static record ValidationResult(boolean namesMatch, boolean postalCodesMatch) {

    boolean isValid() {
      return namesMatch && postalCodesMatch;
    }
  }

  public ValidationResult validateOwners(
      @NonNull List<BusinessOwner> owners, @NonNull List<Owner> plaidOwners) {

    /* It would be wise to split validation according to what is being validated,
     * and to set up different validation by country for postal code as noted below, and to
     * isolate the validation from the source of the data, but for now, this will suffice.
     */
    Set<String> plaidNames =
        plaidOwners.stream()
            .map(Owner::getNames)
            .flatMap(Collection::stream)
            .map(s -> Arrays.asList(s.split("\\s+")))
            .flatMap(Collection::stream)
            .distinct()
            .map(String::toLowerCase)
            .collect(Collectors.toSet());

    /*
     * This validation trims the string to 5 characters - which is shorter than many
     * non-US postal codes (CA comes to mind, with its pattern like "K1A 0A2").  It could be
     * done entirely differently, or a (lambda) function could provide specialization for US
     * ZIP codes, any other countries needing processing to unify values, and a default.
     */
    Set<String> plaidZips =
        plaidOwners.stream()
            .map(Owner::getAddresses)
            .flatMap(Collection::stream)
            .filter(a -> "US".equals(a.getData().getCountry()))
            .map(a -> a.getData().getPostalCode())
            .filter(Objects::nonNull)
            .map(z -> z.trim().substring(0, 5).toLowerCase())
            .collect(Collectors.toSet());

    Set<String> ownersZips =
        owners.stream()
            .map(
                a ->
                    a.getAddress()
                        .getPostalCode()
                        .getEncrypted()
                        .trim()
                        .substring(0, 5)
                        .toLowerCase())
            .collect(Collectors.toSet());

    // last name could include a space - check for all parts of it
    Set<Set<String>> ownersLastNames =
        owners.stream()
            .map(
                businessOwner ->
                    Stream.of(businessOwner.getLastName().getEncrypted().split("\\s+"))
                        .map(String::toLowerCase)
                        .collect(Collectors.toSet()))
            .collect(Collectors.toSet());

    boolean namesMatch = ownersLastNames.stream().anyMatch(plaidNames::containsAll);
    boolean zipsMatch = plaidZips.stream().anyMatch(ownersZips::contains);
    return new ValidationResult(namesMatch, zipsMatch);
  }
}
