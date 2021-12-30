package com.clearspend.capital.controller.nonprod.type.testdata;

import com.clearspend.capital.data.model.Allocation;
import com.clearspend.capital.data.model.Business;
import com.clearspend.capital.data.model.Card;
import com.clearspend.capital.data.model.User;
import com.clearspend.capital.service.UserService.CreateUpdateUserRecord;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

// This can be used until the moment when we will be able to access database

@Data
@AllArgsConstructor
public class CreateTestDataResponse {

  @JsonProperty("businesses")
  List<TestBusiness> business;

  @AllArgsConstructor
  public static class TestBusiness {
    @JsonProperty("business")
    Business business;

    @JsonProperty("users")
    List<User> users;

    @JsonProperty("allocations")
    List<Allocation> allocations;

    @JsonProperty("cards")
    List<Card> cards;

    @JsonProperty("createUserRecords")
    List<CreateUpdateUserRecord> createUpdateUserRecords;
  }
}
