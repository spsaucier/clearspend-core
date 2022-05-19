package com.clearspend.capital.service;

import com.clearspend.capital.client.fusionauth.FusionAuthProperties;
import com.clearspend.capital.common.error.FusionAuthException;
import com.clearspend.capital.common.error.InvalidRequestException;
import com.clearspend.capital.common.masking.annotation.Sensitive;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.common.typedid.data.business.BusinessOwnerId;
import com.clearspend.capital.controller.Common;
import com.clearspend.capital.data.model.enums.UserType;
import com.clearspend.capital.data.repository.UserRepository;
import com.clearspend.capital.service.FusionAuthService.CapitalChangePasswordReason;
import com.clearspend.capital.service.FusionAuthService.RoleChange;
import com.inversoft.error.Errors;
import com.inversoft.rest.ClientResponse;
import io.fusionauth.client.FusionAuthClient;
import io.fusionauth.domain.Application;
import io.fusionauth.domain.ChangePasswordReason;
import io.fusionauth.domain.User;
import io.fusionauth.domain.UserRegistration;
import io.fusionauth.domain.api.ApplicationResponse;
import io.fusionauth.domain.api.UserRequest;
import io.fusionauth.domain.api.UserResponse;
import io.fusionauth.domain.api.user.RegistrationRequest;
import io.fusionauth.domain.api.user.RegistrationResponse;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * This service provides core functionality which must be available for other key services. {@link
 * RolesAndPermissionsService}, {@link UserService}, and {@link BusinessProspectService} all depend
 * on this service, so it is not possible to annotate its methods for security purposes. All methods
 * on this class are to be package protected or more secure.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CoreFusionAuthService {
  private static final String BUSINESS_ID = "businessId";
  private static final String USER_TYPE = "userType";
  private static final String ROLES = "roles";

  private final FusionAuthClient client;
  private final FusionAuthProperties fusionAuthProperties;
  private final UserRepository userRepository;

  UUID createBusinessOwner(
      TypedId<BusinessId> businessId,
      TypedId<BusinessOwnerId> businessOwnerId,
      String username,
      String password) {
    return createUser(
        businessId,
        new TypedId<UserId>(businessOwnerId.toUuid()),
        username,
        password,
        UserType.BUSINESS_OWNER,
        Optional.empty());
  }

  UUID createUser(
      TypedId<BusinessId> businessId, TypedId<UserId> userId, String email, String password) {
    return createUser(businessId, userId, email, password, UserType.EMPLOYEE, Optional.empty());
  }

  UUID createUser(
      TypedId<BusinessId> businessId,
      TypedId<UserId> userId,
      String email,
      String password,
      UserType userType,
      Optional<CapitalChangePasswordReason> changePasswordReason) {

    UUID fusionAuthId = UUID.randomUUID();

    RegistrationRequest request =
        new RegistrationRequest(
            userFactory(
                email,
                password,
                fusionAuthId,
                changePasswordReason.map(CapitalChangePasswordReason::getFusionAuthReason)),
            userRegistrationFactory(
                businessId, userId.toUuid(), userType, fusionAuthId, Collections.emptySet()));
    validateResponse(client.register(fusionAuthId, request));
    return fusionAuthId;
  }

  Set<String> getUserRoles(TypedId<UserId> userId) {
    final UUID fusionAuthUserId =
        UUID.fromString(userRepository.findById(userId).orElseThrow().getSubjectRef());
    return getUserRoles(fusionAuthUserId);
  }

  Set<String> getUserRoles(UUID fusionAuthUserId) {
    return getUserRoles(getUser(fusionAuthUserId));
  }

  @SuppressWarnings("unchecked")
  Set<String> getUserRoles(User user) {
    return Set.copyOf((List<String>) user.data.getOrDefault(ROLES, Collections.emptyList()));
  }

  /**
   * Change a user's global roles
   *
   * @param change Either GRANT or REVOKE.
   * @param fusionAuthUserId the FusionAuth user ID to change.
   * @param changingRole the role to change.
   * @return true if the role is granted, false if it was already there
   */
  boolean changeUserRole(
      @NonNull FusionAuthService.RoleChange change,
      @NonNull String fusionAuthUserId,
      @NonNull String changingRole) {
    User user = getUser(UUID.fromString(fusionAuthUserId));
    Set<String> roles = new HashSet<String>(getUserRoles(user));
    if (!(change.equals(RoleChange.GRANT) ? roles.add(changingRole) : roles.remove(changingRole))) {
      return false;
    }
    user.data.put(ROLES, List.copyOf(roles));
    validateResponse(client.updateUser(user.id, new UserRequest(user)));
    return true;
  }

  UUID updateUser(com.clearspend.capital.data.model.User user, @Nullable String password) {
    return updateUser(
        user.getBusinessId(),
        user.getId(),
        user.getEmail().getEncrypted(),
        password,
        user.getType(),
        user.getSubjectRef());
  }

  /**
   * @param businessId the user's business
   * @param userId capital's number
   * @param email null for no change, correct value will be persisted
   * @param password null for no change, correct value will be persisted
   * @param userType the compiler will tell you if this is wrong.
   * @param fusionAuthUserIdStr fusionAuth's UUID for the user (subjectRef)
   * @return the FusionAuth UUID for the user
   */
  UUID updateUser(
      TypedId<BusinessId> businessId,
      @NonNull TypedId<UserId> userId,
      @Sensitive String email,
      @Sensitive String password,
      UserType userType,
      @NonNull String fusionAuthUserIdStr) {
    UUID fusionAuthUserId = UUID.fromString(fusionAuthUserIdStr);

    User user = userFactory(email, password, fusionAuthUserId);
    if (email != null || password != null) {

      final ClientResponse<UserResponse, Errors> response =
          client.updateUser(fusionAuthUserId, new UserRequest(user));

      validateResponse(response);
    }

    final ClientResponse<RegistrationResponse, Errors> response1 =
        client.updateRegistration(
            fusionAuthUserId,
            new RegistrationRequest(
                user,
                userRegistrationFactory(
                    businessId,
                    userId.toUuid(),
                    userType,
                    fusionAuthUserId,
                    getUserRoles(fusionAuthUserId))));

    validateResponse(response1);

    return fusionAuthUserId;
  }

  static <T> T validateResponse(ClientResponse<T, Errors> response) {
    if (response.wasSuccessful()) {
      return response.successResponse;
    }

    throw new FusionAuthException(response.status, response.errorResponse, response.exception);
  }

  private UserRegistration userRegistrationFactory(
      TypedId<BusinessId> businessId,
      UUID userId,
      UserType userType,
      UUID fusionAuthId,
      Set<String> roles) {
    UserRegistration registration = new UserRegistration();
    registration.data.put(Common.CAPITAL_USER_ID, userId.toString());
    registration.data.put(BUSINESS_ID, businessId.toUuid());
    registration.data.put(USER_TYPE, userType.name());
    registration.data.put(ROLES, roles.toArray(String[]::new));
    registration.applicationId = getApplicationId();
    registration.id = fusionAuthId;
    return registration;
  }

  private static User userFactory(String email, String password, UUID fusionAuthId) {
    return userFactory(email, password, fusionAuthId, Optional.empty());
  }

  private static User userFactory(
      String email,
      String password,
      @NonNull UUID fusionAuthId,
      Optional<ChangePasswordReason> changePasswordReason) {
    User user = new User();

    user.id = fusionAuthId;

    Optional.ofNullable(email).ifPresent(s -> user.email = s);
    Optional.ofNullable(password).ifPresent(s -> user.password = s);

    changePasswordReason.ifPresent(
        r -> {
          user.passwordChangeRequired = true;
          user.passwordChangeReason = r;
        });

    return user;
  }

  UUID getApplicationId() {
    return UUID.fromString(fusionAuthProperties.getApplicationId());
  }

  Application getApplication() {
    ClientResponse<ApplicationResponse, Void> response =
        client.retrieveApplication(getApplicationId());
    if (response.wasSuccessful()) {
      return response.successResponse.application;
    }
    throw new InvalidRequestException(String.valueOf(response.status), response.exception);
  }

  User getUser(@NonNull UUID fusionAuthUserId) {
    ClientResponse<UserResponse, Errors> user = client.retrieveUser(fusionAuthUserId);
    return CoreFusionAuthService.validateResponse(user).user;
  }

  void deleteUser(com.clearspend.capital.data.model.User user) {
    final ClientResponse<Void, Errors> response =
        client.deleteUser(UUID.fromString(user.getSubjectRef()));
    validateResponse(response);
  }
}
