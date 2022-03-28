package com.clearspend.capital.client.codat;

import com.clearspend.capital.client.codat.types.CodatAccount;
import com.clearspend.capital.client.codat.types.CodatAccountStatus;
import com.clearspend.capital.client.codat.types.CodatAccountType;
import com.clearspend.capital.client.codat.types.CodatBankAccount;
import com.clearspend.capital.client.codat.types.CodatBankAccountStatusResponse;
import com.clearspend.capital.client.codat.types.CodatCreateBankAccountResponse;
import com.clearspend.capital.client.codat.types.CodatPushDataResponse;
import com.clearspend.capital.client.codat.types.CodatPushStatusResponse;
import com.clearspend.capital.client.codat.types.CodatSupplier;
import com.clearspend.capital.client.codat.types.CodatSupplierRequest;
import com.clearspend.capital.client.codat.types.CodatSyncDirectCostResponse;
import com.clearspend.capital.client.codat.types.CodatSyncResponse;
import com.clearspend.capital.client.codat.types.CodatValidation;
import com.clearspend.capital.client.codat.types.ConnectionStatus;
import com.clearspend.capital.client.codat.types.ConnectionStatusResponse;
import com.clearspend.capital.client.codat.types.DirectCostRequest;
import com.clearspend.capital.client.codat.types.GetAccountsResponse;
import com.clearspend.capital.client.codat.types.GetSuppliersResponse;
import com.clearspend.capital.common.error.CodatApiCallException;
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

  public CodatMockClient(WebClient codatWebClient, ObjectMapper objectMapper) {
    super(codatWebClient, objectMapper);
    supplierList = new ArrayList<>();
    supplierList.add(new CodatSupplier("1", "Test Business", "ACTIVE", "USD"));
  }

  @Override
  public GetAccountsResponse getAccountsForBusiness(String companyRef) {
    List<CodatAccount> accountList = new ArrayList<CodatAccount>();
    accountList.add(
        new CodatAccount(
            "codat-card-id",
            "checking",
            CodatAccountStatus.ACTIVE,
            "category",
            "full_name",
            CodatAccountType.ASSET));
    accountList.add(
        new CodatAccount(
            "banking",
            "checking_account",
            CodatAccountStatus.ACTIVE,
            "category",
            "full_name",
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

  public void addSupplierToList(CodatSupplier supplier) {
    this.supplierList.add(supplier);
  }

  public CodatPushStatusResponse getPushStatus(String pushOperationKey, String companyRef) {
    return new CodatPushStatusResponse("Success");
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
}
