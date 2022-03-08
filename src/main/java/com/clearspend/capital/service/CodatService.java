package com.clearspend.capital.service;

import static java.util.stream.Collectors.toList;

import com.clearspend.capital.client.codat.CodatClient;
import com.clearspend.capital.client.codat.types.CodatAccount;
import com.clearspend.capital.client.codat.types.CodatAccountNested;
import com.clearspend.capital.client.codat.types.CodatAccountNestedResponse;
import com.clearspend.capital.client.codat.types.CodatAccountType;
import com.clearspend.capital.client.codat.types.CodatBankAccountStatusResponse;
import com.clearspend.capital.client.codat.types.CodatBankAccountsResponse;
import com.clearspend.capital.client.codat.types.CodatCreateBankAccountRequest;
import com.clearspend.capital.client.codat.types.CodatCreateBankAccountResponse;
import com.clearspend.capital.client.codat.types.CodatPushDataResponse;
import com.clearspend.capital.client.codat.types.CodatPushStatusResponse;
import com.clearspend.capital.client.codat.types.CodatSupplier;
import com.clearspend.capital.client.codat.types.CodatSupplierRequest;
import com.clearspend.capital.client.codat.types.CodatSyncDirectCostResponse;
import com.clearspend.capital.client.codat.types.CreateCompanyResponse;
import com.clearspend.capital.client.codat.types.GetAccountsResponse;
import com.clearspend.capital.client.codat.types.GetSuppliersResponse;
import com.clearspend.capital.client.codat.types.SyncTransactionResponse;
import com.clearspend.capital.common.typedid.data.AccountActivityId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.data.model.AccountActivity;
import com.clearspend.capital.data.model.TransactionSyncLog;
import com.clearspend.capital.data.model.User;
import com.clearspend.capital.data.model.business.Business;
import com.clearspend.capital.data.model.enums.AccountingSetupStep;
import com.clearspend.capital.data.model.enums.TransactionSyncStatus;
import com.clearspend.capital.data.repository.TransactionSyncLogRepository;
import com.clearspend.capital.data.repository.business.BusinessRepository;
import com.clearspend.capital.service.type.CurrentUser;
import java.util.ArrayList;
import java.util.Arrays;
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
  private final UserService userService;
  private final BusinessRepository businessRepository;

  @PreAuthorize(
      "hasPermission(#businessId, 'BusinessId', 'CROSS_BUSINESS_BOUNDARY|MANAGE_CONNECTIONS')")
  public String createQboConnectionForBusiness(TypedId<BusinessId> businessId)
      throws RuntimeException {
    Business business = businessService.retrieveBusiness(businessId, true);

    if (business.getCodatCompanyRef() == null) {
      CreateCompanyResponse response =
          codatClient.createCodatCompanyForBusiness(business.getLegalName());
      business =
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

    return business.getCodatConnectionId() != null;
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

    GetSuppliersResponse suppliersResponse =
        codatClient.getSuppliersForBusiness(business.getCodatCompanyRef());

    CodatSupplier supplier =
        supplierForTransaction(accountActivity, suppliersResponse.getResults());

    String connectionId = business.getCodatConnectionId();
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

      User currentUserDetails = userService.retrieveUser(CurrentUser.getUserId());
      transactionSyncLogRepository.save(
          new TransactionSyncLog(
              business.getId(),
              accountActivityId,
              supplier.getId(), // TODO look back at this
              TransactionSyncStatus.IN_PROGRESS,
              syncResponse.getPushOperationKey(),
              business.getCodatCompanyRef(),
              currentUserDetails.getFirstName(),
              currentUserDetails.getLastName()));

      return new SyncTransactionResponse("IN_PROGRESS", syncResponse);
    } else {
      // if supplier does not exist, create it

      CodatPushDataResponse response =
          codatClient.syncSupplierToCodat(
              business.getCodatCompanyRef(),
              connectionId,
              new CodatSupplierRequest(
                  accountActivity.getMerchant().getName(),
                  "ACTIVE",
                  business.getCurrency().name()));

      User currentUserDetails = userService.retrieveUser(CurrentUser.getUserId());

      transactionSyncLogRepository.save(
          new TransactionSyncLog(
              business.getId(),
              accountActivityId,
              "", // TODO look back at this
              TransactionSyncStatus.AWAITING_SUPPLIER,
              response.getPushOperationKey(),
              business.getCodatCompanyRef(),
              currentUserDetails.getFirstName(),
              currentUserDetails.getLastName()));

      return new SyncTransactionResponse("WAITING_FOR_SUPPLIER");
    }
  }

  @PreAuthorize(
      "hasPermission(#businessId, 'BusinessId', 'CROSS_BUSINESS_BOUNDARY|MANAGE_CONNECTIONS')")
  public CodatBankAccountsResponse getBankAccountsForBusiness(TypedId<BusinessId> businessId) {
    Business currentBusiness = businessService.retrieveBusiness(businessId, true);

    CodatBankAccountsResponse bankAccounts =
        codatClient.getBankAccountsForBusiness(
            currentBusiness.getCodatCompanyRef(), currentBusiness.getCodatConnectionId());

    return bankAccounts;
  }

  @PreAuthorize(
      "hasPermission(#businessId, 'BusinessId', 'CROSS_BUSINESS_BOUNDARY|MANAGE_CONNECTIONS')")
  public CodatAccountNestedResponse getChartOfAccountsForBusiness(TypedId<BusinessId> businessId) {
    Business currentBusiness = businessService.retrieveBusiness(businessId, true);

    GetAccountsResponse chartOfAccounts =
        codatClient.getAccountsForBusiness(currentBusiness.getCodatCompanyRef());

    return new CodatAccountNestedResponse(nestCodatAccounts(chartOfAccounts.getResults()));
  }

  @PreAuthorize(
      "hasPermission(#businessId, 'BusinessId', 'CROSS_BUSINESS_BOUNDARY|MANAGE_CONNECTIONS')")
  public CodatAccountNestedResponse getChartOfAccountsForBusiness(
      TypedId<BusinessId> businessId, CodatAccountType type) {
    Business currentBusiness = businessService.retrieveBusiness(businessId, true);

    GetAccountsResponse chartOfAccounts =
        codatClient.getAccountsForBusiness(currentBusiness.getCodatCompanyRef());

    List<CodatAccount> response =
        chartOfAccounts.getResults().stream()
            .filter(account -> account.getType().equals(type))
            .collect(toList());

    return new CodatAccountNestedResponse(nestCodatAccounts(response));
  }

  @PreAuthorize(
      "hasPermission(#businessId, 'BusinessId', 'CROSS_BUSINESS_BOUNDARY|MANAGE_CONNECTIONS')")
  public CodatCreateBankAccountResponse createBankAccountForBusiness(
      TypedId<BusinessId> businessId, CodatCreateBankAccountRequest createBankAccountRequest)
      throws RuntimeException {
    Business currentBusiness = businessService.retrieveBusiness(businessId, true);

    return codatClient.createBankAccountForBusiness(
        currentBusiness.getCodatCompanyRef(),
        currentBusiness.getCodatConnectionId(),
        createBankAccountRequest);
  }

  @PreAuthorize(
      "hasPermission(#businessId, 'BusinessId', 'CROSS_BUSINESS_BOUNDARY|MANAGE_CONNECTIONS')")
  public Boolean deleteCodatIntegrationConnection(TypedId<BusinessId> businessId)
      throws RuntimeException {
    Business currentBusiness = businessService.retrieveBusiness(businessId, true);

    Boolean deleteResult =
        codatClient.deleteCodatIntegrationConnectionForBusiness(
            currentBusiness.getCodatCompanyRef(), currentBusiness.getCodatConnectionId());

    if (deleteResult) {
      businessService.updateBusinessAccountingSetupStep(
          businessId, AccountingSetupStep.ADD_CREDIT_CARD);
      businessService.deleteCodatConnectionForBusiness(businessId);
    }

    return deleteResult;
  }

  // TODO: Nesting off of fully qualified name swap to codat method when changes are implements on
  // their end.
  private List<CodatAccountNested> nestCodatAccounts(List<CodatAccount> accounts) {
    List<CodatAccountNested> base = new ArrayList<CodatAccountNested>();
    accounts.stream()
        .forEach(
            account -> {
              CodatAccountNested currentAccount = null;

              // Get rid of fluff
              String[] baseElements = account.getQualifiedName().split("\\.");
              String[] elements = Arrays.copyOfRange(baseElements, 1, baseElements.length);
              for (String element : elements) {
                if (currentAccount == null) {
                  currentAccount = findAccountInList(base, element);
                  if (currentAccount == null) {
                    currentAccount = createNestedAccountFromAccount(account, element);
                    base.add(currentAccount);
                  }
                } else {
                  CodatAccountNested newAccount =
                      findAccountInList(currentAccount.getChildren(), element);
                  if (newAccount == null) {
                    newAccount = createNestedAccountFromAccount(account, element);
                    currentAccount.getChildren().add(newAccount);
                  }
                  currentAccount = newAccount;
                }
              }
              if (currentAccount != null) {
                // reorient leaf node
                currentAccount.setId(account.getId());
                currentAccount.setName(account.getName());
                currentAccount.setStatus(account.getStatus().getName());
                currentAccount.setCategory(account.getCategory());
                currentAccount.setQualifiedName(account.getQualifiedName());
                currentAccount.setType(account.getType().getName());
              }
            });

    return base;
  }

  private CodatAccountNested createNestedAccountFromAccount(CodatAccount account, String id) {
    CodatAccountNested newAccount = new CodatAccountNested(id, id);
    newAccount.setStatus(account.getStatus().getName());
    newAccount.setCategory(account.getCategory());
    newAccount.setQualifiedName(id);
    newAccount.setType(account.getType().getName());
    return newAccount;
  }

  private CodatAccountNested findAccountInList(List<CodatAccountNested> accounts, String search) {
    return accounts.stream()
        .filter(account -> account.getId().equals(search))
        .findFirst()
        .orElse(null);
  }

  public void syncTransactionsAwaitingSupplierForCompany(String companyRef) {
    List<TransactionSyncLog> transactionsWaitingForSupplier =
        transactionSyncLogRepository.findByStatusAndCodatCompanyRef(
            TransactionSyncStatus.AWAITING_SUPPLIER, companyRef);

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
                business.getCodatConnectionId(),
                accountActivity,
                business.getCurrency().name(),
                supplier,
                checkingAccount.get());

        Optional<TransactionSyncLog> transactionSyncLogOptional =
            transactionSyncLogRepository.findById(transaction.getId());

        if (transactionSyncLogOptional.isEmpty()) {
          return;
        }

        TransactionSyncLog transactionSyncLog = transactionSyncLogOptional.get();

        transactionSyncLog.setStatus(TransactionSyncStatus.IN_PROGRESS);
        transactionSyncLog.setDirectCostPushOperationKey(syncResponse.getPushOperationKey());
        transactionSyncLogRepository.saveAndFlush(transactionSyncLog);
      }
    }
  }

  private CodatSupplier supplierForTransaction(
      AccountActivity accountActivity, List<CodatSupplier> suppliers) {
    Optional<CodatSupplier> matchingSupplier =
        suppliers.stream()
            .filter(
                supplier ->
                    supplier.getSupplierName().equals(accountActivity.getMerchant().getName()))
            .findFirst();

    if (matchingSupplier.isEmpty()) {
      return null;
    } else {
      return matchingSupplier.get();
    }
  }

  public void updateSyncedTransactionsInLog(String companyRef) {
    List<TransactionSyncLog> transactionsWaitingForSupplier =
        transactionSyncLogRepository.findByStatusAndCodatCompanyRef(
            TransactionSyncStatus.IN_PROGRESS, companyRef);
    ;

    transactionsWaitingForSupplier.stream()
        .forEach(transaction -> updateSyncStatusIfComplete(transaction));
  }

  private void updateSyncStatusIfComplete(TransactionSyncLog transaction) {
    Business business = businessService.retrieveBusiness(transaction.getBusinessId(), true);
    CodatPushStatusResponse status =
        codatClient.getPushStatus(
            transaction.getDirectCostPushOperationKey(), business.getCodatCompanyRef());

    Optional<TransactionSyncLog> transactionSyncLogOptional =
        transactionSyncLogRepository.findById(transaction.getId());

    if (transactionSyncLogOptional.isEmpty()) {
      return;
    }

    TransactionSyncLog transactionSyncLog = transactionSyncLogOptional.get();

    if (status.getStatus().equals("Success")) {
      transactionSyncLog.setStatus(TransactionSyncStatus.COMPLETED);
    } else if (status.getStatus().equals("Failed")) {
      transactionSyncLog.setStatus(TransactionSyncStatus.FAILED);
    }

    transactionSyncLogRepository.save(transactionSyncLog);
  }

  public void syncTransactionAwaitingSupplier(String companyId, String pushOperationKey) {
    Optional<TransactionSyncLog> syncForKey =
        transactionSyncLogRepository.findByDirectCostPushOperationKey(pushOperationKey);

    if (syncForKey.isEmpty()) {
      return;
    }

    syncTransactionIfSupplierExists(syncForKey.get(), syncForKey.get().getBusinessId());
  }

  public void updateStatusForSyncedTransaction(String companyId, String pushOperationKey) {
    Optional<TransactionSyncLog> syncForKey =
        transactionSyncLogRepository.findByDirectCostPushOperationKey(pushOperationKey);

    if (syncForKey.isEmpty()) {
      return;
    }

    updateSyncStatusIfComplete(syncForKey.get());
  }

  public void updateConnectionIdForBusiness(String codatCompanyRef, String dataConnectionId) {
    businessRepository
        .findByCodatCompanyRef(codatCompanyRef)
        .ifPresent(
            business ->
                businessService.updateBusinessWithCodatConnectionId(
                    business.getId(), dataConnectionId));
  }

  public void updateCodatBankAccountForBusiness(String codatCompanyRef, String pushOperationKey) {
    CodatBankAccountStatusResponse accountStatus =
        codatClient.getBankAccountDetails(pushOperationKey, codatCompanyRef);
    if (accountStatus.getStatus().equals("Success")) {
      businessRepository
          .findByCodatCompanyRef(codatCompanyRef)
          .ifPresent(
              business ->
                  businessService.updateCodatCreditCardForBusiness(
                      business.getId(), accountStatus.getData().getId()));
    }
  }
}
