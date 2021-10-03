package com.tranwall.capital.controller;

import com.tranwall.capital.service.BusinessBankAccountService;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import lombok.Data;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController("business-bank-account")
@CrossOrigin
@Data
@Slf4j
public class BusinessBankAccountController {
  @NonNull private BusinessBankAccountService businessBankAccountService;

  @GetMapping("/link-token/{businessId}")
  private String linkToken(@PathVariable UUID businessId) throws IOException {
    //TODO: Get business UUID from JWT
    return businessBankAccountService.getLinkToken(businessId);
  }

  @GetMapping(value = "/accounts/{linkToken}", produces = MediaType.APPLICATION_JSON_VALUE)
  private List<BusinessBankAccountService.BusinessBankAccountRecord> accounts(
      @PathVariable String linkToken) throws IOException {
    //TODO: Get business UUID from JWT
    return businessBankAccountService.getAccounts(
        linkToken, UUID.fromString("1a2c8d0d-0d4c-4bc7-aa14-1e5b638e97ff"));
  }
}
