package com.tranwall.capital.service;

import com.plaid.client.model.AccountBalance;
import com.plaid.client.model.AccountBase;
import com.tranwall.capital.client.plaid.PlaidClient;
import com.tranwall.capital.common.data.model.Amount;
import com.tranwall.capital.common.data.model.TypedMutable;
import com.tranwall.capital.common.typedid.data.BusinessBankAccountId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.crypto.data.model.embedded.RequiredEncryptedStringWithHash;
import com.tranwall.capital.data.model.BusinessBankAccountBalance;
import com.tranwall.capital.data.model.enums.Currency;
import com.tranwall.capital.data.repository.BusinessBankAccountBalanceRepository;
import com.tranwall.capital.data.repository.BusinessBankAccountRepository;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.transaction.Transactional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class BusinessBankAccountBalanceService {

  private final BusinessBankAccountBalanceRepository businessBankAccountBalanceRepository;
  private final BusinessBankAccountRepository businessBankAccountRepository;
  private final PlaidClient plaidClient;

  public BusinessBankAccountBalance createBusinessBankAccountBalance(
      @NonNull TypedId<BusinessBankAccountId> busunessBankAccountId,
      Amount current,
      Amount available,
      Amount limit) {
    BusinessBankAccountBalance balance = new BusinessBankAccountBalance(busunessBankAccountId);
    balance.setCurrent(current);
    balance.setAvailable(available);
    balance.setLimit(limit);

    return businessBankAccountBalanceRepository.save(balance);
  }

  public BusinessBankAccountBalance createBusinessBankAccountBalance(
      @NonNull TypedId<BusinessBankAccountId> busunessBankAccountId,
      @NonNull AccountBalance balance) {
    Function<Double, Amount> amountFactory =
        (amount) ->
            amount != null
                ? new Amount(
                    Currency.valueOf(balance.getIsoCurrencyCode()), BigDecimal.valueOf(amount))
                : null;
    Amount current = amountFactory.apply(balance.getCurrent());
    Amount available = amountFactory.apply(balance.getAvailable());
    Amount limit = amountFactory.apply(balance.getLimit());

    return createBusinessBankAccountBalance(busunessBankAccountId, current, available, limit);
  }

  /**
   * Fetch the latest balance. (This also updates balances of other related connected plaid
   * accounts, because that's how Plaid works.)
   *
   * @param businessBankAccountId for the account needing a balance check
   * @return a new balance record for that account. This may not be up-to-the-minute data depending
   *     on the specific financial institution reporting strategy.
   */
  @Transactional
  public @NonNull BusinessBankAccountBalance getNewBalance(
      TypedId<BusinessBankAccountId> businessBankAccountId) throws IOException {

    @NonNull
    RequiredEncryptedStringWithHash accessToken =
        businessBankAccountRepository
            .findById(businessBankAccountId)
            .orElseThrow(NoSuchElementException::new)
            .getAccessToken();

    List<AccountBase> accountBases = plaidClient.getBalances(accessToken.getEncrypted());
    Map<String, TypedId<BusinessBankAccountId>> businessBankAccounts =
        businessBankAccountRepository.findAllByAccessToken(accessToken).stream()
            .collect(
                Collectors.toMap(a -> a.getPlaidAccountRef().getEncrypted(), TypedMutable::getId));
    BusinessBankAccountBalance returnBalance = null;

    for (AccountBase accountBase : accountBases) {
      TypedId<BusinessBankAccountId> loopBBAID =
          businessBankAccounts.get(accountBase.getAccountId());

      if (loopBBAID == null) {
        log.info("Unrecognized Plaid account came back in balance check.");
        continue;
      }

      BusinessBankAccountBalance loopBalance =
          createBusinessBankAccountBalance(loopBBAID, accountBase.getBalances());
      if (loopBBAID.equals(businessBankAccountId)) {
        returnBalance = loopBalance;
      }
    }

    Objects.requireNonNull(returnBalance);

    return returnBalance;
  }
}
