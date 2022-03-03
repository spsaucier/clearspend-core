package com.clearspend.capital.controller.type.card;

import com.clearspend.capital.common.typedid.data.CardId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class EphemeralKeyRequest {

  @JsonProperty("cardId")
  @NonNull
  private TypedId<CardId> cardId;

  @JsonProperty("apiVersion")
  @NonNull
  private String apiVersion;
}
