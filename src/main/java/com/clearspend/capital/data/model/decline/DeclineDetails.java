package com.clearspend.capital.data.model.decline;

import com.clearspend.capital.data.model.enums.network.DeclineReason;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    property = "reason",
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    visible = true,
    defaultImpl = DeclineDetails.class)
@JsonSubTypes({
  @Type(value = AddressPostalCodeMismatch.class, name = "ADDRESS_POSTAL_CODE_MISMATCH")
})
@Getter
@Setter
@EqualsAndHashCode
public class DeclineDetails {

  @NonNull
  @JsonProperty("reason")
  private DeclineReason reason;

  @JsonCreator
  public DeclineDetails(DeclineReason reason) {
    this.reason = reason;
  }
}
