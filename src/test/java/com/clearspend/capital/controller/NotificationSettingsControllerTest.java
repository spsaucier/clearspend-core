package com.clearspend.capital.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.MockMvcHelper;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.TestHelper.CreateBusinessRecord;
import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.controller.type.notification.AllocationNotificationSettingsResponse;
import com.clearspend.capital.controller.type.notification.AllocationNotificationSettingsResponse.AllocationNotificationRecipient;
import com.clearspend.capital.controller.type.notification.AllocationNotificationsSettingRequest;
import com.clearspend.capital.data.model.User;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.data.model.notifications.AllocationNotificationsSettings;
import com.clearspend.capital.data.model.security.DefaultRoles;
import com.clearspend.capital.data.repository.AllocationNotificationSettingRepository;
import com.clearspend.capital.testutils.permission.PermissionValidationHelper;
import com.clearspend.capital.util.function.ThrowableFunctions.ThrowingFunction;
import java.math.BigDecimal;
import java.util.Set;
import javax.servlet.http.Cookie;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.ResultActions;

@RequiredArgsConstructor(onConstructor = @__({@Autowired}))
public class NotificationSettingsControllerTest extends BaseCapitalTest {
  private final MockMvcHelper mockMvcHelper;
  private final TestHelper testHelper;
  private final PermissionValidationHelper permissionValidationHelper;
  private CreateBusinessRecord createBusinessRecord;
  private User otherManager;
  private final AllocationNotificationSettingRepository allocationNotificationSettingRepository;

  @BeforeEach
  void setup() {
    createBusinessRecord = testHelper.createBusiness();
    testHelper.setCurrentUser(createBusinessRecord.user());
    otherManager =
        testHelper
            .createUserWithRole(
                createBusinessRecord.allocationRecord().allocation(),
                DefaultRoles.ALLOCATION_MANAGER)
            .user();
  }

  @Test
  void getAllocationNotificationSettings() {
    final AllocationNotificationsSettings setting =
        new AllocationNotificationsSettings(
            createBusinessRecord.allocationRecord().allocation().getId(),
            true,
            entityAmount(Currency.USD, new BigDecimal("10")),
            Set.of(createBusinessRecord.user().getId()));
    allocationNotificationSettingRepository.saveAndFlush(setting);
    final AllocationNotificationSettingsResponse response =
        mockMvcHelper.queryObject(
            "/notification-settings/allocations/%s"
                .formatted(createBusinessRecord.allocationRecord().allocation().getId()),
            HttpMethod.GET,
            createBusinessRecord.authCookie(),
            AllocationNotificationSettingsResponse.class);
    assertThat(response)
        .hasFieldOrPropertyWithValue(
            "allocationId", createBusinessRecord.allocationRecord().allocation().getId())
        .hasFieldOrPropertyWithValue("lowBalance", true)
        .hasFieldOrPropertyWithValue(
            "lowBalanceLevel", dtoAmount(Currency.USD, new BigDecimal("10")));
    assertThat(response.recipients())
        .contains(
            new AllocationNotificationRecipient(
                createBusinessRecord.user().getId(),
                createBusinessRecord.user().getFirstName().getEncrypted(),
                createBusinessRecord.user().getLastName().getEncrypted(),
                createBusinessRecord.user().getEmail().getEncrypted(),
                DefaultRoles.ALLOCATION_ADMIN,
                true),
            new AllocationNotificationRecipient(
                otherManager.getId(),
                otherManager.getFirstName().getEncrypted(),
                otherManager.getLastName().getEncrypted(),
                otherManager.getEmail().getEncrypted(),
                DefaultRoles.ALLOCATION_MANAGER,
                false));
  }

  @Test
  void getAllocationNotificationSettings_UserPermissions() {
    final ThrowingFunction<Cookie, ResultActions> action =
        cookie ->
            mockMvcHelper.query(
                "/notification-settings/allocations/%s"
                    .formatted(createBusinessRecord.allocationRecord().allocation().getId()),
                HttpMethod.GET,
                cookie);
    permissionValidationHelper
        .buildValidator(createBusinessRecord)
        .allowRolesOnAllocation(
            Set.of(DefaultRoles.ALLOCATION_MANAGER, DefaultRoles.ALLOCATION_ADMIN))
        .allowGlobalRoles(
            Set.of(
                DefaultRoles.GLOBAL_CUSTOMER_SERVICE, DefaultRoles.GLOBAL_CUSTOMER_SERVICE_MANAGER))
        .build()
        .validateMockMvcCall(action);
  }

  private Amount entityAmount(final Currency currency, final BigDecimal amount) {
    return Amount.of(Currency.USD, amount);
  }

  private com.clearspend.capital.controller.type.Amount dtoAmount(
      final Currency currency, final BigDecimal amount) {
    return com.clearspend.capital.controller.type.Amount.of(entityAmount(currency, amount));
  }

