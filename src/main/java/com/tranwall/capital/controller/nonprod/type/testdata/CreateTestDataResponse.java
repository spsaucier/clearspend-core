package com.tranwall.capital.controller.nonprod.type.testdata;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tranwall.capital.data.model.Allocation;
import com.tranwall.capital.data.model.Bin;
import com.tranwall.capital.data.model.Card;
import com.tranwall.capital.data.model.Program;
import com.tranwall.capital.data.model.User;
import com.tranwall.capital.service.UserService.CreateUpdateUserRecord;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

// This can be used until the moment when we will be able to access database

@Data
@AllArgsConstructor
public class CreateTestDataResponse {

  @JsonProperty("bins")
  List<Bin> bins;

  @JsonProperty("programs")
  List<Program> programs;

  @JsonProperty("businesses")
  List<TestBusiness> business;

  @AllArgsConstructor
  public static class TestBusiness {
    @JsonProperty("business")
    com.tranwall.capital.data.model.Business business;

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
