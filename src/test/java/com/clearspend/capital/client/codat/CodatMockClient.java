package com.clearspend.capital.client.codat;

import com.clearspend.capital.client.codat.types.CodatAccount;
import com.clearspend.capital.client.codat.types.CodatAccountStatus;
import com.clearspend.capital.client.codat.types.CodatAccountType;
import com.clearspend.capital.client.codat.types.CodatBankAccount;
import com.clearspend.capital.client.codat.types.CodatBankAccountStatusResponse;
import com.clearspend.capital.client.codat.types.CodatBankAccountsResponse;
import com.clearspend.capital.client.codat.types.CodatCreateBankAccountResponse;
import com.clearspend.capital.client.codat.types.CodatPushData;
import com.clearspend.capital.client.codat.types.CodatPushDataResponse;
import com.clearspend.capital.client.codat.types.CodatPushStatusResponse;
import com.clearspend.capital.client.codat.types.CodatSupplier;
import com.clearspend.capital.client.codat.types.CodatSupplierRequest;
import com.clearspend.capital.client.codat.types.CodatSyncDirectCostResponse;
import com.clearspend.capital.client.codat.types.CodatSyncReceiptRequest;
import com.clearspend.capital.client.codat.types.CodatSyncReceiptResponse;
import com.clearspend.capital.client.codat.types.CodatSyncResponse;
import com.clearspend.capital.client.codat.types.CodatValidation;
import com.clearspend.capital.client.codat.types.ConnectionStatus;
import com.clearspend.capital.client.codat.types.ConnectionStatusResponse;
import com.clearspend.capital.client.codat.types.CreateCompanyResponse;
import com.clearspend.capital.client.codat.types.CreateIntegrationResponse;
import com.clearspend.capital.client.codat.types.DirectCostRequest;
import com.clearspend.capital.client.codat.types.GetAccountsResponse;
import com.clearspend.capital.client.codat.types.GetSuppliersResponse;
import com.clearspend.capital.common.error.CodatApiCallException;
import com.clearspend.capital.data.model.AccountActivity;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Profile("test")
@Component
@Slf4j
public class CodatMockClient extends CodatClient {
  private List<CodatSupplier> supplierList;
  private List<CodatAccount> accountList;

  public CodatMockClient(WebClient codatWebClient, ObjectMapper objectMapper) {
    super(codatWebClient, objectMapper, "quickbooksonlinesandbox");
    supplierList = new ArrayList<>();
    supplierList.add(new CodatSupplier("1", "Test Business", "ACTIVE", "USD"));
    createDefaultAccountList();
  }

  public void overrideDefaultAccountList(List<CodatAccount> accountList) {
    this.accountList = accountList;
  }

  public void createDefaultAccountList() {
    accountList = new ArrayList();
    accountList.add(
        new CodatAccount(
            "codat-card-id",
            "checking",
            CodatAccountStatus.ACTIVE,
            "category",
            "Asset.accounts.something.full_name",
            CodatAccountType.ASSET));
    accountList.add(
        new CodatAccount(
            "banking",
            "checking_account",
            CodatAccountStatus.ACTIVE,
            "category",
            "Asset.accounts.something.full_name",
            CodatAccountType.ASSET));
    accountList.add(
        new CodatAccount(
            "auto",
            "automobile",
            CodatAccountStatus.ACTIVE,
            "category",
            "Expense.Expense.auto.something.auto",
            CodatAccountType.EXPENSE));
    accountList.add(
        new CodatAccount(
            "fuel",
            "fuel",
            CodatAccountStatus.ACTIVE,
            "category",
            "Expense.Expense.auto.something.fuel",
            CodatAccountType.EXPENSE));
    accountList.add(
        new CodatAccount(
            "trash",
            "trash",
            CodatAccountStatus.ACTIVE,
            "category",
            "dont.want.something.this",
            CodatAccountType.EQUITY));
    accountList.add(
        new CodatAccount(
            "fixed",
            "my asset",
            CodatAccountStatus.ACTIVE,
            "category",
            "Asset.Fixed Asset.something.my asset",
            CodatAccountType.ASSET));
  }

  @Override
  public GetAccountsResponse getAccountsForBusiness(String companyRef) {

    return new GetAccountsResponse(accountList);
  }

  public GetSuppliersResponse getSuppliersForBusiness(String companyRef) {
    return new GetSuppliersResponse(supplierList);
  }