  @Test
  void getAllocationNotificationSettings_RecipientIsNoLongerManager() {
    final User employee =
        testHelper
            .createUserWithRole(
                createBusinessRecord.allocationRecord().allocation(),
                DefaultRoles.ALLOCATION_EMPLOYEE)
            .user();
    final AllocationNotificationsSettings setting =
        new AllocationNotificationsSettings(
            createBusinessRecord.allocationRecord().allocation().getId(),
            true,
            entityAmount(Currency.USD, new BigDecimal("10")),
            Set.of(createBusinessRecord.user().getId(), employee.getId()));
    allocationNotificationSettingRepository.saveAndFlush(setting);
    final AllocationNotificationSettingsResponse response =
        mockMvcHelper.queryObject(
            "/notification-settings/allocations/%s"
                .formatted(createBusinessRecord.allocationRecord().allocation().getId()),
            HttpMethod.GET,
            createBusinessRecord.authCookie(),
            AllocationNotificationSettingsResponse.class);
    assertThat(response)
        .hasFieldOrPropertyWithValue(
            "allocationId", createBusinessRecord.allocationRecord().allocation().getId())
        .hasFieldOrPropertyWithValue("lowBalance", true)
        .hasFieldOrPropertyWithValue(
            "lowBalanceLevel", dtoAmount(Currency.USD, new BigDecimal("10")));
    assertThat(response.recipients())
        .contains(
            new AllocationNotificationRecipient(
                createBusinessRecord.user().getId(),
                createBusinessRecord.user().getFirstName().getEncrypted(),
                createBusinessRecord.user().getLastName().getEncrypted(),
                createBusinessRecord.user().getEmail().getEncrypted(),
                DefaultRoles.ALLOCATION_ADMIN,
                true),
            new AllocationNotificationRecipient(
                otherManager.getId(),
                otherManager.getFirstName().getEncrypted(),
                otherManager.getLastName().getEncrypted(),
                otherManager.getEmail().getEncrypted(),
                DefaultRoles.ALLOCATION_MANAGER,
                false));
  }

  @Test
  void getAllocationNotificationSettings_Defaults() {
    final AllocationNotificationSettingsResponse response =
        mockMvcHelper.queryObject(
            "/notification-settings/allocations/%s"
                .formatted(createBusinessRecord.allocationRecord().allocation().getId()),
            HttpMethod.GET,
            createBusinessRecord.authCookie(),
            AllocationNotificationSettingsResponse.class);
    assertThat(response)
        .hasFieldOrPropertyWithValue(
            "allocationId", createBusinessRecord.allocationRecord().allocation().getId())
        .hasFieldOrPropertyWithValue("lowBalance", false)
        .hasFieldOrPropertyWithValue(
            "lowBalanceLevel", dtoAmount(Currency.USD, new BigDecimal("0")));
    assertThat(response.recipients())
        .contains(
            new AllocationNotificationRecipient(
                createBusinessRecord.user().getId(),
                createBusinessRecord.user().getFirstName().getEncrypted(),
                createBusinessRecord.user().getLastName().getEncrypted(),
                createBusinessRecord.user().getEmail().getEncrypted(),
                DefaultRoles.ALLOCATION_ADMIN,
                false),
            new AllocationNotificationRecipient(
                otherManager.getId(),
                otherManager.getFirstName().getEncrypted(),
                otherManager.getLastName().getEncrypted(),
                otherManager.getEmail().getEncrypted(),
                DefaultRoles.ALLOCATION_MANAGER,
                false));
  }

  @Test
  void updateAllocationNotificationSettings() {
    final AllocationNotificationsSettingRequest request =
        new AllocationNotificationsSettingRequest(
            createBusinessRecord.allocationRecord().allocation().getId(),
            true,
            dtoAmount(Currency.USD, new BigDecimal("10")),
            Set.of(createBusinessRecord.user().getId()));
    final AllocationNotificationSettingsResponse response =
        mockMvcHelper.queryObject(
            "/notification-settings/allocations/%s"
                .formatted(createBusinessRecord.allocationRecord().allocation().getId()),
            HttpMethod.POST,
            createBusinessRecord.authCookie(),
            request,
            AllocationNotificationSettingsResponse.class);
    assertThat(response)
        .hasFieldOrPropertyWithValue(
            "allocationId", createBusinessRecord.allocationRecord().allocation().getId())
        .hasFieldOrPropertyWithValue("lowBalance", true)
        .hasFieldOrPropertyWithValue(
            "lowBalanceLevel", dtoAmount(Currency.USD, new BigDecimal("10")));
    assertThat(response.recipients())
        .contains(
            new AllocationNotificationRecipient(
                createBusinessRecord.user().getId(),
                createBusinessRecord.user().getFirstName().getEncrypted(),
                createBusinessRecord.user().getLastName().getEncrypted(),
                createBusinessRecord.user().getEmail().getEncrypted(),
                DefaultRoles.ALLOCATION_ADMIN,
                true),
            new AllocationNotificationRecipient(
                otherManager.getId(),
                otherManager.getFirstName().getEncrypted(),
                otherManager.getLastName().getEncrypted(),
                otherManager.getEmail().getEncrypted(),
                DefaultRoles.ALLOCATION_MANAGER,
                false));
  }

  @Test
  void updateAllocationNotificationSettings_UserPermissions() {
    final AllocationNotificationsSettingRequest request =
        new AllocationNotificationsSettingRequest(
            createBusinessRecord.allocationRecord().allocation().getId(),
            true,
            dtoAmount(Currency.USD, new BigDecimal("10")),
            Set.of(createBusinessRecord.user().getId()));
    final ThrowingFunction<Cookie, ResultActions> action =
        cookie ->
            mockMvcHelper.query(
                "/notification-settings/allocations/%s"
                    .formatted(createBusinessRecord.allocationRecord().allocation().getId()),
                HttpMethod.POST,
                cookie,
                request);
    permissionValidationHelper
        .buildValidator(createBusinessRecord)
        .allowRolesOnAllocation(
            Set.of(DefaultRoles.ALLOCATION_MANAGER, DefaultRoles.ALLOCATION_ADMIN))
        .allowGlobalRoles(
            Set.of(
                DefaultRoles.GLOBAL_CUSTOMER_SERVICE, DefaultRoles.GLOBAL_CUSTOMER_SERVICE_MANAGER))
        .build()
        .validateMockMvcCall(action);
  }
}
