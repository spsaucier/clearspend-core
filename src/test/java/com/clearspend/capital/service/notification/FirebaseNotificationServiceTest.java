package com.clearspend.capital.service.notification;

import static org.assertj.core.api.Assertions.assertThat;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.client.firebase.FirebaseCloudMessageMockClient;
import com.clearspend.capital.client.firebase.FirebaseCloudMessageMockClient.NotificationObject;
import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.controller.nonprod.TestDataController;
import com.clearspend.capital.controller.nonprod.TestDataController.NetworkCommonAuthorization;
import com.clearspend.capital.data.model.Card;
import com.clearspend.capital.data.model.business.Business;
import com.clearspend.capital.data.model.business.BusinessBankAccount;
import com.clearspend.capital.data.model.enums.AccountActivityStatus;
import com.clearspend.capital.data.model.enums.AccountActivityType;
import com.clearspend.capital.data.model.enums.BankAccountTransactType;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.data.model.enums.FundingType;
import com.clearspend.capital.data.model.enums.card.CardType;
import com.clearspend.capital.data.model.security.DefaultRoles;
import com.clearspend.capital.service.BusinessBankAccountService;
import com.clearspend.capital.service.NetworkMessageService;
import com.clearspend.capital.service.UserService.CreateUpdateUserRecord;
import com.clearspend.capital.service.type.NetworkCommon;
import com.clearspend.capital.service.type.PushNotificationEvent;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__({@Autowired}))
class FirebaseNotificationServiceTest extends BaseCapitalTest {

  public static final String TEST_DEVICE = "test.device";
  public static final String VALID_TOKEN_DEVICE =
      "dBq0pAnQQS-5yMVVYkG9-6:QPA91bE1eG7v2-VRO97xtOjkcUEEIP7yHMzrke4G5yrlpdaoQAa-8UjIBLUgOTJrikHYCGiMTWZ5LKYLgWGFlrOfuK0dyyxzYOHGpDl3a8oRifLgE0CNB3weEYPMPlsXmRSGhPVk-6-4";
  private final TestHelper testHelper;
  private final DeviceRegistrationService deviceRegistrationService;
  private final FirebaseNotificationService firebaseNotificationService;

  private final BusinessBankAccountService businessBankAccountService;

  private final NetworkMessageService networkMessageService;

  private TestHelper.CreateBusinessRecord createBusinessRecord;

  @BeforeEach
  void setup() {
    if (createBusinessRecord == null) {
      createBusinessRecord = testHelper.createBusiness();
    }
  }

  @AfterEach
  void clear() {
    if (FirebaseCloudMessageMockClient.notificationQueue.get() != null) {
      FirebaseCloudMessageMockClient.notificationQueue.get().clear();
    }
  }

