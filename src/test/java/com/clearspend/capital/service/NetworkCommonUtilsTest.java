package com.clearspend.capital.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.clearspend.capital.client.stripe.types.TransactionType;
import com.clearspend.capital.data.model.Account;
import com.clearspend.capital.data.model.Allocation;
import com.clearspend.capital.data.model.Card;
import com.clearspend.capital.data.model.enums.network.NetworkMessageType;
import com.clearspend.capital.service.type.NetworkCommon;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class NetworkCommonUtilsTest {

  private NetworkCommon common;

  @BeforeEach
  void setup() {
    final Allocation allocation = new Allocation();
    final Account account = new Account();
    allocation.setAccountId(account.getId());
    account.setAllocationId(allocation.getId());
    final Card card = new Card();
    card.setAllocationId(allocation.getId());
    card.setAccountId(account.getId());

    common = new NetworkCommon();
    common.setAllocation(allocation);
    common.setAccount(account);
    common.setCard(card);
    common.setStripeAuthorizationExternalRef(UUID.randomUUID().toString());
  }

  private void clearAuth(final NetworkCommon common) {
    common.setStripeAuthorizationExternalRef(null);
  }

  private void unlinkCard(final NetworkCommon common) {
    common.getCard().setAllocationId(null);
    common.getCard().setAccountId(null);
  }

  private void archiveAllocation(final NetworkCommon common) {
    common.getAllocation().setArchived(true);
  }

  private void setIsAuthorization(final NetworkCommon common) {
    common.setNetworkMessageType(NetworkMessageType.AUTH_REQUEST);
  }

  private void setIsRefund(final NetworkCommon common) {
    common.setNetworkMessageType(NetworkMessageType.TRANSACTION_CREATED);
    common.setNetworkMessageSubType(TransactionType.REFUND.getStripeKey());
  }

  private void setIsCapture(final NetworkCommon common) {
    common.setNetworkMessageType(NetworkMessageType.TRANSACTION_CREATED);
    common.setNetworkMessageSubType(TransactionType.CAPTURE.getStripeKey());
  }

  @Test
  void isAuthorization() {
    setIsAuthorization(common);
    assertFalse(NetworkCommonUtils.test(common));
  }

  @Test
  void isCapture_HasAuth_Normal() {
    setIsCapture(common);
    assertFalse(NetworkCommonUtils.test(common));
  }

  @Test
  void isRefund_HasAuth_Normal() {
    setIsRefund(common);
    assertFalse(NetworkCommonUtils.test(common));
  }

  @Test
  void isCapture_HasAuth_CardUnlinked() {
    setIsCapture(common);
    unlinkCard(common);
    assertFalse(NetworkCommonUtils.test(common));
  }

  @Test
  void isRefund_HasAuth_CardUnlinked() {
    setIsRefund(common);
    unlinkCard(common);
    assertFalse(NetworkCommonUtils.test(common));
  }

  @Test
  void isCapture_NoAuth_CardUnlinked() {
    setIsCapture(common);
    clearAuth(common);
    unlinkCard(common);
    assertTrue(NetworkCommonUtils.test(common));
  }

  @Test
  void isCapture_NoAuth_AllocationArchived() {
    setIsCapture(common);
    clearAuth(common);
    archiveAllocation(common);
    assertTrue(NetworkCommonUtils.test(common));
  }

  @Test
  void isRefund_NoAuth_CardUnlinked() {
    setIsRefund(common);
    clearAuth(common);
    unlinkCard(common);
    assertTrue(NetworkCommonUtils.test(common));
  }

  @Test
  void isRefund_NoAuth_AllocationArchived() {
    setIsRefund(common);
    clearAuth(common);
    archiveAllocation(common);
    assertTrue(NetworkCommonUtils.test(common));
  }

  @Test
  void isRefund_HasAuth_AllocationArchived() {
    setIsRefund(common);
    archiveAllocation(common);
    assertTrue(NetworkCommonUtils.test(common));
  }
}
