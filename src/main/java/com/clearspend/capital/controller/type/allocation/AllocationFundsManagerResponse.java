package com.clearspend.capital.controller.type.allocation;

import com.clearspend.capital.controller.type.user.UserData;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;

@Data
@AllArgsConstructor
public class AllocationFundsManagerResponse {
  @JsonProperty("userDataList")
  @NonNull
  @NotNull(message = "userDataList required")
  private List<UserData> userData;
}
