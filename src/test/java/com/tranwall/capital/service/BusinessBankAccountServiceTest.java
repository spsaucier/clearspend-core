package com.tranwall.capital.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.plaid.client.model.Address;
import com.plaid.client.model.AddressData;
import com.plaid.client.model.Owner;
import com.tranwall.capital.BaseCapitalTest;
import com.tranwall.capital.TestHelper;
import com.tranwall.capital.TestHelper.CreateBusinessRecord;
import com.tranwall.capital.common.data.model.Amount;
import com.tranwall.capital.common.error.InsufficientFundsException;
import com.tranwall.capital.common.typedid.data.BusinessBankAccountId;
import com.tranwall.capital.common.typedid.data.BusinessId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.crypto.data.model.embedded.EncryptedString;
import com.tranwall.capital.crypto.data.model.embedded.NullableEncryptedString;
import com.tranwall.capital.data.model.Bin;
import com.tranwall.capital.data.model.BusinessOwner;
import com.tranwall.capital.data.model.Program;
import com.tranwall.capital.data.model.enums.BankAccountTransactType;
import com.tranwall.capital.data.model.enums.Country;
import com.tranwall.capital.data.model.enums.Currency;
import com.tranwall.capital.service.AccountService.AdjustmentRecord;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import javax.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
@Transactional
class BusinessBankAccountServiceTest extends BaseCapitalTest {

  @Autowired private TestHelper testHelper;
  @Autowired private BusinessBankAccountService bankAccountService;

  private Bin bin;
  private Program program;

  @BeforeEach
  public void setup() {
    if (bin == null) {
      bin = testHelper.createBin();
      program = testHelper.createProgram(bin);
    }
  }

  @Test
  void depositFunds() {
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    TypedId<BusinessBankAccountId> businessBankAccountId =
        testHelper.createBusinessBankAccount(createBusinessRecord.business().getId());
    AdjustmentRecord adjustmentRecord =
        bankAccountService.transactBankAccount(
            createBusinessRecord.business().getId(),
            businessBankAccountId,
            BankAccountTransactType.DEPOSIT,
            Amount.of(Currency.USD, new BigDecimal("1000")),
            true);
  }

  @Test
  void withdrawFunds_success() {
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    TypedId<BusinessId> businessId = createBusinessRecord.business().getId();
    TypedId<BusinessBankAccountId> businessBankAccountId =
        testHelper.createBusinessBankAccount(businessId);
    bankAccountService.transactBankAccount(
        createBusinessRecord.business().getId(),
        businessBankAccountId,
        BankAccountTransactType.DEPOSIT,
        Amount.of(Currency.USD, new BigDecimal("700.51")),
        false);
    AdjustmentRecord adjustmentRecord =
        bankAccountService.transactBankAccount(
            createBusinessRecord.business().getId(),
            businessBankAccountId,
            BankAccountTransactType.WITHDRAW,
            Amount.of(Currency.USD, new BigDecimal("241.85")),
            true);
  }

  @Test
  void withdrawFunds_insufficientBalance() {
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    TypedId<BusinessId> businessId = createBusinessRecord.business().getId();
    TypedId<BusinessBankAccountId> businessBankAccountId =
        testHelper.createBusinessBankAccount(businessId);
    bankAccountService.transactBankAccount(
        createBusinessRecord.business().getId(),
        businessBankAccountId,
        BankAccountTransactType.DEPOSIT,
        Amount.of(Currency.USD, new BigDecimal("710.51")),
        true);

    InsufficientFundsException insufficientFundsException =
        assertThrows(
            InsufficientFundsException.class,
            () ->
                bankAccountService.transactBankAccount(
                    createBusinessRecord.business().getId(),
                    businessBankAccountId,
                    BankAccountTransactType.WITHDRAW,
                    Amount.of(Currency.USD, new BigDecimal("1.85")),
                    true));
  }

  @Test
  void validateOwners() {
    // the code used by the PlaidAPI to parse this in the http response is byzantine and not
    // suited to re-use in this context
    Owner alice = new Owner();
    alice.setNames(Collections.singletonList("Alice Kramden"));
    alice.setAddresses(List.of(plaidAddress("328 Chauncey Street", "Brooklyn", "NY", "11233")));
    List<Owner> owners = Collections.singletonList(alice);

    BusinessOwner aliceUser = mock(BusinessOwner.class);
    when(aliceUser.getLastName()).thenReturn(new NullableEncryptedString("Alice"));
    when(aliceUser.getFirstName()).thenReturn(new NullableEncryptedString("Kramden"));
    com.tranwall.capital.common.data.model.Address csaddress =
        csAddress("328 Chauncey Street", "Brooklyn", "NY", "11233");
    when(aliceUser.getAddress()).thenReturn(csaddress);

    assertTrue(bankAccountService.validateOwners(Collections.singletonList(aliceUser), owners));

    BusinessOwner bobUser = mock(BusinessOwner.class);
    when(bobUser.getLastName()).thenReturn(new NullableEncryptedString("Bob"));
    when(bobUser.getFirstName()).thenReturn(new NullableEncryptedString("Barker"));
    com.tranwall.capital.common.data.model.Address bobAddress =
        csAddress("7800 Beverly Blvd", "Los Angeles", "CA", "90036");
    when(bobUser.getAddress()).thenReturn(bobAddress);

    assertFalse(bankAccountService.validateOwners(Collections.singletonList(bobUser), owners));
  }

  private com.tranwall.capital.common.data.model.Address csAddress(
      String street, String city, String state, String zip) {
    com.tranwall.capital.common.data.model.Address address =
        mock(com.tranwall.capital.common.data.model.Address.class);
    when(address.getCountry()).thenReturn(Country.USA);
    when(address.getStreetLine1()).thenReturn(new EncryptedString(street));
    when(address.getRegion()).thenReturn(state);
    when(address.getPostalCode()).thenReturn(new EncryptedString(zip));
    when(address.getLocality()).thenReturn(city);
    return address;
  }

  private Address plaidAddress(String street, String city, String state, String zip) {
    AddressData ad = new AddressData();
    ad.setCountry("US");
    ad.setPostalCode(zip);
    ad.setCity(city);
    ad.setStreet(street);
    ad.setRegion(state);
    Address a = new Address();
    a.setData(ad);
    return a;
  }
}
