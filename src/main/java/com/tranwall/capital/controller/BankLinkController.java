package com.tranwall.capital.controller;

import com.tranwall.capital.service.BankLinkService;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class BankLinkController {

  private final BankLinkService bankLinkService;

  @GetMapping("/bank-link")
  private String linkBank() throws IOException {
    return bankLinkService.getLinkToken();
  }
}
