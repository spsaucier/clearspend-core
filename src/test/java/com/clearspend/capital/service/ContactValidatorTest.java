package com.clearspend.capital.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.crypto.data.model.embedded.EncryptedString;
import com.clearspend.capital.crypto.data.model.embedded.NullableEncryptedString;
import com.clearspend.capital.data.model.BusinessOwner;
import com.clearspend.capital.data.model.enums.Country;
import com.plaid.client.model.Address;
import com.plaid.client.model.AddressData;
import com.plaid.client.model.Owner;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
class ContactValidatorTest extends BaseCapitalTest {

  @Autowired private ContactValidator validator;

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
    com.clearspend.capital.common.data.model.Address csaddress =
        csAddress("328 Chauncey Street", "Brooklyn", "NY", "11233");
    when(aliceUser.getAddress()).thenReturn(csaddress);

    assertTrue(validator.validateOwners(Collections.singletonList(aliceUser), owners).isValid());

    BusinessOwner bobUser = mock(BusinessOwner.class);
    when(bobUser.getLastName()).thenReturn(new NullableEncryptedString("Bob"));
    when(bobUser.getFirstName()).thenReturn(new NullableEncryptedString("Barker"));
    com.clearspend.capital.common.data.model.Address bobAddress =
        csAddress("7800 Beverly Blvd", "Los Angeles", "CA", "90036");
    when(bobUser.getAddress()).thenReturn(bobAddress);

    assertFalse(validator.validateOwners(Collections.singletonList(bobUser), owners).isValid());
  }

  private com.clearspend.capital.common.data.model.Address csAddress(
      String street, String city, String state, String zip) {
    com.clearspend.capital.common.data.model.Address address =
        mock(com.clearspend.capital.common.data.model.Address.class);
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
