package com.clearspend.capital.client.codat;

import com.clearspend.capital.client.codat.types.CodatAccount;
import com.clearspend.capital.client.codat.types.CodatAccountStatus;
import com.clearspend.capital.client.codat.types.CodatAccountType;
import com.clearspend.capital.client.codat.types.CodatBankAccount;
import com.clearspend.capital.client.codat.types.CodatBankAccountStatusResponse;
import com.clearspend.capital.client.codat.types.CodatCreateBankAccountResponse;
import com.clearspend.capital.client.codat.types.CodatDataIdStub;
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
import com.clearspend.capital.client.codat.types.DirectCostRequest;
import com.clearspend.capital.client.codat.types.GetAccountsResponse;
import com.clearspend.capital.client.codat.types.GetSuppliersResponse;
import com.clearspend.capital.common.error.CodatApiCallException;
import com.clearspend.capital.data.model.AccountActivity;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

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
            "Asset.accounts.full_name",
            CodatAccountType.ASSET));
    accountList.add(
        new CodatAccount(
            "banking",
            "checking_account",
            CodatAccountStatus.ACTIVE,
            "category",
            "Asset.accounts.full_name",
            CodatAccountType.ASSET));
    accountList.add(
        new CodatAccount(
            "auto",
            "automobile",
            CodatAccountStatus.ACTIVE,
            "category",
            "expense.auto.auto",
            CodatAccountType.EXPENSE));
    accountList.add(
        new CodatAccount(
            "fuel",
            "fuel",
            CodatAccountStatus.ACTIVE,
            "category",
            "expense.auto.fuel",
            CodatAccountType.EXPENSE));
    accountList.add(
        new CodatAccount(
            "trash",
            "trash",
            CodatAccountStatus.ACTIVE,
            "category",
            "dont.want.this",
            CodatAccountType.EQUITY));
    accountList.add(
        new CodatAccount(
            "fixed",
            "my asset",
            CodatAccountStatus.ACTIVE,
            "category",
            "Asset.Fixed Asset.my asset",
            CodatAccountType.ASSET));
  }

  @Override
  public GetAccountsResponse getAccountsForBusiness(String companyRef) {

    return new GetAccountsResponse(accountList);
  }

  private <T> T getFromCodatApi(String uri, Class<T> clazz) {
    if (uri.equals("companies/test-codat-ref/data/suppliers")) {
      return (T) List.of(new CodatSupplier("1", "Test Business", "ACTIVE", "USD"));
    }
    return null;
  }

  public GetSuppliersResponse getSuppliersForBusiness(String companyRef) {
    return new GetSuppliersResponse(supplierList);
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
    success.setDataId(new CodatDataIdStub("123"));
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
}
