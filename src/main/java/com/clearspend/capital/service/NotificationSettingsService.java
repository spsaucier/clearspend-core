package com.clearspend.capital.service;

import com.clearspend.capital.common.data.dao.UserRolesAndPermissions;
import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.controller.type.notification.AllocationNotificationSettingsResponse;
import com.clearspend.capital.controller.type.notification.AllocationNotificationSettingsResponse.AllocationNotificationRecipient;
import com.clearspend.capital.controller.type.notification.AllocationNotificationsSettingRequest;
import com.clearspend.capital.crypto.data.model.embedded.RequiredEncryptedStringWithHash;
import com.clearspend.capital.data.model.Allocation;
import com.clearspend.capital.data.model.User;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.data.model.notifications.AllocationNotificationsSettings;
import com.clearspend.capital.data.model.security.DefaultRoles;
import com.clearspend.capital.data.repository.AllocationNotificationSettingRepository;
import com.clearspend.capital.data.repository.UserRepository;
import com.clearspend.capital.data.repository.security.UserAllocationRoleRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationSettingsService {
  private static final Set<String> MANAGER_AND_ADMIN =
      Set.of(DefaultRoles.ALLOCATION_MANAGER, DefaultRoles.ALLOCATION_ADMIN);
  private final AllocationNotificationSettingRepository allocationNotificationSettingRepository;
  private final UserAllocationRoleRepository userAllocationRoleRepository;
  private final UserRepository userRepository;
  private final RetrievalService retrievalService;

  @Transactional
  @PreAuthorize("hasAllocationPermission(#allocationId, 'MANAGE_FUNDS|CUSTOMER_SERVICE')")
  public AllocationNotificationSettingsResponse getAllocationNotificationSetting(
      final TypedId<AllocationId> allocationId) {
    final Allocation allocation = retrievalService.retrieveAllocation(allocationId);
    final AllocationNotificationsSettings settings =
        findAllocationNotificationSettingsOrDefault(allocationId);
    final Map<TypedId<UserId>, UserRolesAndPermissions> usersAtAllocation =
        userAllocationRoleRepository.getActiveUsersWithAllocationPermission(
            allocation.getBusinessId(), allocationId);
    final List<User> users = userRepository.findAllByIdIn(usersAtAllocation.keySet());

    final Set<AllocationNotificationRecipient> recipients =
        usersAtAllocation.values().stream()
            .filter(permissions -> MANAGER_AND_ADMIN.contains(permissions.allocationRole()))
            .map(
                permission ->
                    new AllocationNotificationRecipient(
                        permission.userId(),
                        permission.firstName(),
                        permission.lastName(),
                        findEmail(users, permission.userId()),
                        permission.allocationRole(),
                        settings.getRecipients().contains(permission.userId())))
            .collect(Collectors.toSet());

    return new AllocationNotificationSettingsResponse(
        allocationId,
        settings.isLowBalance(),
        com.clearspend.capital.controller.type.Amount.of(settings.getLowBalanceLevel()),
        recipients);
  }

  private String findEmail(final List<User> users, final TypedId<UserId> userId) {
    return users.stream()
        .filter(user -> user.getId().equals(userId))
        .findFirst()
        .map(User::getEmail)
        .map(RequiredEncryptedStringWithHash::getEncrypted)
        .orElse(null);
  }

  private AllocationNotificationsSettings findAllocationNotificationSettingsOrDefault(
      final TypedId<AllocationId> allocationId) {
    return allocationNotificationSettingRepository
        .findByAllocationId(allocationId)
        .orElse(
            new AllocationNotificationsSettings(
                allocationId, false, Amount.of(Currency.USD, new BigDecimal("0")), Set.of()));
  }

  @Transactional
  @PreAuthorize("hasAllocationPermission(#allocationId, 'MANAGE_FUNDS|CUSTOMER_SERVICE')")
  public AllocationNotificationSettingsResponse updateAllocationNotificationSetting(
      final TypedId<AllocationId> allocationId,
      final AllocationNotificationsSettingRequest request) {
    final AllocationNotificationsSettings settings =
        new AllocationNotificationsSettings(
            request.allocationId(),
            request.lowBalance(),
            request.lowBalanceLevel().toAmount(),
            request.recipients());
    allocationNotificationSettingRepository.deleteByAllocationId(allocationId);
    allocationNotificationSettingRepository.flush();
    allocationNotificationSettingRepository.saveAndFlush(settings);
    return getAllocationNotificationSetting(allocationId);
  }
}