  public GetSuppliersResponse getSupplierForBusiness(String companyRef, String supplierName) {
    List<CodatSupplier> list = new ArrayList<>();
    list.addAll(
        supplierList.stream()
            .filter(supplier -> supplier.getSupplierName().equals(supplierName))
            .collect(Collectors.toList()));
    return new GetSuppliersResponse(list);
  }

  public ConnectionStatusResponse getConnectionsForBusiness(String companyRef) {
    return new ConnectionStatusResponse(List.of(new ConnectionStatus("Linked", "test-connection")));
  }

  public CodatPushDataResponse syncSupplierToCodat(
      String companyRef, String connectionId, CodatSupplierRequest supplier) {
    return new CodatPushDataResponse("Started", "test-push-operation-key-supplier");
  }

  public CodatSyncDirectCostResponse syncDirectCostToCodat(
      String companyRef, String connectionId, DirectCostRequest request) {
    return new CodatSyncDirectCostResponse("Started", "test-push-operation-key-cost");
  }

  public CodatSyncDirectCostResponse syncTransactionAsDirectCost(
      String companyRef,
      String connectionId,
      AccountActivity transaction,
      String currency,
      CodatSupplier supplier,
      CodatAccount expenseAccount,
      String expenseCategoryRef) {
    return new CodatSyncDirectCostResponse("Started", "test-push-operation-key-cost");
  }

  public void addSupplierToList(CodatSupplier supplier) {
    this.supplierList.add(supplier);
  }

  public CodatPushStatusResponse getPushStatus(String pushOperationKey, String companyRef) {
    CodatPushStatusResponse success = new CodatPushStatusResponse("Success");
    success.setData(new CodatPushData("123"));
    success.getData().setSupplierName("supplier-1");
    return success;
  }

  public Boolean deleteCodatIntegrationConnectionForBusiness(
      String companyRef, String connectionId) {
    return true;
  }

  public CodatBankAccountStatusResponse getBankAccountDetails(
      String pushOperationKey, String companyRef) {
    return new CodatBankAccountStatusResponse(
        "Success", new CodatBankAccount("1234", "Clearspend Card"));
  }

  public CodatCreateBankAccountResponse createBankAccountForBusiness(
      String companyRef, String connectionId, String accountName) throws CodatApiCallException {
    return new CodatCreateBankAccountResponse(
        new CodatValidation(new ArrayList()), "push-key", "Success");
  }

  public CodatSyncResponse syncDataTypeForCompany(String companyRef, String dataType) {
    return new CodatSyncResponse("company-id", companyRef, dataType);
  }

  public CodatSyncReceiptResponse syncReceiptsForDirectCost(
      CodatSyncReceiptRequest codatSyncReceiptRequest) {
    return new CodatSyncReceiptResponse("A-OK", "push-key");
  }

  @Override
  public CreateCompanyResponse createCodatCompanyForBusiness(String legalName)
      throws CodatApiCallException {
    return new CreateCompanyResponse("testing");
  }

  @Override
  public CreateIntegrationResponse createQboConnectionForBusiness(String companyRef) {
    return new CreateIntegrationResponse("testing");
  }

  private List<CodatBankAccount> bankAccounts =
      List.of(
          new CodatBankAccount("1234", "Clearspend Card"),
          new CodatBankAccount("1", "testing-1"),
          new CodatBankAccount("2", "testing-2"),
          new CodatBankAccount("3", "testing-3"));

  @Override
  public CodatBankAccountsResponse getBankAccountForBusinessByAccountName(
      String companyRef, String connectionId, String accountName) {
    return bankAccounts.stream()
        .filter(it -> it.getAccountName().equals(accountName))
        .map(it -> new CodatBankAccountsResponse(List.of(it)))
        .findFirst()
        .orElseThrow(() -> new WebClientResponseException(0, "", null, null, null));
  }

  @Override
  public CodatBankAccount getBankAccountById(String companyRef, String connectionId, String id) {
    return bankAccounts.stream()
        .filter(it -> it.getId().equals(id))
        .findFirst()
        .orElseThrow(() -> new WebClientResponseException(0, "", null, null, null));
  }

  @Override
  public CodatBankAccountsResponse getBankAccountsForBusiness(
      String companyRef, String connectionId) {
    return new CodatBankAccountsResponse(bankAccounts);
  }
}
