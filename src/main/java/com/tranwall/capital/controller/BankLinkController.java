package com.tranwall.capital.controller;

import com.tranwall.capital.service.BankLinkService;
import java.io.IOException;
import lombok.Data;
import lombok.NonNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Data
public class BankLinkController {
  @NonNull private BankLinkService bankLinkService;

  @GetMapping("/bankLink")
  private String linkBank() throws IOException {
    bankLinkService.getLinkToken();
    return "done";
  }
}
