package com.tranwall.capital.service;

import com.tranwall.capital.client.plaid.PlaidClient;
import java.io.IOException;
import lombok.Data;
import org.springframework.stereotype.Service;

@Service
@Data
public class BankLinkService {

  private final PlaidClient plaidClient;

  public String getLinkToken() throws IOException {
    return plaidClient.createLinkToken();
  }
}
