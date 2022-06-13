package com.clearspend.capital.service;

import com.clearspend.capital.client.stripe.StripeClient;
import com.clearspend.capital.client.stripe.types.FinancialAccount;
import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.data.repository.ClearspendRepository;
import com.clearspend.capital.data.repository.ClearspendRepository.Balance;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ClearspendService {

  private final ClearspendRepository clearspendRepository;
  private final StripeClient stripeClient;

  public record FinancialAccountBalance(
      Amount totalBalance, Amount businessBalance, Amount availableBalance) {}

  FinancialAccountBalance getFinancialAccountBalance() {
    FinancialAccount clearspendFinancialAccount = stripeClient.getClearspendFinancialAccount();
    Amount totalBalance =
        Amount.fromStripeAmount(
            Currency.USD,
            clearspendFinancialAccount.getBalance().getCash().get(Currency.USD.toStripeCurrency()));

    Balance businessBalance = clearspendRepository.retrieveTotalBusinessBalance();

    return new FinancialAccountBalance(
        totalBalance,
        businessBalance.availableBalance(),
        totalBalance.sub(businessBalance.availableBalance()));
  }
}
