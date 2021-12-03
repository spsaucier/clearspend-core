package com.tranwall.capital.controller.type.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tranwall.capital.common.masking.annotation.Sensitive;
import com.tranwall.capital.controller.type.common.CardInfo;
import com.tranwall.capital.data.model.User;
import com.tranwall.capital.data.repository.impl.UserRepositoryImpl;
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
  private List<CardInfo> cardInfoList;

  public UserPageData(User user, List<UserRepositoryImpl.CardAndAllocationName> cards) {
    this.userData = new UserData(user);
    this.email = user.getEmail().getEncrypted();
    this.cardInfoList =
        cards != null
            ? cards.stream()
                .map(
                    card ->
                        new CardInfo(
                            card.card().getId(),
                            card.card().getLastFour(),
                            card.allocationName(),
                            user.getFirstName().getEncrypted(),
                            user.getLastName().getEncrypted()))
                .collect(Collectors.toList())
            : Collections.emptyList();
  }
}
