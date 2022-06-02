package com.clearspend.capital.service;

import static java.util.stream.Collectors.toList;

import com.clearspend.capital.client.codat.CodatClient;
import com.clearspend.capital.client.codat.types.CodatAccount;
import com.clearspend.capital.client.codat.types.CodatAccountNested;
import com.clearspend.capital.client.codat.types.CodatAccountNestedResponse;
import com.clearspend.capital.client.codat.types.CodatAccountSubtype;
import com.clearspend.capital.client.codat.types.CodatBankAccount;
import com.clearspend.capital.client.codat.types.CodatBankAccountStatusResponse;
import com.clearspend.capital.client.codat.types.CodatBankAccountsResponse;
import com.clearspend.capital.client.codat.types.CodatCreateBankAccountResponse;
import com.clearspend.capital.client.codat.types.CodatError;
import com.clearspend.capital.client.codat.types.CodatProperties;
import com.clearspend.capital.client.codat.types.CodatPushDataResponse;
import com.clearspend.capital.client.codat.types.CodatPushStatusResponse;
import com.clearspend.capital.client.codat.types.CodatSupplier;
import com.clearspend.capital.client.codat.types.CodatSupplierRequest;
import com.clearspend.capital.client.codat.types.CodatSyncDirectCostResponse;
import com.clearspend.capital.client.codat.types.CodatSyncReceiptRequest;
import com.clearspend.capital.client.codat.types.CodatSyncReceiptResponse;
import com.clearspend.capital.client.codat.types.CodatSyncResponse;
import com.clearspend.capital.client.codat.types.CodatTrackingCategory;
import com.clearspend.capital.client.codat.types.CodatTrackingCategoryRef;
import com.clearspend.capital.client.codat.types.CodatValidation;
import com.clearspend.capital.client.codat.types.CreateAssignSupplierResponse;
import com.clearspend.capital.client.codat.types.CreateCompanyResponse;
import com.clearspend.capital.client.codat.types.CreateCreditCardRequest;
import com.clearspend.capital.client.codat.types.GetAccountsResponse;
import com.clearspend.capital.client.codat.types.GetSuppliersResponse;
import com.clearspend.capital.client.codat.types.GetTrackingCategoriesResponse;
import com.clearspend.capital.client.codat.types.SetCategoryNamesRequest;
import com.clearspend.capital.client.codat.types.SyncTransactionResponse;
import com.clearspend.capital.common.audit.AccountingAuditEventPublisher;
import com.clearspend.capital.common.audit.CodatSyncEventType;
import com.clearspend.capital.common.data.model.TypedMutable;
import com.clearspend.capital.common.error.CodatApiCallException;
import com.clearspend.capital.common.typedid.data.AccountActivityId;
import com.clearspend.capital.common.typedid.data.ReceiptId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.data.model.AccountActivity;
import com.clearspend.capital.data.model.ChartOfAccountsMapping;
import com.clearspend.capital.data.model.CodatCategory;
import com.clearspend.capital.data.model.Receipt;
import com.clearspend.capital.data.model.TransactionSyncLog;
import com.clearspend.capital.data.model.User;
import com.clearspend.capital.data.model.business.Business;
import com.clearspend.capital.data.model.enums.AccountActivityIntegrationSyncStatus;
import com.clearspend.capital.data.model.enums.AccountingSetupStep;
import com.clearspend.capital.data.model.enums.ChartOfAccountsUpdateStatus;
import com.clearspend.capital.data.model.enums.CodatCategoryType;
import com.clearspend.capital.data.model.enums.TransactionSyncStatus;
import com.clearspend.capital.data.repository.AccountActivityRepository;
import com.clearspend.capital.data.repository.ChartOfAccountsMappingRepository;
import com.clearspend.capital.data.repository.CodatCategoryRepository;
import com.clearspend.capital.data.repository.ReceiptRepository;
import com.clearspend.capital.data.repository.TransactionSyncLogRepository;
import com.clearspend.capital.data.repository.business.BusinessRepository;
import com.clearspend.capital.service.type.CurrentUser;
import com.google.cloud.Tuple;
import com.google.common.base.Splitter;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.text.similarity.FuzzyScore;
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

  private final CodatCategoryRepository codatCategoryRepository;

  private final CodatProperties codatProperties;

  private final AccountingAuditEventPublisher accountingEventPublisher;

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
    if (business.isPresent()
        && business.get().getAccountingSetupStep().equals(AccountingSetupStep.AWAITING_SYNC)) {
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

    String connectionId = business.getCodatConnectionId();

    AccountActivity accountActivity =
        accountActivityService.retrieveAccountActivity(
            CurrentUser.getActiveBusinessId(), accountActivityId);

    if (accountActivity.getMerchant().getCodatSupplierId() == null) {
      return null;
    }

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

    List<CodatTrackingCategoryRef> trackingCategoryRefs = new ArrayList<>();

    if (accountActivity.getAccountingDetails() != null) {
      if (accountActivity.getAccountingDetails().getCodatClassId() != null) {
        Optional<CodatCategory> classCategory =
            codatCategoryRepository.findById(
                accountActivity.getAccountingDetails().getCodatClassId());
        if (classCategory.isPresent()) {
          trackingCategoryRefs.add(
              new CodatTrackingCategoryRef(classCategory.get().getCodatCategoryId()));
        }
      }
      if (accountActivity.getAccountingDetails().getCodatLocationId() != null) {
        Optional<CodatCategory> locationCategory =
            codatCategoryRepository.findById(
                accountActivity.getAccountingDetails().getCodatLocationId());
        if (locationCategory.isPresent()) {
          trackingCategoryRefs.add(
              new CodatTrackingCategoryRef(locationCategory.get().getCodatCategoryId()));
        }
      }
    }

    CodatSyncDirectCostResponse directCostSyncResponse =
        codatClient.syncTransactionAsDirectCost(
            business.getCodatCompanyRef(),
            connectionId,
            accountActivity,
            business.getCurrency().name(),
            expenseAccount.get(),
            expenseCategoryMapping.get().getAccountRefId(),
            trackingCategoryRefs);

    User currentUserDetails = userService.retrieveUserForService(CurrentUser.getUserId());
    transactionSyncLogRepository.save(
        new TransactionSyncLog(
            business.getId(),
            accountActivityId,
            accountActivity.getMerchant().getCodatSupplierId(),
            TransactionSyncStatus.IN_PROGRESS,
            directCostSyncResponse.getPushOperationKey(),
            business.getCodatCompanyRef(),
            currentUserDetails.getFirstName(),
            currentUserDetails.getLastName()));

    accountActivityService.updateAccountActivitySyncStatus(
        business.getId(), accountActivityId, AccountActivityIntegrationSyncStatus.SYNCED_LOCKED);

    Map<String, String> codatActivity = new HashMap<>();
    codatActivity.put(CodatSyncEventType.DIRECT_COST_SYNC.toString(), accountActivityId.toString());

    accountingEventPublisher.publishAccountingCodatSyncAuditEvent(
        codatActivity, businessId.toString(), currentUserDetails.getId().toString());

    return new SyncTransactionResponse("IN_PROGRESS", directCostSyncResponse);
  }

  @PreAuthorize("hasRootPermission(#businessId, 'CROSS_BUSINESS_BOUNDARY|MANAGE_CONNECTIONS')")
  public CodatAccountNestedResponse getChartOfAccountsForBusiness(TypedId<BusinessId> businessId) {
    Business currentBusiness = businessService.retrieveBusinessForService(businessId, true);

    GetAccountsResponse chartOfAccounts =
        codatClient.getAccountsForBusiness(currentBusiness.getCodatCompanyRef());

    return new CodatAccountNestedResponse(nestCodatAccounts(chartOfAccounts.getResults()));
  }

  @PreAuthorize("hasRootPermission(#businessId, 'MANAGE_CONNECTIONS|READ|APPLICATION')")
  public CodatAccountNestedResponse getCodatChartOfAccountsForBusiness(
      TypedId<BusinessId> businessId, List<CodatAccountSubtype> subCategories) {
    Business currentBusiness = businessService.retrieveBusinessForService(businessId, true);

    GetAccountsResponse chartOfAccounts =
        codatClient.getAccountsForBusiness(currentBusiness.getCodatCompanyRef());

    List<CodatAccount> response =
        chartOfAccounts.getResults().stream()
            .filter(
                account ->
                    subCategories.stream()
                        .map(category -> category.getName())
                        .toList()
                        .contains(
                            Splitter.on(".")
                                .limit(3)
                                .splitToList(account.getQualifiedName())
                                .get(1)))
            .collect(toList());

    return new CodatAccountNestedResponse(nestCodatAccounts(response));
  }

  @PreAuthorize("hasRootPermission(#businessId, 'CROSS_BUSINESS_BOUNDARY|MANAGE_CONNECTIONS')")
  public CodatBankAccountsResponse getBankAccountsForBusiness(TypedId<BusinessId> businessId) {
    Business currentBusiness = businessService.retrieveBusinessForService(businessId, true);

    return codatClient.getBankAccountsForBusiness(
        currentBusiness.getCodatCompanyRef(), currentBusiness.getCodatConnectionId());
  }

  @PreAuthorize("hasRootPermission(#businessId, 'CROSS_BUSINESS_BOUNDARY|MANAGE_CONNECTIONS')")
  public CodatCreateBankAccountResponse createBankAccountForBusiness(
      TypedId<BusinessId> businessId, CreateCreditCardRequest createBankAccountRequest)
      throws CodatApiCallException {
    Business currentBusiness = businessService.retrieveBusinessForService(businessId, true);

    if (codatClient
            .getBankAccountForBusinessByAccountName(
                currentBusiness.getCodatCompanyRef(),
                currentBusiness.getCodatConnectionId(),
                createBankAccountRequest.getAccountName())
            .getResults()
            .size()
        > 0) {
      // A Bank Account with this Account Name already exists.
      log.info(
          "User attempted to create a QBO Bank Account with an Account Name matching an existing Account: {}",
          createBankAccountRequest.getAccountName());
      return new CodatCreateBankAccountResponse(
          new CodatValidation(
              List.of(new CodatError("0", "Account with that name already exists"))),
          "",
          "Failure");
    }

    return codatClient.createBankAccountForBusiness(
        currentBusiness.getCodatCompanyRef(),
        currentBusiness.getCodatConnectionId(),
        createBankAccountRequest.getAccountName());
  }

  @PreAuthorize("hasRootPermission(#businessId, 'MANAGE_CONNECTIONS|READ|APPLICATION')")
  public Business updateCodatCreditCardForBusiness(
      TypedId<BusinessId> businessId, String codatCreditCardId) {
    Business business = businessService.retrieveBusinessForService(businessId, true);

    try {
      CodatBankAccount account =
          codatClient.getBankAccountById(
              business.getCodatCompanyRef(), business.getCodatConnectionId(), codatCreditCardId);
    } catch (Exception e) {
      log.info(
          "User attempted to link to a QBO Bank Account that does not exist: {}",
          codatCreditCardId);
      return business;
    }

    business.setCodatCreditCardId(codatCreditCardId);

    return businessRepository.save(business);
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
      businessService.setAutomaticExpenseCategories(businessId, false);
      chartOfAccountsMappingService.deleteChartOfAccountsMappingsForBusiness(businessId);
      // first enable all default categories and then disable the qbo/non default categories
      expenseCategoryService.enableDefaultExpenseCategories(businessId);
      expenseCategoryService.disableQboExpenseCategories(businessId);
    }

    return deleteResult;
  }

  // TODO: Nesting off of fully qualified name swap to codat method when changes are implements on
  // their end.
  List<CodatAccountNested> nestCodatAccounts(List<CodatAccount> accounts) {
    Tree<String, CodatAccount> tree = new Tree<>("root");
    Tree<String, CodatAccount> current = tree;

    for (CodatAccount account : accounts) {
      Tree<String, CodatAccount> walker = current;
      List<String> baseElements = Splitter.on('.').splitToList(account.getQualifiedName());
      for (int i = 3; i < baseElements.size(); i++) {
        current = current.child(baseElements.get(i));
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
    if (account.getQualifiedName().endsWith("(deleted)")) {
      newAccount.setUpdateStatus(ChartOfAccountsUpdateStatus.DELETED);
    } else {
      newAccount.setUpdateStatus(ChartOfAccountsUpdateStatus.NOT_CHANGED);
    }
    newAccount.setStatus(account.getStatus().getName());
    newAccount.setCategory(account.getCategory());
    newAccount.setQualifiedName(account.getQualifiedName());
    newAccount.setType(account.getType().getName());
    return newAccount;
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
            accountActivity.setLastSyncTime(OffsetDateTime.now(ZoneOffset.UTC));
            accountActivityRepository.save(accountActivity);

            if (accountActivity.getReceipt() != null
                && accountActivity.getReceipt().getReceiptIds() != null
                && !accountActivity.getReceipt().getReceiptIds().isEmpty()) {
              syncReceiptsForAccountActivity(accountActivity, status.getData().getId());
              transactionSyncLog.setStatus(TransactionSyncStatus.UPLOADED_RECEIPTS);
            }
          });

    } finally {
      transactionSyncLogRepository.save(transactionSyncLog);
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

  void syncIndividualReceipt(Receipt receipt, AccountActivity accountActivity) {
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
      syncIndividualReceipt(receipt, business, status.getData().getId());

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
                  updateCodatCreditCardForBusiness(
                      business.getId(), accountStatus.getData().getId()));
      codatClient.syncDataTypeForCompany(codatCompanyRef, "chartOfAccounts");
    }
  }

  @PreAuthorize("hasRootPermission(#businessId, 'CROSS_BUSINESS_BOUNDARY|MANAGE_CONNECTIONS')")
  public CodatSyncResponse updateChartOfAccountsForBusiness(TypedId<BusinessId> businessId) {
    Business business = businessService.getBusiness(businessId, true);
    if (business.getCodatCompanyRef() != null) {
      return codatClient.syncDataTypeForCompany(business.getCodatCompanyRef(), "chartOfAccounts");
    }
    return null;
  }

  @PreAuthorize("hasRootPermission(#businessId, 'MANAGE_CONNECTIONS|READ|APPLICATION')")
  public List<SyncTransactionResponse> syncMultipleTransactions(
      List<TypedId<AccountActivityId>> accountActivityIds, TypedId<BusinessId> businessId) {
    return accountActivityIds.stream()
        .map(accountActivityId -> syncTransactionAsDirectCost(accountActivityId, businessId))
        .collect(Collectors.toUnmodifiableList());
  }

  @PreAuthorize("hasRootPermission(#businessId, 'MANAGE_CONNECTIONS|READ|APPLICATION')")
  public List<SyncTransactionResponse> syncAllReadyTransactions(TypedId<BusinessId> businessId) {
    List<AccountActivity> transactionsReadyToSync =
        accountActivityService.findAllSyncableForBusiness(businessId);
    return syncMultipleTransactions(
        transactionsReadyToSync.stream().map(TypedMutable::getId).collect(toList()), businessId);
  }

  @PreAuthorize("hasRootPermission(#businessId, 'MANAGE_CONNECTIONS|READ|APPLICATION')")
  public Integer getSyncReadyCount(TypedId<BusinessId> businessId) {
    return accountActivityRepository.countByIntegrationSyncStatusAndBusinessId(
        AccountActivityIntegrationSyncStatus.READY, businessId);
  }

  @PreAuthorize("hasRootPermission(#businessId, 'MANAGE_CONNECTIONS|READ|APPLICATION')")
  public GetSuppliersResponse getAllSuppliersFromQboByBusiness(
      TypedId<BusinessId> businessId, Integer limit) {
    Business business = businessService.retrieveBusinessForService(businessId, true);
    GetSuppliersResponse suppliersFromQbo =
        codatClient.getSuppliersForBusiness(business.getCodatCompanyRef());
    // 1. return empty list if nothing from QBO.
    if (suppliersFromQbo == null || CollectionUtils.isEmpty(suppliersFromQbo.getResults())) {
      GetSuppliersResponse emptyResponse = new GetSuppliersResponse(0, Collections.emptyList());
      return emptyResponse;
    }

    List<CodatSupplier> finalResult = null;
    // 2. if limit null or is greater than the return from qbo, return all
    if (limit == null || suppliersFromQbo.getResults().size() <= limit) {
      finalResult = suppliersFromQbo.getResults();
    } else {
      // 3. return first limit records
      finalResult =
          suppliersFromQbo.getResults().stream()
              .limit(Long.valueOf(limit))
              .collect(Collectors.toList());
    }
    return new GetSuppliersResponse(finalResult.size(), finalResult);
  }

  @PreAuthorize("hasRootPermission(#businessId, 'MANAGE_CONNECTIONS|READ|APPLICATION')")
  public GetSuppliersResponse getMatchedSuppliersFromQboByBusiness(
      TypedId<BusinessId> businessId, Integer limit, String targetName) {
    Business business = businessService.retrieveBusinessForService(businessId, true);
    GetSuppliersResponse suppliersFromQbo =
        codatClient.getSuppliersForBusiness(business.getCodatCompanyRef());

    if (suppliersFromQbo == null || CollectionUtils.isEmpty(suppliersFromQbo.getResults())) {
      GetSuppliersResponse emptyResponse = new GetSuppliersResponse(0, Collections.emptyList());
      return emptyResponse;
    }

    Double threshold = codatProperties.getSupplierMatchingRatio();
    FuzzyScore scoreAlgorithm = new FuzzyScore(Locale.ENGLISH);
    return GetSuppliersResponse.fromSupplierList(
        suppliersFromQbo.getResults().stream()
            .map(it -> Tuple.of(it, scoreAlgorithm.fuzzyScore(it.getSupplierName(), targetName)))
            .filter(it -> (it.y().doubleValue() / targetName.length()) > threshold)
            .sorted((lhs, rhs) -> rhs.y().compareTo(lhs.y()))
            .limit(limit)
            .map(it -> it.x())
            .collect(toList()));
  }

  @PreAuthorize("hasRootPermission(#businessId, 'MANAGE_CONNECTIONS|READ|APPLICATION')")
  public CreateAssignSupplierResponse createVendorAssignedToAccountActivity(
      TypedId<BusinessId> businessId,
      TypedId<AccountActivityId> accountActivityId,
      String supplierName) {
    Business business = businessService.retrieveBusinessForService(businessId, true);
    CodatPushDataResponse response =
        codatClient.syncSupplierToCodat(
            business.getCodatCompanyRef(),
            business.getCodatConnectionId(),
            new CodatSupplierRequest(supplierName, "Active", business.getCurrency().toString()));
    User currentUserDetails = userService.retrieveUserForService(CurrentUser.getUserId());
    transactionSyncLogRepository.save(
        new TransactionSyncLog(
            businessId,
            accountActivityId,
            "",
            TransactionSyncStatus.AWAITING_SUPPLIER,
            response.getPushOperationKey(),
            business.getCodatCompanyRef(),
            currentUserDetails.getFirstName(),
            currentUserDetails.getLastName()));
    // Emit Audit Event
    Map<String, String> codatActivity = new HashMap<>();
    codatActivity.put(CodatSyncEventType.SUPPLIER_SYNC_TO_CODAT.toString(), supplierName);
    accountingEventPublisher.publishAccountingCodatSyncAuditEvent(
        codatActivity, businessId.toString(), currentUserDetails.getId().toString());

    return new CreateAssignSupplierResponse(accountActivityId);
  }

  @PreAuthorize("hasGlobalPermission('APPLICATION')")
  public void updateSupplierForWaitingActivity(String companyId, String pushOperationKey) {
    Optional<TransactionSyncLog> syncForKey =
        transactionSyncLogRepository.findByDirectCostPushOperationKey(pushOperationKey);

    if (syncForKey.isEmpty()) {
      return;
    }

    Optional<AccountActivity> loggedActivity =
        accountActivityRepository.findById(syncForKey.get().getAccountActivityId());
    if (loggedActivity.isPresent()) {
      CodatPushStatusResponse pushStatus = codatClient.getPushStatus(pushOperationKey, companyId);

      AccountActivity updatedActivity = loggedActivity.get();
      updatedActivity.getMerchant().setCodatSupplierName(pushStatus.getData().getSupplierName());
      updatedActivity.getMerchant().setCodatSupplierId(pushStatus.getData().getId());

      accountActivityRepository.save(updatedActivity);
    }
  }

  @PreAuthorize("hasGlobalPermission('APPLICATION')")
  public void updateCodatCategories(String companyRef) {
    Optional<Business> business = businessRepository.findByCodatCompanyRef(companyRef);
    if (business.isPresent()) {
      GetTrackingCategoriesResponse categories =
          codatClient.getTrackingCategoriesForBusiness(business.get().getCodatCompanyRef());
      for (CodatTrackingCategory category : categories.getResults()) {
        if (codatCategoryRepository
            .findByBusinessIdAndCodatCategoryId(business.get().getBusinessId(), category.getId())
            .isEmpty()) {
          if ("DEPARTMENTS".equals(category.getParentId())) {
            codatCategoryRepository.save(
                new CodatCategory(
                    business.get().getId(),
                    category.getId(),
                    category.getName(),
                    category.getName(),
                    CodatCategoryType.LOCATION));
          }
          if ("CLASSES".equals(category.getParentId())) {
            codatCategoryRepository.save(
                new CodatCategory(
                    business.get().getId(),
                    category.getId(),
                    category.getName(),
                    category.getName(),
                    CodatCategoryType.CLASS));
          }
        }
      }
    }
  }

  @PreAuthorize("hasRootPermission(#businessId, 'MANAGE_CONNECTIONS|READ|APPLICATION')")
  public Boolean setClearspendNamesForCategories(
      TypedId<BusinessId> businessId, List<SetCategoryNamesRequest> nameUpdateRequests) {
    nameUpdateRequests.forEach(
        update -> {
          Optional<CodatCategory> category =
              codatCategoryRepository.findById(update.getCategoryId());
          category.ifPresent(
              codatCategory -> {
                codatCategory.setCategoryName(update.getName());
                codatCategoryRepository.save(codatCategory);
              });
        });
    return true;
  }

  @PreAuthorize("hasRootPermission(#businessId, 'MANAGE_CONNECTIONS|READ|APPLICATION')")
  public List<CodatCategory> getCodatCategoriesByType(
      TypedId<BusinessId> businessId, CodatCategoryType type) {
    return codatCategoryRepository.findByBusinessIdAndType(businessId, type);
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
