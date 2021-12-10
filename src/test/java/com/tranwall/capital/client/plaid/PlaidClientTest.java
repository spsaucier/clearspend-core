package com.tranwall.capital.client.plaid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.plaid.client.model.AccountBase;
import com.plaid.client.model.AccountIdentity;
import com.plaid.client.model.Address;
import com.plaid.client.model.Owner;
import com.plaid.client.model.Products;
import com.tranwall.capital.BaseCapitalTest;
import com.tranwall.capital.client.plaid.PlaidClient.OwnersResponse;
import com.tranwall.capital.common.typedid.data.BusinessId;
import com.tranwall.capital.common.typedid.data.TypedId;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
class PlaidClientTest extends BaseCapitalTest {

  @Autowired private PlaidClient underTest;

  @Test
  void getAccounts() throws IOException {
    assumeTrue(underTest.isConfigured());
    String linkToken =
        underTest.createLinkToken(businessId(), Arrays.asList(Products.AUTH, Products.IDENTITY));
    String accessToken = underTest.exchangePublicTokenForAccessToken(linkToken);
    PlaidClient.AccountsResponse accounts = underTest.getAccounts(accessToken);
    assertNotNull(accounts);
  }

  @Test
  void getOwners() throws IOException {
    assumeTrue(underTest.isConfigured());
    String linkToken =
        underTest.createLinkToken(businessId(), Arrays.asList(Products.AUTH, Products.IDENTITY));
    String accessToken = underTest.exchangePublicTokenForAccessToken(linkToken);
    PlaidClient.OwnersResponse owners = underTest.getOwners(accessToken);
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
    String accessToken = underTest.exchangePublicTokenForAccessToken(linkToken);
    PlaidClient.AccountsResponse accounts = underTest.getAccounts(accessToken);
    PlaidClient.OwnersResponse owners = underTest.getOwners(accessToken);
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
  }

  @Test
  void getAccountsAndOwnersButTheInstitutionDoesNotSupportOwners() throws IOException {
    assumeTrue(underTest.isConfigured());
    String linkToken =
        underTest.createLinkToken(
            TestPlaidClient.SANDBOX_INSTITUTIONS_BY_NAME.get("Flexible Platypus Open Banking"));
    String accessToken = underTest.exchangePublicTokenForAccessToken(linkToken);
    PlaidClient.AccountsResponse accounts = underTest.getAccounts(accessToken);
    assertFalse(accounts.accounts().isEmpty());
    PlaidClientException e =
        assertThrows(PlaidClientException.class, () -> underTest.getOwners(accessToken));
    assertEquals(PlaidErrorCode.PRODUCTS_NOT_SUPPORTED, e.getErrorCode());
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
            try {
              linkToken =
                  underTest.createLinkToken(
                      institution.businessId(), Arrays.asList(Products.AUTH, Products.IDENTITY));
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
            String accessToken = underTest.exchangePublicTokenForAccessToken(linkToken);
            OwnersResponse response = underTest.getOwners(accessToken);
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

    // Just a few basic assertions to show items that are validated
    // debug or print out the rows to get the full table.
    // rows.forEach(r -> System.out.println(String.join("\t",r.toArray(new String[0]))));
    assertTrue(uniqueCells.contains("93405-2255"));
    assertTrue(uniqueCells.contains("[Alberta Bobbeth Charleson]"));
    assertTrue(uniqueCells.contains("First Gingham Credit Union"));
  }

  private TypedId<BusinessId> businessId() {
    return businessId("Tartan Bank");
  }

  private TypedId<BusinessId> businessId(String bankName) {
    if (!TestPlaidClient.SANDBOX_INSTITUTIONS_BY_NAME.containsKey(bankName)) {
      throw new NoSuchElementException(bankName);
    }
    return TestPlaidClient.SANDBOX_INSTITUTIONS_BY_NAME.get(bankName);
  }
}
