package com.clearspend.capital.client.codat.types;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class GetTrackingCategoriesResponsePage {
  @JsonProperty("results")
  @NonNull
  private List<CodatTrackingCategory> results;

  @JsonProperty("pageNumber")
  @NonNull
  private Integer pageNumber;

  @JsonProperty("pageSize")
  @NonNull
  private Integer pageSize;

  @JsonProperty("totalResults")
  @NonNull
  private Integer totalResults;

  @JsonProperty("_links")
  @NonNull
  private Map<String, HateoasLink> links;
}
