package com.clearspend.capital.service;

import com.clearspend.capital.client.plaid.PlaidClient;
import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.common.error.BalanceNotFoundException;
import com.clearspend.capital.crypto.data.model.embedded.RequiredEncryptedStringWithHash;
import com.clearspend.capital.data.model.business.BusinessBankAccount;
import com.clearspend.capital.data.model.business.BusinessBankAccountBalance;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.data.repository.business.BusinessBankAccountBalanceRepository;
import com.clearspend.capital.data.repository.business.BusinessBankAccountRepository;
import com.plaid.client.model.AccountBalance;
import com.plaid.client.model.AccountBase;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.transaction.Transactional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class BusinessBankAccountBalanceService {

  private final BusinessBankAccountBalanceRepository businessBankAccountBalanceRepository;
  private final BusinessBankAccountRepository businessBankAccountRepository;
  private final PlaidClient plaidClient;

  @PreAuthorize("hasRootPermission(#businessBankAccount, 'LINK_BANK_ACCOUNTS')")
  public BusinessBankAccountBalance createBusinessBankAccountBalance(
      @NonNull BusinessBankAccount businessBankAccount,
      Amount current,
      Amount available,
      Amount limit) {
    BusinessBankAccountBalance balance =
        new BusinessBankAccountBalance(businessBankAccount.getId());
    balance.setCurrent(current);
    balance.setAvailable(available);
    balance.setLimit(limit);

    return businessBankAccountBalanceRepository.save(balance);
  }

  @PreAuthorize("hasRootPermission(#businessBankAccount, 'LINK_BANK_ACCOUNTS')")
  public BusinessBankAccountBalance createBusinessBankAccountBalance(
      @NonNull BusinessBankAccount businessBankAccount, @NonNull AccountBalance balance) {
    Function<Double, Amount> amountFactory =
        (amount) ->
            amount != null
                ? new Amount(
                    Currency.valueOf(balance.getIsoCurrencyCode()), BigDecimal.valueOf(amount))
                : null;
    Amount current = amountFactory.apply(balance.getCurrent());
    Amount available = amountFactory.apply(balance.getAvailable());
    Amount limit = amountFactory.apply(balance.getLimit());

    return createBusinessBankAccountBalance(businessBankAccount, current, available, limit);
  }

  /**
   * Fetch the latest balance. (This also updates balances of other related connected plaid
   * accounts, because that's how Plaid works.)
   *
   * @param businessBankAccount for the account needing a balance check
   * @return a new balance record for that account. This may not be up-to-the-minute data depending
   *     on the specific financial institution reporting strategy.
   */
  @Transactional
  @PreAuthorize("hasRootPermission(#businessBankAccount, 'LINK_BANK_ACCOUNTS')")
  public @NonNull BusinessBankAccountBalance getNewBalance(
      final BusinessBankAccount businessBankAccount) throws IOException {

    @NonNull
    final RequiredEncryptedStringWithHash accessToken = businessBankAccount.getAccessToken();

    final List<AccountBase> accountBases =
        plaidClient.getBalances(accessToken.getEncrypted(), businessBankAccount.getBusinessId());
    final Map<String, BusinessBankAccount> dbBusinessBankAccounts =
        businessBankAccountRepository.findAllByAccessToken(accessToken).stream()
            .collect(Collectors.toMap(a -> a.getPlaidAccountRef().getEncrypted(), a -> a));

    BusinessBankAccountBalance returnBalance = null;

    for (AccountBase accountBase : accountBases) {
      final BusinessBankAccount businessWithLoopBBAID =
          dbBusinessBankAccounts.get(accountBase.getAccountId());

      if (businessWithLoopBBAID == null) {
        log.info("Unrecognized Plaid account came back in balance check.");
        continue;
      }

      BusinessBankAccountBalance loopBalance =
          createBusinessBankAccountBalance(businessWithLoopBBAID, accountBase.getBalances());
      if (businessWithLoopBBAID.getId().equals(businessBankAccount.getId())) {
        returnBalance = loopBalance;
      }
    }

    return Optional.ofNullable(returnBalance)
        .orElseThrow(() -> new BalanceNotFoundException(businessBankAccount.getId()));
  }
}