  @SneakyThrows
  @Test
  void sendNotification_approvedTxn() {
    Business business = createBusinessRecord.business();
    testHelper.setCurrentUser(createBusinessRecord.user());
    BusinessBankAccount businessBankAccount =
        testHelper.createBusinessBankAccount(createBusinessRecord.business().getId());

    businessBankAccountService.transactBankAccount(
        business.getId(),
        businessBankAccount.getId(),
        createBusinessRecord.user().getId(),
        BankAccountTransactType.DEPOSIT,
        Amount.of(Currency.USD, new BigDecimal("5000")),
        false);

    CreateUpdateUserRecord user =
        testHelper.createUserWithRole(
            createBusinessRecord.allocationRecord().allocation(), DefaultRoles.ALLOCATION_EMPLOYEE);
    deviceRegistrationService.saveUpdateDeviceTokens(user.user().getId(), List.of(TEST_DEVICE));
    Card card =
        testHelper.issueCard(
            business,
            createBusinessRecord.allocationRecord().allocation(),
            user.user(),
            Currency.USD,
            FundingType.POOLED,
            CardType.PHYSICAL,
            true);

    NetworkCommonAuthorization networkCommonAuthorization =
        TestDataController.generateAuthorizationNetworkCommon(
            user.user(),
            card,
            createBusinessRecord.allocationRecord().account(),
            Amount.of(Currency.USD, BigDecimal.valueOf(7)));

    final NetworkCommon common = networkCommonAuthorization.networkCommon();
    testHelper.runWithWebhookUser(
        createBusinessRecord.user(), () -> networkMessageService.processNetworkMessage(common));
    firebaseNotificationService.doPushNotification(new PushNotificationEvent(common));
    // For this PENDING NETWORK_AUTHORIZATION notification should not be added
    Assertions.assertTrue(
        CollectionUtils.isEmpty(FirebaseCloudMessageMockClient.notificationQueue.get()));
    assertThat(networkCommonAuthorization.networkCommon().isPostAdjustment()).isFalse();
    assertThat(networkCommonAuthorization.networkCommon().isPostDecline()).isFalse();
    assertThat(networkCommonAuthorization.networkCommon().isPostHold()).isTrue();

    final NetworkCommon common2 =
        TestDataController.generateCaptureNetworkCommon(
            business, networkCommonAuthorization.authorization());
    testHelper.runWithWebhookUser(
        createBusinessRecord.user(), () -> networkMessageService.processNetworkMessage(common2));
    firebaseNotificationService.doPushNotification(new PushNotificationEvent(common2));
    assertThat(common2.isPostAdjustment()).isTrue();
    assertThat(common2.isPostDecline()).isFalse();
    assertThat(common2.isPostHold()).isFalse();

    NotificationObject expected =
        new NotificationObject(
            "ClearSpend",
            "Your payment of -$7.00 to Tuscon Bakery was approved.",
            List.of(TEST_DEVICE));
    List<NotificationObject> notificationObjects =
        FirebaseCloudMessageMockClient.notificationQueue.get();
    Assertions.assertFalse(CollectionUtils.isEmpty(notificationObjects));
    Assertions.assertEquals(expected, notificationObjects.get(0));
  }

  @SneakyThrows
  @Test
  void sendNotification_declineTxn_insufficientFunds() {
    Business business = createBusinessRecord.business();
    testHelper.setCurrentUser(createBusinessRecord.user());

    CreateUpdateUserRecord user =
        testHelper.createUserWithRole(
            createBusinessRecord.allocationRecord().allocation(), DefaultRoles.ALLOCATION_EMPLOYEE);
    deviceRegistrationService.saveUpdateDeviceTokens(user.user().getId(), List.of(TEST_DEVICE));
    Card card =
        testHelper.issueCard(
            business,
            createBusinessRecord.allocationRecord().allocation(),
            user.user(),
            Currency.USD,
            FundingType.POOLED,
            CardType.PHYSICAL,
            true);

    NetworkCommonAuthorization networkCommonAuthorization =
        TestDataController.generateAuthorizationNetworkCommon(
            user.user(),
            card,
            createBusinessRecord.allocationRecord().account(),
            Amount.of(Currency.USD, BigDecimal.valueOf(70000)));

    final NetworkCommon common = networkCommonAuthorization.networkCommon();
    testHelper.runWithWebhookUser(
        createBusinessRecord.user(), () -> networkMessageService.processNetworkMessage(common));
    firebaseNotificationService.doPushNotification(new PushNotificationEvent(common));
    // For this auth notification should announce decline
    Assertions.assertFalse(
        CollectionUtils.isEmpty(FirebaseCloudMessageMockClient.notificationQueue.get()));
    assertThat(networkCommonAuthorization.networkCommon().isPostAdjustment()).isFalse();
    assertThat(networkCommonAuthorization.networkCommon().isPostDecline()).isTrue();
    assertThat(networkCommonAuthorization.networkCommon().isPostHold()).isFalse();

    NotificationObject expected =
        new NotificationObject(
            "ClearSpend",
            "Your payment of -$70000.00 to Tuscon Bakery was declined.",
            List.of(TEST_DEVICE));
    List<NotificationObject> notificationObjects =
        FirebaseCloudMessageMockClient.notificationQueue.get();
    Assertions.assertFalse(CollectionUtils.isEmpty(notificationObjects));
    Assertions.assertEquals(expected, notificationObjects.get(0));
  }

