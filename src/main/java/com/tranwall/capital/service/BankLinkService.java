package com.tranwall.capital.service;

import com.tranwall.capital.client.plaid.PlaidClient;
import java.io.IOException;
import lombok.Data;
import lombok.NonNull;
import org.springframework.stereotype.Service;

@Service
@Data
public class BankLinkService {
	@NonNull private PlaidClient plaidClient;

	public void getLinkToken() throws IOException {
		plaidClient.createLinkToken();
	}
}
