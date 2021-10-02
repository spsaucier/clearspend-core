package com.tranwall.capital.service;

import com.tranwall.capital.client.plaid.PlaidClient;
import java.io.IOException;
import lombok.Data;
import lombok.NonNull;
import org.springframework.stereotype.Service;

@Service
@Data
public class BankLinkService {
  @NonNull private PlaidClient plaidService;

  public String getLinkToken() throws IOException {
    return plaidService.createLinkToken();
  }

  public String getAccounts(String linkToken) throws IOException {
    return plaidService.getAccounts(linkToken);
  }
}
