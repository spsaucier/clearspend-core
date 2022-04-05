package com.clearspend.capital.client.plaid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.client.plaid.PlaidClient.OwnersResponse;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.data.model.PlaidLogEntry;
import com.clearspend.capital.data.model.enums.PlaidResponseType;
import com.clearspend.capital.data.repository.PlaidLogEntryRepository;
import com.plaid.client.model.AccountBase;
import com.plaid.client.model.AccountIdentity;
import com.plaid.client.model.Address;
import com.plaid.client.model.Owner;
import com.plaid.client.model.Products;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
public class PlaidClientTest extends BaseCapitalTest {

  @Autowired private PlaidClient underTest;

  @Autowired private PlaidLogEntryRepository plaidLogEntryRepository;

  @Autowired private TestHelper testHelper;

  @BeforeEach
  void init() {
    assumeTrue(underTest.isConfigured());
    TestPlaidClient.INSTITUTION_SANDBOX_ID_BY_BUSINESS_ID
        .keySet()
        .forEach(testHelper::createBusiness);
  }

  @Test
  void getAccounts() throws IOException {
    assumeTrue(underTest.isConfigured());
    String linkToken =
        underTest.createLinkToken(businessId(), Arrays.asList(Products.AUTH, Products.IDENTITY));
    String accessToken = underTest.exchangePublicTokenForAccessToken(linkToken, businessId());
    PlaidClient.AccountsResponse accounts = underTest.getAccounts(accessToken, businessId());
    assertNotNull(accounts);

    checkLastLogTypes(
        PlaidResponseType.SANDBOX_LINK_TOKEN,
        PlaidResponseType.ACCESS_TOKEN,
        PlaidResponseType.ACCOUNT);
  }

  void checkLastLogTypes(PlaidResponseType... expectedTypes) {
    checkLastLogTypes(businessId(), expectedTypes);
  }

  void checkLastLogTypes(TypedId<BusinessId> businessId, PlaidResponseType... expectedTypes) {
    List<PlaidLogEntry> logEntries =
        plaidLogEntryRepository.findByBusinessIdOrderByCreatedAsc(businessId);
    for (int i = 0; i < expectedTypes.length; i++) {
      assertEquals(
          expectedTypes[i],
          logEntries.get(logEntries.size() - expectedTypes.length + i).getPlaidResponseType());
    }
  }

  @Test
  void getOwners() throws IOException {
    assumeTrue(underTest.isConfigured());
    String linkToken =
        underTest.createLinkToken(businessId(), Arrays.asList(Products.AUTH, Products.IDENTITY));
    String accessToken = underTest.exchangePublicTokenForAccessToken(linkToken, businessId());
    PlaidClient.OwnersResponse owners = underTest.getOwners(accessToken, businessId());
    assertNotNull(owners);
    Owner owner = owners.accounts().get(0).getOwners().get(0);
    assertNotNull(owner);
    assertEquals("Alberta Bobbeth Charleson", owner.getNames().get(0));
    assertEquals("14236", owner.getAddresses().get(0).getData().getPostalCode());
    assertEquals("93405-2255", owner.getAddresses().get(1).getData().getPostalCode());
  }

  @Test
  void getAccountsAndOwners() throws IOException {
    assumeTrue(underTest.isConfigured());
    String linkToken =
        underTest.createLinkToken(businessId(), Arrays.asList(Products.AUTH, Products.IDENTITY));
    String accessToken = underTest.exchangePublicTokenForAccessToken(linkToken, businessId());
    PlaidClient.AccountsResponse accounts = underTest.getAccounts(accessToken, businessId());
    PlaidClient.OwnersResponse owners = underTest.getOwners(accessToken, businessId());
    assertNotNull(accounts);
    assertNotNull(owners);

    // Verifying that when the same accessToken is used to make both calls, the account IDs match.
    // (they do not match between different accessTokens)
    Set<String> accessAccts =
        accounts.accounts().stream().map(AccountBase::getAccountId).collect(Collectors.toSet());
    Set<String> ownerAccts =
        owners.accounts().stream().map(AccountIdentity::getAccountId).collect(Collectors.toSet());
    assertEquals(accessAccts, ownerAccts);
    assertFalse(accessAccts.isEmpty());

    checkLastLogTypes(
        PlaidResponseType.SANDBOX_LINK_TOKEN,
        PlaidResponseType.ACCESS_TOKEN,
        PlaidResponseType.ACCOUNT,
        PlaidResponseType.OWNER);
  }

