package com.tranwall.capital.controller;

import com.tranwall.capital.common.error.ForbiddenException;
import com.tranwall.capital.common.typedid.data.AccountActivityId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.controller.type.CurrentUser;
import com.tranwall.capital.controller.type.PagedData;
import com.tranwall.capital.controller.type.activity.AccountActivityRequest;
import com.tranwall.capital.controller.type.activity.AccountActivityResponse;
import com.tranwall.capital.controller.type.common.PageRequest;
import com.tranwall.capital.data.model.AccountActivity;
import com.tranwall.capital.data.repository.AccountActivityRepository;
import com.tranwall.capital.data.repository.AccountActivityRepositoryCustom.FilteredAccountActivityRecord;
import com.tranwall.capital.service.AccountActivityFilterCriteria;
import com.tranwall.capital.service.AccountActivityService;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/account-activity")
@RequiredArgsConstructor
public class AccountActivityController {

  private final AccountActivityService accountActivityService;

  private final AccountActivityRepository accountActivityRepository;

  @PostMapping("")
  private PagedData<AccountActivityResponse> retrieveAccountActivityPage(
      @Validated @RequestBody AccountActivityRequest request) {
    Page<FilteredAccountActivityRecord> filteredAccountActivity =
        accountActivityRepository.find(
            CurrentUser.get().businessId(),
            new AccountActivityFilterCriteria(
                request.getAllocationId(),
                request.getUserId(),
                request.getCardId(),
                request.getType(),
                request.getSearchText(),
                request.getFrom(),
                request.getTo(),
                PageRequest.toPageToken(request.getPageRequest())));

    return PagedData.of(filteredAccountActivity, AccountActivityResponse::new);
  }

  @GetMapping("/{accountActivityId}")
  private AccountActivityResponse getAccountActivity(
      @PathVariable(value = "accountActivityId")
          @Parameter(
              required = true,
              name = "accountActivityId",
              description = "ID of the transaction record.",
              example = "48104ecb-1343-4cc1-b6f2-e6cc88e9a80f")
          TypedId<AccountActivityId> accountActivityId) {
    AccountActivity accountActivity =
        accountActivityService.retrieveAccountActivity(accountActivityId);
    // check if user has rights to read this information
    if (CurrentUser.get().businessId() != accountActivity.getBusinessId()) {
      throw new ForbiddenException();
    }

    return new AccountActivityResponse(accountActivity);
  }
}
