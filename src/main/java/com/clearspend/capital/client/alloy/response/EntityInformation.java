package com.clearspend.capital.client.alloy.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.sql.Timestamp;
import java.util.List;
import lombok.Value;

@Value
public class EntityInformation {

  @JsonProperty("name")
  String name;

  @JsonProperty("type")
  String type;

  @JsonProperty("entity_token")
  String entityToken;

  @JsonProperty("archived")
  Boolean archived;

  @JsonProperty("created")
  Timestamp created;

  @JsonProperty("evaluations")
  List<Evaluation> evaluations;

  @JsonProperty("documents")
  List<Document> documents;

  @JsonProperty("reviews")
  List<Review> reviews;
}