  @Test
  void getAccountsAndOwnersButTheInstitutionDoesNotSupportOwners() throws IOException {
    assumeTrue(underTest.isConfigured());
    TypedId<BusinessId> businessId =
        TestPlaidClient.SANDBOX_INSTITUTIONS_BY_NAME.get("Tattersall Federal Credit Union");
    String linkToken = underTest.createLinkToken(businessId);
    String accessToken = underTest.exchangePublicTokenForAccessToken(linkToken, businessId);
    PlaidClient.AccountsResponse accounts = underTest.getAccounts(accessToken, businessId);
    assertFalse(accounts.accounts().isEmpty());
    PlaidClientException e =
        assertThrows(
            PlaidClientException.class, () -> underTest.getOwners(accessToken, businessId));
    assertEquals(PlaidErrorCode.PRODUCTS_NOT_SUPPORTED, e.getErrorCode());

    checkLastLogTypes(
        businessId,
        PlaidResponseType.SANDBOX_LINK_TOKEN,
        PlaidResponseType.ACCESS_TOKEN,
        PlaidResponseType.ACCOUNT,
        PlaidResponseType.ERROR);
  }

  @Test
  void testBalanceCheck() throws IOException {
    assumeTrue(underTest.isConfigured());
    String linkToken = underTest.createLinkToken(businessId());
    String accessToken = underTest.exchangePublicTokenForAccessToken(linkToken, businessId());
    PlaidClient.AccountsResponse accounts = underTest.getAccounts(accessToken, businessId());
    List<AccountBase> balances = underTest.getBalances(businessId(), accessToken);
    assertNotNull(balances);
    assertNotNull(accounts);

    checkLastLogTypes(
        PlaidResponseType.SANDBOX_LINK_TOKEN,
        PlaidResponseType.ACCESS_TOKEN,
        PlaidResponseType.ACCOUNT,
        PlaidResponseType.BALANCE);
  }

  @Test
  void testBalanceCheckAfterChangePassword_CAP_859() throws IOException {
    assumeTrue(underTest.isConfigured());
    String linkToken = underTest.createLinkToken(businessId());
    String accessToken = underTest.exchangePublicTokenForAccessToken(linkToken, businessId());
    PlaidClient.AccountsResponse accounts = underTest.getAccounts(accessToken, businessId());
    List<AccountBase> balances = underTest.getBalances(businessId(), accessToken);
    assertNotNull(balances);
    assertNotNull(accounts);

    underTest.sandboxItemResetLogin(businessId(), accessToken);

    // TODO Throws something - make sure it's meaningful, translate into HTTP for controller
    Assertions.assertThatExceptionOfType(PlaidClientException.class)
        .isThrownBy(() -> underTest.getBalances(businessId(), accessToken));

    String newLinkToken = underTest.createLinkToken(businessId(), accessToken);
    // Mock Link to use the link token to tie things together. (but they have to tell me how)
    // List<AccountBase> newBalances = underTest.getBalances(businessId(), newAccessToken);
    // assertNotNull(newBalances);
    /*
        https://dashboard.plaid.com/support/case/410638
        There's not a way to re-authenticate or update an item without using the Link UI just like
        there's not a way to create an Item initially without using the Link UI. The exception,
        obviously, is the sandbox endpoint to create a link token, but this endpoint does not work
        for update mode.

        In the standard Link flow, you begin the process by calling /link/token/create to
        create a link_token . Then you use the temporary public_token and exchange that for
        the permanent access_token. The access_token is then what you use for making calls to the Item.

        With update mode, you do not need the original link_token used to create the Item. An Item's
        access_token does not change when using Link in update mode, so there is no need to repeat
        the exchange token process. You just need to initialize Link with a link_token configured
        with the access_token for the Item that you wish to update. You can create this link_token
        using the /link/token/create endpoint as normal, but no products should be specified when
        creating the link_token for update mode.

        When launched with this new token, Link will automatically detect the institution ID
        associated with the link_token and present the appropriate credential view to your user.

        This credential re-validation will need to be done via the UI, just like the initial Link,
        as that is all done through Plaid's back-end and these user credentials are not stored or
        visible anywhere for security purposes.
    */
  }

