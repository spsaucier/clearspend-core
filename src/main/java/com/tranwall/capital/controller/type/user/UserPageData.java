package com.tranwall.capital.controller.type.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tranwall.capital.common.masking.annotation.Sensitive;
import com.tranwall.capital.data.model.Card;
import com.tranwall.capital.data.model.User;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;

@Data
@AllArgsConstructor
public class UserPageData {

  @JsonProperty("userData")
  @NonNull
  private UserData userData;

  @Sensitive
  @JsonProperty("email")
  @NonNull
  private String email;

  @JsonProperty("cardInfoList")
  @NonNull
  private List<String> cardNumberList;

  public UserPageData(User user, List<Card> cards) {
    this.userData = new UserData(user);
    this.email = user.getEmail().getEncrypted();
    this.cardNumberList =
        cards != null
            ? cards.stream()
                .map(card -> card.getCardNumber().getEncrypted())
                .collect(Collectors.toList())
            : Collections.emptyList();
  }
}
