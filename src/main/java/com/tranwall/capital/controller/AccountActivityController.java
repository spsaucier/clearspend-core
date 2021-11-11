package com.tranwall.capital.controller;

import com.tranwall.capital.controller.type.CurrentUser;
import com.tranwall.capital.controller.type.activity.AccountActivityRequest;
import com.tranwall.capital.controller.type.activity.AccountActivityResponse;
import com.tranwall.capital.controller.type.common.PageRequest;
import com.tranwall.capital.service.AccountActivityFilterCriteria;
import com.tranwall.capital.service.AccountActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/account-activity")
@RequiredArgsConstructor
public class AccountActivityController {

  private final AccountActivityService accountActivityService;

  @PostMapping("")
  private Page<AccountActivityResponse> getAccountActivity(
      @Validated @RequestBody AccountActivityRequest request) {
    return accountActivityService.getFilteredAccountActivity(
        CurrentUser.get().businessId(),
        new AccountActivityFilterCriteria(
            request.getAllocationId(),
            request.getAccountId(),
            null,
            request.getType(),
            request.getFrom(),
            request.getTo(),
            PageRequest.toPageToken(request.getPageRequest())));
  }
}