  /**
   * This method was used to dump out the accounts in the sandbox for banks referenced in their
   * sandbox documentation, so that we would have a list of information we would be checking
   * against.
   *
   * <p>By exercising every bank in their test set, it revealed some subtle issues in API coding.
   */
  @Test
  void getAllOwners() {
    assumeTrue(underTest.isConfigured());
    List<List<String>> rows = new ArrayList<>();
    TestPlaidClient.SANDBOX_INSTITUTIONS.forEach(
        institution -> {
          try {
            String linkToken;
            TypedId<BusinessId> businessId = institution.businessId();
            try {
              linkToken =
                  underTest.createLinkToken(
                      businessId, Arrays.asList(Products.AUTH, Products.IDENTITY));
            } catch (PlaidClientException e) {
              switch (e.getErrorCode()) {
                case INVALID_PRODUCT, PRODUCTS_NOT_SUPPORTED -> {
                  log.info("Institution {}: {}", institution.name(), e.getErrorCode());
                  log.debug("PlaidClientException", e);
                  return;
                }
                default -> throw e;
              }
            }

            String accessToken = underTest.exchangePublicTokenForAccessToken(linkToken, businessId);
            OwnersResponse response = underTest.getOwners(accessToken, businessId);
            // log.info(institution.name());
            response
                .accounts()
                .forEach(
                    accountIdentity ->
                        accountIdentity
                            .getOwners()
                            .forEach(
                                owner ->
                                    owner.getAddresses().stream()
                                        .map(Address::getData)
                                        .forEach(
                                            address ->
                                                rows.add(
                                                    Arrays.asList(
                                                        institution.name(),
                                                        accountIdentity.getName(),
                                                        accountIdentity.getOfficialName(),
                                                        accountIdentity.getType().toString(),
                                                        accountIdentity.getMask(),
                                                        String.valueOf(
                                                            accountIdentity
                                                                .getBalances()
                                                                .getCurrent()),
                                                        String.valueOf(
                                                            accountIdentity
                                                                .getBalances()
                                                                .getAvailable()),
                                                        String.valueOf(
                                                            accountIdentity
                                                                .getBalances()
                                                                .getLimit()),
                                                        accountIdentity
                                                            .getBalances()
                                                            .getIsoCurrencyCode(),
                                                        owner.getNames().toString(),
                                                        address.getStreet(),
                                                        address.getCity(),
                                                        address.getRegion(),
                                                        address.getPostalCode(),
                                                        address.getCountry())))));
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        });
    Set<String> uniqueCells = rows.stream().flatMap(Collection::stream).collect(Collectors.toSet());

    // debug or print out the rows to get the full table.
    /*
     System.out.println(
        "Bank\tAccount name\tOfficial Account Name\tType\tMask\tCurrent Bal\tAvail Bal\tLimit\tBal Curr\tOwner\tStreet\tCity\tRegion\tPostCode\tCountry");
    rows.forEach(r -> System.out.println(String.join("\t", r.toArray(new String[0]))));
     */

    // Just a few basic assertions to show items that are validated
    assertTrue(uniqueCells.contains("93405-2255"));
    assertTrue(uniqueCells.contains("[Alberta Bobbeth Charleson]"));
    assertTrue(uniqueCells.contains("First Gingham Credit Union"));
  }

  public static TypedId<BusinessId> businessId() {
    return businessId("Tartan Bank");
  }

  public static TypedId<BusinessId> businessId(String bankName) {
    return Optional.ofNullable(TestPlaidClient.SANDBOX_INSTITUTIONS_BY_NAME.get(bankName))
        .orElseThrow(() -> new NoSuchElementException(bankName));
  }
}
