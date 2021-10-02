package com.tranwall.capital.controller;

import com.tranwall.capital.service.BankLinkService;
import java.io.IOException;
import lombok.Data;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin
@Data
@Slf4j
public class BankLinkController {
  @NonNull private BankLinkService bankLinkService;

  @GetMapping("/link-token")
  private String linkToken() throws IOException {
    return bankLinkService.getLinkToken();
  }

  @GetMapping(value = "/accounts/{linkToken}", produces = MediaType.APPLICATION_JSON_VALUE)
  private String accounts(@PathVariable String linkToken) throws IOException {
    return bankLinkService.getAccounts(linkToken);
  }
}
