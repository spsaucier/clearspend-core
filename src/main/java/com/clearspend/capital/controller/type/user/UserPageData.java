package com.clearspend.capital.controller.type.user;

import com.clearspend.capital.common.masking.annotation.Sensitive;
import com.clearspend.capital.controller.type.common.CardInfo;
import com.clearspend.capital.data.model.User;
import com.clearspend.capital.data.repository.impl.UserRepositoryImpl;
import com.fasterxml.jackson.annotation.JsonProperty;
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

  @JsonProperty("archived")
  @NonNull
  private Boolean archived;

  @JsonProperty("cardInfoList")
  @NonNull
  private List<CardInfo> cardInfoList;

  public UserPageData(User user, List<UserRepositoryImpl.CardAndAllocationName> cards) {
    this.userData = new UserData(user);
    this.email = user.getEmail().getEncrypted();
    this.archived = user.isArchived();
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
