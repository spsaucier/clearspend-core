package com.clearspend.capital.client.codat.types;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class CodatAccount {
  @JsonProperty("id")
  @NonNull
  private String id;

  @JsonProperty("name")
  @NonNull
  private String name;

  @JsonProperty("status")
  @NonNull
  private CodatAccountStatus status;

  @JsonProperty("fullyQualifiedCategory")
  @NonNull
  private String category;

  @JsonProperty("fullyQualifiedName")
  @NonNull
  private String qualifiedName;

  @JsonProperty("type")
  @NonNull
  private CodatAccountType type;
}
