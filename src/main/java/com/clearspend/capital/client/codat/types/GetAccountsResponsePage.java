package com.clearspend.capital.client.codat.types;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;

@Data
@RequiredArgsConstructor
public class GetAccountsResponsePage {
  @JsonProperty("results")
  @NonNull
  private List<CodatAccount> results;

  @JsonProperty("pageNumber")
  private Integer pageNumber;

  @JsonProperty("pageSize")
  private Integer pageSize;

  @JsonProperty("totalResults")
  private Integer totalResults;

  @JsonProperty("_links")
  private Map<String, HateoasLink> links;
}