  @SneakyThrows
  @Test
  void sendNotification_refund() {
    testHelper.setCurrentUser(createBusinessRecord.user());
    deviceRegistrationService.saveUpdateDeviceTokens(
        createBusinessRecord.user().getId(), List.of(TEST_DEVICE));
    PushNotificationEvent event =
        new PushNotificationEvent(
            createBusinessRecord.business().getBusinessId(),
            createBusinessRecord.user().getId(),
            new TypedId<>(UUID.randomUUID().toString()),
            Amount.of(Currency.USD, 10),
            "merchant",
            AccountActivityStatus.PROCESSED,
            AccountActivityType.NETWORK_REFUND);
    firebaseNotificationService.doPushNotification(event);

    NotificationObject expected =
        new NotificationObject(
            "ClearSpend", "You received a refund of $10.00 from merchant.", List.of(TEST_DEVICE));
    List<NotificationObject> notificationObjects =
        FirebaseCloudMessageMockClient.notificationQueue.get();
    Assertions.assertFalse(CollectionUtils.isEmpty(notificationObjects));
    Assertions.assertEquals(expected, notificationObjects.get(0));
  }

  @SneakyThrows
  @Test
  void sendNotificationOnMultipleDevice_AndRemoveUnregisteredDevicesOnFirebase() {
    testHelper.setCurrentUser(createBusinessRecord.user());
    deviceRegistrationService.saveUpdateDeviceTokens(
        createBusinessRecord.user().getId(), List.of("unregistered", VALID_TOKEN_DEVICE));
    PushNotificationEvent event =
        new PushNotificationEvent(
            createBusinessRecord.business().getBusinessId(),
            createBusinessRecord.user().getId(),
            new TypedId<>(UUID.randomUUID().toString()),
            Amount.of(Currency.USD, 10),
            "merchant",
            AccountActivityStatus.PROCESSED,
            AccountActivityType.NETWORK_REFUND);
    firebaseNotificationService.doPushNotification(event);

    Assertions.assertTrue(
        deviceRegistrationService.findAllByUserId(createBusinessRecord.user().getId()).isEmpty());
  }

  @SneakyThrows
  @Test
  void doNotSendNotification_ForNotRegisteredUserDevice() {
    Business business = createBusinessRecord.business();
    testHelper.setCurrentUser(createBusinessRecord.user());

    CreateUpdateUserRecord user =
        testHelper.createUserWithRole(
            createBusinessRecord.allocationRecord().allocation(), DefaultRoles.ALLOCATION_EMPLOYEE);

    Card card =
        testHelper.issueCard(
            business,
            createBusinessRecord.allocationRecord().allocation(),
            user.user(),
            Currency.USD,
            FundingType.POOLED,
            CardType.PHYSICAL,
            true);

    NetworkCommonAuthorization networkCommonAuthorization =
        TestDataController.generateAuthorizationNetworkCommon(
            user.user(),
            card,
            createBusinessRecord.allocationRecord().account(),
            Amount.of(Currency.USD, BigDecimal.valueOf(7)));

    final NetworkCommon common = networkCommonAuthorization.networkCommon();
    testHelper.runWithWebhookUser(
        createBusinessRecord.user(), () -> networkMessageService.processNetworkMessage(common));
    firebaseNotificationService.doPushNotification(new PushNotificationEvent(common));
    // This is a declined transaction, but we don't have a device for this user
    Assertions.assertTrue(
        CollectionUtils.isEmpty(FirebaseCloudMessageMockClient.notificationQueue.get()));
    assertThat(networkCommonAuthorization.networkCommon().isPostAdjustment()).isFalse();
    assertThat(networkCommonAuthorization.networkCommon().isPostDecline()).isTrue();

    Assertions.assertTrue(
        CollectionUtils.isEmpty(FirebaseCloudMessageMockClient.notificationQueue.get()));
  }
}
