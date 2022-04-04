package com.clearspend.capital.data.model.decline;

import com.clearspend.capital.data.model.enums.network.DeclineReason;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class AddressPostalCodeMismatch extends DeclineDetails {

  @NonNull
  @JsonProperty("postalCode")
  private String postalCode;

  @JsonCreator
  public AddressPostalCodeMismatch(String postalCode) {
    super(DeclineReason.ADDRESS_POSTAL_CODE_MISMATCH);
    this.postalCode = postalCode;
  }
}
