package com.clearspend.capital.client.codat.types;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class CodatAccountNested {
  @JsonProperty("id")
  @NonNull
  private String id;

  @JsonProperty("name")
  @NonNull
  private String name;

  @JsonProperty("status")
  private String status;

  @JsonProperty("fullyQualifiedCategory")
  private String category;

  @JsonProperty("fullyQualifiedName")
  private String qualifiedName;

  @JsonProperty("type")
  private String type;

  @JsonProperty("children")
  private List<CodatAccountNested> children = new ArrayList<CodatAccountNested>();
}
