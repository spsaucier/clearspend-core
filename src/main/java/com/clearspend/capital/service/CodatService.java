package com.clearspend.capital.service;

import static java.util.stream.Collectors.toList;

import com.clearspend.capital.client.codat.CodatClient;
import com.clearspend.capital.client.codat.types.CodatAccount;
import com.clearspend.capital.client.codat.types.CodatAccountNested;
import com.clearspend.capital.client.codat.types.CodatAccountNestedResponse;
import com.clearspend.capital.client.codat.types.CodatAccountSubtype;
import com.clearspend.capital.client.codat.types.CodatAccountType;
import com.clearspend.capital.client.codat.types.CodatBankAccountStatusResponse;
import com.clearspend.capital.client.codat.types.CodatBankAccountsResponse;
import com.clearspend.capital.client.codat.types.CodatCreateBankAccountResponse;
import com.clearspend.capital.client.codat.types.CodatPushDataResponse;
import com.clearspend.capital.client.codat.types.CodatPushStatusResponse;
import com.clearspend.capital.client.codat.types.CodatSupplier;
import com.clearspend.capital.client.codat.types.CodatSupplierRequest;
import com.clearspend.capital.client.codat.types.CodatSyncDirectCostResponse;
import com.clearspend.capital.client.codat.types.CodatSyncReceiptRequest;
import com.clearspend.capital.client.codat.types.CodatSyncReceiptResponse;
import com.clearspend.capital.client.codat.types.CreateCompanyResponse;
import com.clearspend.capital.client.codat.types.CreateCreditCardRequest;
import com.clearspend.capital.client.codat.types.GetAccountsResponse;
import com.clearspend.capital.client.codat.types.GetSuppliersResponse;
import com.clearspend.capital.client.codat.types.SyncTransactionResponse;
import com.clearspend.capital.common.error.CodatApiCallException;
import com.clearspend.capital.common.typedid.data.AccountActivityId;
import com.clearspend.capital.common.typedid.data.ReceiptId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.data.model.AccountActivity;
import com.clearspend.capital.data.model.ChartOfAccountsMapping;
import com.clearspend.capital.data.model.Receipt;
import com.clearspend.capital.data.model.TransactionSyncLog;
import com.clearspend.capital.data.model.User;
import com.clearspend.capital.data.model.business.Business;
import com.clearspend.capital.data.model.enums.AccountActivityIntegrationSyncStatus;
import com.clearspend.capital.data.model.enums.AccountingSetupStep;
import com.clearspend.capital.data.model.enums.ChartOfAccountsUpdateStatus;
import com.clearspend.capital.data.model.enums.TransactionSyncStatus;
import com.clearspend.capital.data.repository.AccountActivityRepository;
import com.clearspend.capital.data.repository.ChartOfAccountsMappingRepository;
import com.clearspend.capital.data.repository.ReceiptRepository;
import com.clearspend.capital.data.repository.TransactionSyncLogRepository;
import com.clearspend.capital.data.repository.business.BusinessRepository;
import com.clearspend.capital.service.type.CurrentUser;
import com.google.common.annotations.VisibleForTesting;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class CodatService {

  private final CodatClient codatClient;

  private final AccountActivityService accountActivityService;
  private final AccountActivityRepository accountActivityRepository;
  private final BusinessRepository businessRepository;
  private final BusinessService businessService;
  private final ChartOfAccountsMappingRepository chartOfAccountsMappingRepository;
  private final ReceiptRepository receiptRepository;
  private final ReceiptImageService receiptImageService;
  private final TransactionSyncLogRepository transactionSyncLogRepository;
  private final UserService userService;

  private final ExpenseCategoryService expenseCategoryService;
  private final ChartOfAccountsMappingService chartOfAccountsMappingService;

  @PreAuthorize("hasRootPermission(#businessId, 'CROSS_BUSINESS_BOUNDARY|MANAGE_CONNECTIONS')")
  public String createQboConnectionForBusiness(TypedId<BusinessId> businessId)
      throws CodatApiCallException {
    Business business = businessService.retrieveBusinessForService(businessId, true);

    if (business.getCodatCompanyRef() == null) {
      CreateCompanyResponse response =
          codatClient.createCodatCompanyForBusiness(business.getLegalName());
      business =
          businessService.updateBusinessWithCodatCompanyRef(business.getId(), response.getId());
    }

    return codatClient.createQboConnectionForBusiness(business.getCodatCompanyRef()).getLinkUrl();
  }

  @PreAuthorize("hasRootPermission(#businessId, 'CROSS_BUSINESS_BOUNDARY|MANAGE_CONNECTIONS')")
  public Boolean getIntegrationConnectionStatus(TypedId<BusinessId> businessId) {
    Business business = businessService.retrieveBusinessForService(businessId, true);

    if (business.getCodatCompanyRef() == null) {
      return false;
    }

    return business.getCodatConnectionId() != null;
  }

  @PreAuthorize("hasGlobalPermission('APPLICATION')")
  public void updateBusinessStatusOnSync(String companyRef) {
    Optional<Business> business = businessRepository.findByCodatCompanyRef(companyRef);
    if (business.isPresent()) {
      businessService.updateBusinessAccountingStepFromSync(business.get().getBusinessId());
    }
  }

  @PreAuthorize("hasRootPermission(#businessId, 'CROSS_BUSINESS_BOUNDARY|MANAGE_CONNECTIONS')")
  public SyncTransactionResponse syncTransactionAsDirectCost(
      TypedId<AccountActivityId> accountActivityId, TypedId<BusinessId> businessId)
      throws CodatApiCallException {
    Business business = businessService.retrieveBusinessForService(businessId, true);

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
      Optional<CodatAccount> expenseAccount =
          accountsResponse.getResults().stream()
              .filter(account -> account.getId().equals(business.getCodatCreditCardId()))
              .findFirst();

      if (expenseAccount.isEmpty()) {
        return new SyncTransactionResponse("FAILED (No expense account)");
      }

      Optional<ChartOfAccountsMapping> expenseCategoryMapping =
          chartOfAccountsMappingRepository.findByBusinessIdAndExpenseCategoryId(
              businessId, accountActivity.getExpenseDetails().getExpenseCategoryId());

      if (expenseCategoryMapping.isEmpty()) {
        return new SyncTransactionResponse("FAILED (Expense category for transaction is unmapped");
      }

      CodatSyncDirectCostResponse directCostSyncResponse =
          codatClient.syncTransactionAsDirectCost(
              business.getCodatCompanyRef(),
              connectionId,
              accountActivity,
              business.getCurrency().name(),
              supplier,
              expenseAccount.get(),
              expenseCategoryMapping.get().getAccountRefId());

      User currentUserDetails = userService.retrieveUserForService(CurrentUser.getUserId());
      transactionSyncLogRepository.save(
          new TransactionSyncLog(
              business.getId(),
              accountActivityId,
              supplier.getId(), // TODO look back at this
              TransactionSyncStatus.IN_PROGRESS,
              directCostSyncResponse.getPushOperationKey(),
              business.getCodatCompanyRef(),
              currentUserDetails.getFirstName(),
              currentUserDetails.getLastName()));

      accountActivityService.updateAccountActivitySyncStatus(
          business.getId(), accountActivityId, AccountActivityIntegrationSyncStatus.SYNCED_LOCKED);

      return new SyncTransactionResponse("IN_PROGRESS", directCostSyncResponse);
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

      User currentUserDetails = userService.retrieveUserForService(CurrentUser.getUserId());

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

      accountActivityService.updateAccountActivitySyncStatus(
          business.getId(), accountActivityId, AccountActivityIntegrationSyncStatus.SYNCED_LOCKED);

      return new SyncTransactionResponse("WAITING_FOR_SUPPLIER");
    }
  }

  @PreAuthorize("hasRootPermission(#businessId, 'CROSS_BUSINESS_BOUNDARY|MANAGE_CONNECTIONS')")
  public CodatBankAccountsResponse getBankAccountsForBusiness(TypedId<BusinessId> businessId) {
    Business currentBusiness = businessService.retrieveBusinessForService(businessId, true);

    CodatBankAccountsResponse bankAccounts =
        codatClient.getBankAccountsForBusiness(
            currentBusiness.getCodatCompanyRef(), currentBusiness.getCodatConnectionId());

    return bankAccounts;
  }

  @PreAuthorize("hasRootPermission(#businessId, 'CROSS_BUSINESS_BOUNDARY|MANAGE_CONNECTIONS')")
  public CodatAccountNestedResponse getChartOfAccountsForBusiness(TypedId<BusinessId> businessId) {
    Business currentBusiness = businessService.retrieveBusinessForService(businessId, true);

    GetAccountsResponse chartOfAccounts =
        codatClient.getAccountsForBusiness(currentBusiness.getCodatCompanyRef());

    return new CodatAccountNestedResponse(nestCodatAccounts(chartOfAccounts.getResults()));
  }

  public CodatAccountNestedResponse getCodatChartOfAccountsForBusiness(
      TypedId<BusinessId> businessId,
      CodatAccountType type,
      List<CodatAccountSubtype> subCategories) {
    Business currentBusiness = businessService.retrieveBusinessForService(businessId, true);

    GetAccountsResponse chartOfAccounts =
        codatClient.getAccountsForBusiness(currentBusiness.getCodatCompanyRef());

    List<CodatAccount> response =
        chartOfAccounts.getResults().stream()
            .filter(
                account ->
                    account.getType().equals(type)
                        || (subCategories.stream()
                            .map(category -> category.getName())
                            .collect(toList())
                            .contains(account.getQualifiedName().split("\\.")[1])))
            .collect(toList());

    return new CodatAccountNestedResponse(nestCodatAccounts(response));
  }

  @PreAuthorize("hasRootPermission(#businessId, 'CROSS_BUSINESS_BOUNDARY|MANAGE_CONNECTIONS')")
  public CodatCreateBankAccountResponse createBankAccountForBusiness(
      TypedId<BusinessId> businessId, CreateCreditCardRequest createBankAccountRequest)
      throws CodatApiCallException {
    Business currentBusiness = businessService.retrieveBusinessForService(businessId, true);

    return codatClient.createBankAccountForBusiness(
        currentBusiness.getCodatCompanyRef(),
        currentBusiness.getCodatConnectionId(),
        createBankAccountRequest.getAccountName());
  }

  @PreAuthorize("hasRootPermission(#businessId, 'CROSS_BUSINESS_BOUNDARY|MANAGE_CONNECTIONS')")
  public Boolean deleteCodatIntegrationConnection(TypedId<BusinessId> businessId)
      throws CodatApiCallException {
    Business currentBusiness = businessService.retrieveBusinessForService(businessId, true);

    Boolean deleteResult =
        codatClient.deleteCodatIntegrationConnectionForBusiness(
            currentBusiness.getCodatCompanyRef(), currentBusiness.getCodatConnectionId());

    if (deleteResult) {
      businessService.updateBusinessAccountingSetupStep(
          businessId, AccountingSetupStep.AWAITING_SYNC);
      businessService.deleteCodatConnectionForBusiness(businessId);
      chartOfAccountsMappingService.deleteChartOfAccountsMappingsForBusiness(businessId);
      // first enable all default categories and then disable the qbo/non default categories
      expenseCategoryService.enableDefaultExpenseCategories(businessId);
      expenseCategoryService.disableQboExpenseCategories(businessId);
    }

    return deleteResult;
  }

  // TODO: Nesting off of fully qualified name swap to codat method when changes are implements on
  // their end.
  @VisibleForTesting
  public List<CodatAccountNested> nestCodatAccounts(List<CodatAccount> accounts) {
    Tree<String, CodatAccount> tree = new Tree<String, CodatAccount>("root");
    Tree<String, CodatAccount> current = tree;

    for (CodatAccount account : accounts) {
      Tree<String, CodatAccount> walker = current;
      String[] baseElements = account.getQualifiedName().split("\\.");
      for (int i = 3; i < baseElements.length; i++) {
        current = current.child(baseElements[i]);
      }
      current.setPayload(account);
      current = walker;
    }

    return buildNestedListFromTree(tree, null);
  }

  private List<CodatAccountNested> buildNestedListFromTree(
      Tree<String, CodatAccount> tree, CodatAccountNested parent) {
    List<CodatAccountNested> result = new ArrayList<>();
    if (tree.getPayload() != null) {
      CodatAccountNested me = createNestedAccountFromAccount(tree.getPayload());
      for (Tree<String, CodatAccount> child : tree.getChildren()) {
        me.getChildren().addAll(buildNestedListFromTree(child, me));
      }
      result.add(me);
    } else {
      List<CodatAccountNested> descendents = new ArrayList<>();
      for (Tree<String, CodatAccount> child : tree.getChildren()) {
        descendents.addAll(buildNestedListFromTree(child, null));
      }
      result.addAll(descendents);
    }
    return result;
  }

  private CodatAccountNested createNestedAccountFromAccount(CodatAccount account) {
    CodatAccountNested newAccount = new CodatAccountNested(account.getId(), account.getName());
    newAccount.setUpdateStatus(ChartOfAccountsUpdateStatus.NOT_CHANGED);
    newAccount.setStatus(account.getStatus().getName());
    newAccount.setCategory(account.getCategory());
    newAccount.setQualifiedName(account.getQualifiedName());
    newAccount.setType(account.getType().getName());
    return newAccount;
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
    Business business = businessService.retrieveBusinessForService(businessId, true);

    GetSuppliersResponse suppliersResponse =
        codatClient.getSuppliersForBusiness(business.getCodatCompanyRef());

    AccountActivity accountActivity =
        accountActivityService.retrieveAccountActivityForService(
            businessId, transaction.getAccountActivityId());

    CodatSupplier supplier =
        supplierForTransaction(accountActivity, suppliersResponse.getResults());

    if (supplier != null) {
      GetAccountsResponse accountsResponse =
          codatClient.getAccountsForBusiness(business.getCodatCompanyRef());
      Optional<CodatAccount> expenseAccount =
          accountsResponse.getResults().stream()
              .filter(account -> account.getId().equals(business.getCodatCreditCardId()))
              .findFirst();

      Optional<ChartOfAccountsMapping> expenseCategoryMapping =
          chartOfAccountsMappingRepository.findByBusinessIdAndExpenseCategoryId(
              businessId, accountActivity.getExpenseDetails().getExpenseCategoryId());

      if (expenseAccount.isPresent()) {
        CodatSyncDirectCostResponse directCostSyncResponse =
            codatClient.syncTransactionAsDirectCost(
                business.getCodatCompanyRef(),
                business.getCodatConnectionId(),
                accountActivity,
                business.getCurrency().name(),
                supplier,
                expenseAccount.get(),
                expenseCategoryMapping.get().getAccountRefId());

        Optional<TransactionSyncLog> transactionSyncLogOptional =
            transactionSyncLogRepository.findById(transaction.getId());

        if (transactionSyncLogOptional.isEmpty()) {
          return;
        }

        TransactionSyncLog transactionSyncLog = transactionSyncLogOptional.get();

        transactionSyncLog.setStatus(TransactionSyncStatus.IN_PROGRESS);
        transactionSyncLog.setDirectCostPushOperationKey(
            directCostSyncResponse.getPushOperationKey());
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

  private void updateSyncStatusIfComplete(TransactionSyncLog transaction) {
    Business business =
        businessService.retrieveBusinessForService(transaction.getBusinessId(), true);
    CodatPushStatusResponse status =
        codatClient.getPushStatus(
            transaction.getDirectCostPushOperationKey(), business.getCodatCompanyRef());

    Optional<TransactionSyncLog> transactionSyncLogOptional =
        transactionSyncLogRepository.findById(transaction.getId());

    if (transactionSyncLogOptional.isEmpty()) {
      return;
    }

    TransactionSyncLog transactionSyncLog = transactionSyncLogOptional.get();
    try {
      if (status.getStatus().equals("Success")) {
        transactionSyncLog.setStatus(TransactionSyncStatus.COMPLETED);
      } else if (status.getStatus().equals("Failed")) {
        transactionSyncLog.setStatus(TransactionSyncStatus.FAILED);
      }
      Optional<AccountActivity> accountActivityForSyncOptional =
          accountActivityRepository.findByBusinessIdAndId(
              business.getId(), transactionSyncLog.getAccountActivityId());

      accountActivityForSyncOptional.ifPresent(
          accountActivity -> {
            accountActivity.setLastSyncTime(OffsetDateTime.now());
            accountActivityRepository.save(accountActivity);

            if (accountActivity.getReceipt() != null
                && accountActivity.getReceipt().getReceiptIds() != null
                && !accountActivity.getReceipt().getReceiptIds().isEmpty()) {
              syncReceiptsForAccountActivity(accountActivity, status.getDataId().getId());
              transactionSyncLog.setStatus(TransactionSyncStatus.UPLOADED_RECEIPTS);
            }
          });

    } finally {
      if (transactionSyncLog != null) {
        transactionSyncLogRepository.save(transactionSyncLog);
      }
    }
  }

  private void syncReceiptsForAccountActivity(AccountActivity accountActivity, String externalId) {
    Business business =
        businessService.retrieveBusinessForService(accountActivity.getBusinessId(), true);
    if (accountActivity.getReceipt() != null
        && accountActivity.getReceipt().getReceiptIds() != null
        && !accountActivity.getReceipt().getReceiptIds().isEmpty()
        && StringUtils.hasText(externalId)) {
      for (TypedId<ReceiptId> id : accountActivity.getReceipt().getReceiptIds()) {
        Receipt receipt = receiptRepository.findById(id).orElseThrow();
        syncIndividualReceipt(receipt, business, externalId);
      }
    }
  }

  public void syncIndividualReceipt(Receipt receipt, AccountActivity accountActivity) {
    Optional<TransactionSyncLog> transactionLog =
        transactionSyncLogRepository.findFirstByAccountActivityIdSortByUpdated(
            accountActivity.getId());

    if (transactionLog.isPresent()) {
      TransactionSyncLog transaction = transactionLog.get();
      Business business =
          businessService.retrieveBusinessForService(accountActivity.getBusinessId(), true);
      CodatPushStatusResponse status =
          codatClient.getPushStatus(
              transaction.getDirectCostPushOperationKey(), transaction.getCodatCompanyRef());
      syncIndividualReceipt(receipt, business, status.getDataId().getId());

      transaction.setStatus(TransactionSyncStatus.UPLOADED_RECEIPTS);
      transactionSyncLogRepository.save(transaction);
    }
  }

  private CodatSyncReceiptResponse syncIndividualReceipt(
      Receipt receipt, Business business, String externalId) {
    return codatClient.syncReceiptsForDirectCost(
        new CodatSyncReceiptRequest(
            business.getCodatCompanyRef(),
            business.getCodatConnectionId(),
            externalId,
            receiptImageService.getReceiptImage(receipt.getPath()),
            receipt.getContentType(),
            receipt.getId()));
  }

  @PreAuthorize("hasGlobalPermission('APPLICATION')")
  public void syncTransactionAwaitingSupplier(String companyId, String pushOperationKey) {
    Optional<TransactionSyncLog> syncForKey =
        transactionSyncLogRepository.findByDirectCostPushOperationKey(pushOperationKey);

    if (syncForKey.isEmpty()) {
      return;
    }

    syncTransactionIfSupplierExists(syncForKey.get(), syncForKey.get().getBusinessId());
  }

  @PreAuthorize("hasGlobalPermission('APPLICATION')")
  public void updateStatusForSyncedTransaction(String companyId, String pushOperationKey) {
    Optional<TransactionSyncLog> syncForKey =
        transactionSyncLogRepository.findByDirectCostPushOperationKey(pushOperationKey);

    if (syncForKey.isEmpty()) {
      return;
    }

    updateSyncStatusIfComplete(syncForKey.get());
  }

  @PreAuthorize("hasGlobalPermission('APPLICATION')")
  public void updateConnectionIdForBusiness(String codatCompanyRef, String dataConnectionId) {
    businessRepository
        .findByCodatCompanyRef(codatCompanyRef)
        .ifPresent(
            business ->
                businessService.updateBusinessWithCodatConnectionId(
                    business.getId(), dataConnectionId));
  }

  @PreAuthorize("hasGlobalPermission('APPLICATION')")
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
      codatClient.syncDataTypeForCompany(codatCompanyRef, "chartOfAccounts");
    }
  }

  public List<SyncTransactionResponse> syncMultipleTransactions(
      List<TypedId<AccountActivityId>> accountActivityIds, TypedId<BusinessId> businessId) {
    return accountActivityIds.stream()
        .map(accountActivityId -> syncTransactionAsDirectCost(accountActivityId, businessId))
        .collect(Collectors.toUnmodifiableList());
  }

  public List<SyncTransactionResponse> syncAllReadyTransactions(TypedId<BusinessId> businessId) {
    List<AccountActivity> transactionsReadyToSync =
        accountActivityService.findAllSyncableForBusiness(businessId);
    return syncMultipleTransactions(
        transactionsReadyToSync.stream().map(transaction -> transaction.getId()).collect(toList()),
        businessId);
  }

  public Integer getSyncReadyCount(TypedId<BusinessId> businessId) {
    return accountActivityRepository.countByIntegrationSyncStatusAndBusinessId(
        AccountActivityIntegrationSyncStatus.READY, businessId);
  }

  private static final class Tree<T, R> {
    private final Set<Tree<T, R>> children = new LinkedHashSet<>();
    private final T pathSegment;
    private R payload;

    public Tree(T data) {
      this.pathSegment = data;
    }

    Tree<T, R> child(T data) {
      for (Tree<T, R> child : children) {
        if (child.pathSegment.equals(data)) {
          return child;
        }
      }
      return child(new Tree<>(data));
    }

    Tree<T, R> child(Tree<T, R> child) {
      children.add(child);
      return child;
    }

    void setPayload(R payload) {
      this.payload = payload;
    }

    R getPayload() {
      return payload;
    }

    Set<Tree<T, R>> getChildren() {
      return children;
    }
  }
}
