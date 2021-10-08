package com.tranwall.capital.service;

import com.tranwall.capital.client.plaid.PlaidClient;
import com.tranwall.capital.crypto.data.model.embedded.RequiredEncryptedStringWithHash;
import com.tranwall.capital.data.model.BusinessBankAccount;
import com.tranwall.capital.data.repository.BusinessBankAccountRepository;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BusinessBankAccountService {
  @NonNull private BusinessBankAccountRepository businessBankAccountRepository;
  @NonNull private PlaidClient plaidClient;

  public record BusinessBankAccountRecord(
      RequiredEncryptedStringWithHash routingNumber,
      RequiredEncryptedStringWithHash accountNumber,
      RequiredEncryptedStringWithHash accessToken) {}

  @Transactional
  public BusinessBankAccountRecord createBusinessBankAccount(
      String routingNumber, String accountNumber, String accessToken, UUID businessID) {
    BusinessBankAccount businessBankAccount = new BusinessBankAccount(businessID);
    businessBankAccount.setRoutingNumber(new RequiredEncryptedStringWithHash(routingNumber));
    businessBankAccount.setAccountNumber(new RequiredEncryptedStringWithHash(accountNumber));
    businessBankAccount.setAccessToken(new RequiredEncryptedStringWithHash(accessToken));

    businessBankAccountRepository.save(businessBankAccount);

    return new BusinessBankAccountRecord(
        businessBankAccount.getRoutingNumber(),
        businessBankAccount.getAccountNumber(),
        businessBankAccount.getAccessToken());
  }

  public String getLinkToken(UUID businessId) throws IOException {
    return plaidClient.createLinkToken(businessId);
  }

  public List<BusinessBankAccountService.BusinessBankAccountRecord> getAccounts(
      String linkToken, UUID businessId) throws IOException {
    // TODO: Check for already existing access token
    // Will need some unique ID to look up in the database but can't use routing/account since that
    // is not in the plaid metadata

    PlaidClient.AccountsResponse accountsResponse = plaidClient.getAccounts(linkToken);
    return plaidClient.getAccounts(linkToken).achList().stream()
        .map(
            ach -> {
              BusinessBankAccountService.BusinessBankAccountRecord businessBankAccountRecord =
                  new BusinessBankAccountService.BusinessBankAccountRecord(
                      new RequiredEncryptedStringWithHash(ach.getRouting()),
                      new RequiredEncryptedStringWithHash(ach.getAccount()),
                      new RequiredEncryptedStringWithHash(accountsResponse.accessToken()));
              createBusinessBankAccount(
                  ach.getRouting(),
                  ach.getAccount(),
                  accountsResponse.accessToken(),
                  businessId);
              return businessBankAccountRecord;
            })
        .collect(Collectors.toList());
  }
}
