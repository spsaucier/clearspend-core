package com.clearspend.capital.client.codat.types;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CodatTrackingCategory {
  @JsonProperty("id")
  @NonNull
  private String id;

  @JsonProperty("parentId")
  private String parentId;

  @JsonProperty("name")
  @NonNull
  private String name;

  @JsonProperty("status")
  @NonNull
  private String status;
}
