package com.clearspend.capital.service;

import static java.util.stream.Collectors.toList;

import com.clearspend.capital.client.codat.CodatClient;
import com.clearspend.capital.client.codat.types.CodatAccount;
import com.clearspend.capital.client.codat.types.CodatBankAccountsResponse;
import com.clearspend.capital.client.codat.types.CodatCreateBankAccountRequest;
import com.clearspend.capital.client.codat.types.CodatCreateBankAccountResponse;
import com.clearspend.capital.client.codat.types.CodatPushStatusResponse;
import com.clearspend.capital.client.codat.types.CodatSupplier;
import com.clearspend.capital.client.codat.types.CodatSyncDirectCostResponse;
import com.clearspend.capital.client.codat.types.ConnectionStatus;
import com.clearspend.capital.client.codat.types.ConnectionStatusResponse;
import com.clearspend.capital.client.codat.types.CreateCompanyResponse;
import com.clearspend.capital.client.codat.types.GetAccountsResponse;
import com.clearspend.capital.client.codat.types.GetSuppliersResponse;
import com.clearspend.capital.client.codat.types.SyncTransactionResponse;
import com.clearspend.capital.common.typedid.data.AccountActivityId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.data.model.AccountActivity;
import com.clearspend.capital.data.model.TransactionSyncLog;
import com.clearspend.capital.data.model.business.Business;
import com.clearspend.capital.data.model.enums.TransactionSyncStatus;
import com.clearspend.capital.data.repository.TransactionSyncLogRepository;
import com.clearspend.capital.service.type.CurrentUser;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CodatService {

  private final CodatClient codatClient;
  private final AccountActivityService accountActivityService;
  private final BusinessService businessService;
  private final TransactionSyncLogRepository transactionSyncLogRepository;

  @PreAuthorize(
      "hasPermission(#businessId, 'BusinessId', 'CROSS_BUSINESS_BOUNDARY|MANAGE_CONNECTIONS')")
  public String createQboConnectionForBusiness(TypedId<BusinessId> businessId)
      throws RuntimeException {
    Business business = businessService.retrieveBusiness(businessId, true);

    if (business.getCodatCompanyRef() == null) {
      CreateCompanyResponse response =
          codatClient.createCodatCompanyForBusiness(business.getLegalName());
      businessService.updateBusinessWithCodatCompanyRef(business.getId(), response.getId());
    }

    return codatClient.createQboConnectionForBusiness(business.getCodatCompanyRef()).getLinkUrl();
  }

  @PreAuthorize(
      "hasPermission(#businessId, 'BusinessId', 'CROSS_BUSINESS_BOUNDARY|MANAGE_CONNECTIONS')")
  public Boolean getIntegrationConnectionStatus(TypedId<BusinessId> businessId) {
    Business business = businessService.retrieveBusiness(businessId, true);

    if (business.getCodatCompanyRef() == null) {
      return false;
    }

    ConnectionStatusResponse connectionStatusResponse =
        codatClient.getConnectionsForBusiness(business.getCodatCompanyRef());
    return connectionStatusResponse.getResults().stream()
        .anyMatch(connection -> connection.getStatus().equals("Linked"));
  }

  @PreAuthorize(
      "hasPermission(#businessId, 'BusinessId', 'CROSS_BUSINESS_BOUNDARY|MANAGE_CONNECTIONS')")
  public SyncTransactionResponse syncTransactionAsDirectCost(
      TypedId<AccountActivityId> accountActivityId, TypedId<BusinessId> businessId)
      throws RuntimeException {
    Business business = businessService.retrieveBusiness(businessId, true);

    if (business.getCodatCompanyRef() == null) {
      return null;
    }

    AccountActivity accountActivity =
        accountActivityService.retrieveAccountActivity(
            CurrentUser.getBusinessId(), accountActivityId);

    // TODO verify that these values save properly in Codat as expense categories are added

    // TODO Remove hardcoded/placeholder values. (The CodatAccountRefs and CodatContactRef)

    GetSuppliersResponse suppliersResponse =
        codatClient.getSuppliersForBusiness(business.getCodatCompanyRef());

    CodatSupplier supplier =
        supplierForTransaction(accountActivity, suppliersResponse.getResults());

    String connectionId = getConnectionIdForBusiness(business);
    if (supplier != null) {
      // if supplier does exist, use it

      // TODO there could be more than one page of accounts. Fix when we filter for the actual
      // account.
      GetAccountsResponse accountsResponse =
          codatClient.getAccountsForBusiness(business.getCodatCompanyRef());
      Optional<CodatAccount> checkingAccount =
          accountsResponse.getResults().stream()
              .filter(account -> account.getName().equalsIgnoreCase("checking"))
              .findFirst();

      if (checkingAccount.isEmpty()) {
        return new SyncTransactionResponse("FAILED (No checking account)");
      }
      CodatSyncDirectCostResponse syncResponse =
          codatClient.syncTransactionAsDirectCost(
              business.getCodatCompanyRef(),
              connectionId,
              accountActivity,
              business.getCurrency().name(),
              supplier,
              checkingAccount.get());

      transactionSyncLogRepository.save(
          new TransactionSyncLog(
              business.getId(),
              accountActivityId,
              supplier.getId(), // TODO look back at this
              TransactionSyncStatus.IN_PROGRESS,
              syncResponse.getPushOperationKey()));

      return new SyncTransactionResponse("IN_PROGRESS", syncResponse);
    } else {
      // if supplier does not exist, create it

      codatClient.syncSupplierToCodat(
          business.getCodatCompanyRef(),
          connectionId,
          new CodatSupplier(
              "CS-" + accountActivity.getMerchant().getMerchantNumber(),
              accountActivity.getMerchant().getName(),
              "ACTIVE",
              business.getCurrency().name()));

      transactionSyncLogRepository.save(
          new TransactionSyncLog(
              business.getId(),
              accountActivityId,
              "CS-" + accountActivity.getMerchant().getMerchantNumber(), // TODO look back at this
              TransactionSyncStatus.AWAITING_SUPPLIER,
              ""));

      return new SyncTransactionResponse("WAITING_FOR_SUPPLIER");
    }
  }

  @PreAuthorize(
      "hasPermission(#businessId, 'BusinessId', 'CROSS_BUSINESS_BOUNDARY|MANAGE_CONNECTIONS')")
  public CodatBankAccountsResponse getBankAccountsForBusiness(TypedId<BusinessId> businessId) {
    Business currentBusiness = businessService.retrieveBusiness(businessId, true);

    ConnectionStatusResponse connectionStatusResponse =
        codatClient.getConnectionsForBusiness(currentBusiness.getCodatCompanyRef());

    // For now, get the first Linked (active) connection. It should not really be possible for them
    // to link multiple.
    List<ConnectionStatus> linkedConnections =
        connectionStatusResponse.getResults().stream()
            .filter(connectionStatus -> connectionStatus.getStatus().equals("Linked"))
            .collect(toList());

    if (linkedConnections.isEmpty()) {
      return new CodatBankAccountsResponse(new ArrayList<>());
    }
    CodatBankAccountsResponse bankAccounts =
        codatClient.getBankAccountsForBusiness(
            currentBusiness.getCodatCompanyRef(), linkedConnections.get(0).getId());

    return bankAccounts;
  }

  @PreAuthorize(
      "hasPermission(#businessId, 'BusinessId', 'CROSS_BUSINESS_BOUNDARY|MANAGE_CONNECTIONS')")
  public CodatCreateBankAccountResponse createBankAccountForBusiness(
      TypedId<BusinessId> businessId, CodatCreateBankAccountRequest createBankAccountRequest)
      throws RuntimeException {
    Business currentBusiness = businessService.retrieveBusiness(businessId, true);

    ConnectionStatusResponse connectionStatusResponse =
        codatClient.getConnectionsForBusiness(currentBusiness.getCodatCompanyRef());

    // For now, get the first Linked (active) connection. It should not really be possible for them
    // to link multiple.
    List<ConnectionStatus> linkedConnections =
        connectionStatusResponse.getResults().stream()
            .filter(connectionStatus -> connectionStatus.getStatus().equals("Linked"))
            .collect(toList());

    if (linkedConnections.isEmpty()) {
      throw new RuntimeException("Failed to get connection for business");
    }
    return codatClient.createBankAccountForBusiness(
        currentBusiness.getCodatCompanyRef(),
        linkedConnections.get(0).getId(),
        createBankAccountRequest);
  }

  public void syncTransactionsAwaitingSupplier() {
    List<TransactionSyncLog> transactionsWaitingForSupplier =
        transactionSyncLogRepository.findByStatus(TransactionSyncStatus.AWAITING_SUPPLIER);

    transactionsWaitingForSupplier.stream()
        .forEach(
            transaction ->
                syncTransactionIfSupplierExists(transaction, transaction.getBusinessId()));
  }

  private void syncTransactionIfSupplierExists(
      TransactionSyncLog transaction, TypedId<BusinessId> businessId) {
    Business business = businessService.retrieveBusiness(businessId, true);

    GetSuppliersResponse suppliersResponse =
        codatClient.getSuppliersForBusiness(business.getCodatCompanyRef());

    AccountActivity accountActivity =
        accountActivityService.retrieveAccountActivity(
            businessId, transaction.getAccountActivityId());

    CodatSupplier supplier =
        supplierForTransaction(accountActivity, suppliersResponse.getResults());

    if (supplier != null) {
      GetAccountsResponse accountsResponse =
          codatClient.getAccountsForBusiness(business.getCodatCompanyRef());
      Optional<CodatAccount> checkingAccount =
          accountsResponse.getResults().stream()
              .filter(account -> account.getName().equalsIgnoreCase("checking"))
              .findFirst();

      if (checkingAccount.isPresent()) {
        CodatSyncDirectCostResponse syncResponse =
            codatClient.syncTransactionAsDirectCost(
                business.getCodatCompanyRef(),
                getConnectionIdForBusiness(business),
                accountActivity,
                business.getCurrency().name(),
                supplier,
                checkingAccount.get());

        TransactionSyncLog updatedLog =
            new TransactionSyncLog(
                business.getId(),
                accountActivity.getId(),
                supplier.getId(), // TODO look back at this
                TransactionSyncStatus.IN_PROGRESS,
                syncResponse.getPushOperationKey());

        updatedLog.setId(transaction.getId());

        transactionSyncLogRepository.save(updatedLog);
      }
    }
  }

  private CodatSupplier supplierForTransaction(
      AccountActivity accountActivity, List<CodatSupplier> suppliers) {
    Optional<CodatSupplier> matchingSupplier =
        suppliers.stream()
            .filter(
                supplier ->
                    supplier
                        .getId()
                        .equals("CS-" + accountActivity.getMerchant().getMerchantNumber()))
            .findFirst();

    if (matchingSupplier.isEmpty()) {
      return null;
    } else {
      return matchingSupplier.get();
    }
  }

  private String getConnectionIdForBusiness(Business business) {
    ConnectionStatusResponse connectionStatusResponse =
        codatClient.getConnectionsForBusiness(business.getCodatCompanyRef());

    Optional<ConnectionStatus> connectionStatus =
        connectionStatusResponse.getResults().stream()
            .filter(connection -> connection.getStatus().equals("Linked"))
            .findFirst();

    // for now, use the first valid connection
    if (connectionStatus.isEmpty()) {
      return null;
    }

    return connectionStatus.get().getId();
  }

  public void updateSyncedTransactionsInLog() {
    List<TransactionSyncLog> transactionsWaitingForSupplier =
        transactionSyncLogRepository.findByStatus(TransactionSyncStatus.IN_PROGRESS);
    ;

    transactionsWaitingForSupplier.stream()
        .forEach(transaction -> updateSyncStatusIfComplete(transaction));
  }

  private void updateSyncStatusIfComplete(TransactionSyncLog transaction) {
    Business business = businessService.retrieveBusiness(transaction.getBusinessId(), true);
    CodatPushStatusResponse status =
        codatClient.getPushStatus(
            transaction.getDirectCostPushOperationKey(), business.getCodatCompanyRef());

    if (status.getStatus().equals("Success")) {
      transaction.setStatus(TransactionSyncStatus.COMPLETED);
    } else if (status.getStatus().equals("Failed")) {
      transaction.setStatus(TransactionSyncStatus.FAILED);
    }
  }
}
