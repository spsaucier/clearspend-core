package com.clearspend.capital.service;

import com.clearspend.capital.client.fusionauth.FusionAuthProperties;
import com.clearspend.capital.common.error.ForbiddenException;
import com.clearspend.capital.common.error.InvalidRequestException;
import com.clearspend.capital.common.masking.annotation.Sensitive;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.common.typedid.data.business.BusinessOwnerId;
import com.clearspend.capital.controller.Common;
import com.clearspend.capital.controller.type.user.ForgotPasswordRequest;
import com.clearspend.capital.controller.type.user.ResetPasswordRequest;
import com.clearspend.capital.data.model.enums.UserType;
import com.google.errorprone.annotations.RestrictedApi;
import com.inversoft.error.Errors;
import com.inversoft.rest.ClientResponse;
import io.fusionauth.domain.Application;
import io.fusionauth.domain.User;
import io.fusionauth.domain.UserRegistration;
import io.fusionauth.domain.api.ApplicationResponse;
import io.fusionauth.domain.api.LoginRequest;
import io.fusionauth.domain.api.LoginResponse;
import io.fusionauth.domain.api.UserRequest;
import io.fusionauth.domain.api.UserResponse;
import io.fusionauth.domain.api.user.ChangePasswordRequest;
import io.fusionauth.domain.api.user.ChangePasswordResponse;
import io.fusionauth.domain.api.user.ForgotPasswordResponse;
import io.fusionauth.domain.api.user.RegistrationRequest;
import io.fusionauth.domain.api.user.RegistrationResponse;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class FusionAuthService {

  private static final String BUSINESS_ID = "businessId";
  private static final String CAPITAL_USER_ID = "capitalUserId";
  private static final String USER_TYPE = "userType";
  private static final String ROLES = "roles";

  private final io.fusionauth.client.FusionAuthClient client;
  private final FusionAuthProperties fusionAuthProperties;

  private final TwilioService twilioService;

  @RestrictedApi(
      explanation = "This should only ever be used by UserService",
      allowedOnPath =
          "/(test/.*)|main/java/com/clearspend/capital/(?:service/UserService.java|controller/nonprod/.*)",
      link = "")
  public UUID createBusinessOwner(
      TypedId<BusinessId> businessId,
      TypedId<BusinessOwnerId> businessOwnerId,
      String username,
      String password) {
    return createUser(
        businessId, businessOwnerId.toUuid(), username, password, UserType.BUSINESS_OWNER);
  }

  @RestrictedApi(
      explanation = "This should only ever be used by UserService",
      allowedOnPath =
          "/(test/.*)|main/java/com/clearspend/capital/(?:service/UserService.java|controller/nonprod/.*)",
      link = "")
  public UUID createUser(
      TypedId<BusinessId> businessId, TypedId<UserId> userId, String email, String password) {
    return createUser(businessId, userId.toUuid(), email, password, UserType.EMPLOYEE);
  }

  private UUID createUser(
      TypedId<BusinessId> businessId,
      UUID userId,
      String email,
      String password,
      UserType userType) {

    UUID fusionAuthId = UUID.randomUUID();

    RegistrationRequest request =
        new RegistrationRequest(
            getUser(email, password, fusionAuthId),
            getUserRegistration(
                businessId, userId, userType, fusionAuthId, Collections.emptySet()));
    validateResponse(client.register(fusionAuthId, request));
    return fusionAuthId;
  }

  private UserRegistration getUserRegistration(
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

  private User getUser(String email, String password, UUID fusionAuthId) {
    User user = new User();
    user.id = fusionAuthId;
    user.email = email;
    user.password = password;
    return user;
  }

  @RestrictedApi(
      explanation = "This should only ever be used by RolesAndPermissionsService",
      allowedOnPath =
          "/(test/.*)|main/java/com/clearspend/capital/service/RolesAndPermissionsService.java",
      link = "")
  public Set<String> getUserRoles(UUID fusionAuthUserId) {
    return getUserRoles(getUser(fusionAuthUserId));
  }

  @SuppressWarnings("unchecked")
  private Set<String> getUserRoles(User user) {
    return Set.copyOf((List<String>) user.data.getOrDefault(ROLES, Collections.emptyList()));
  }

  public enum RoleChange {
    GRANT,
    REVOKE
  }

  /**
   * @param change GRANT or REVOKE
   * @param fusionAuthUserId the FusionAuth user ID to change
   * @param changingRole the role to change
   * @return true if the role is granted, false if it was already there
   */
  @RestrictedApi(
      explanation = "This should only ever be used by RolesAndPermissionsService",
      allowedOnPath =
          "/(test/.*)|main/java/com/clearspend/capital/service/RolesAndPermissionsService.java",
      link = "")
  public boolean changeUserRole(
      @NonNull RoleChange change, @NonNull String fusionAuthUserId, @NonNull String changingRole) {
    User user = getUser(UUID.fromString(fusionAuthUserId));
    Set<String> roles = new HashSet<>(getUserRoles(user));
    if (!(change.equals(RoleChange.GRANT) ? roles.add(changingRole) : roles.remove(changingRole))) {
      return false;
    }
    user.data.put(ROLES, List.copyOf(roles));
    validateResponse(client.updateUser(user.id, new UserRequest(user)));
    return true;
  }

  @RestrictedApi(
      explanation = "This should only ever be used by UserService",
      allowedOnPath = "/(test/.*)|main/java/com/clearspend/capital/service/UserService.java",
      link = "")
  public UUID updateUser(
      TypedId<BusinessId> businessId,
      @NonNull TypedId<UserId> userId,
      String email,
      @Sensitive String password,
      UserType userType,
      String fusionAuthUserIdStr) {
    UUID fusionAuthUserId = UUID.fromString(fusionAuthUserIdStr);

    User user = getUser(email, password, fusionAuthUserId);
    final ClientResponse<UserResponse, Errors> response =
        client.updateUser(fusionAuthUserId, new UserRequest(user));

    validateResponse(response);

    final ClientResponse<RegistrationResponse, Errors> response1 =
        client.updateRegistration(
            fusionAuthUserId,
            new RegistrationRequest(
                user,
                getUserRegistration(
                    businessId,
                    userId.toUuid(),
                    userType,
                    fusionAuthUserId,
                    getUserRoles(fusionAuthUserId))));

    validateResponse(response1);

    return fusionAuthUserId;
  }

  private <T> T validateResponse(ClientResponse<T, Errors> response) {
    if (response.wasSuccessful()) {
      return response.successResponse;
    }

    if (response.errorResponse != null) {
      Errors errors = response.errorResponse;
      throw new InvalidRequestException(errors.toString());
    }

    if (response.exception != null) {
      Exception exception = response.exception;
      throw new RuntimeException(exception);
    }

    throw new RuntimeException("shouldn't have gotten here");
  }

  public User retrieveUserByEmail(String email) {
    ClientResponse<UserResponse, Errors> userResponseErrorsClientResponse =
        client.retrieveUserByEmail(email);
    return validateResponse(userResponseErrorsClientResponse).user;
  }

  public User getUser(UUID fusionAuthUserId) {
    ClientResponse<UserResponse, Errors> user = client.retrieveUser(fusionAuthUserId);
    return validateResponse(user).user;
  }

  public void forgotPassword(ForgotPasswordRequest request) {
    io.fusionauth.domain.api.user.ForgotPasswordRequest fusionAuthRequest =
        new io.fusionauth.domain.api.user.ForgotPasswordRequest();
    fusionAuthRequest.loginId = request.getEmail();
    fusionAuthRequest.sendForgotPasswordEmail = false;
    fusionAuthRequest.applicationId = UUID.fromString(fusionAuthProperties.getApplicationId());
    ClientResponse<ForgotPasswordResponse, Errors> forgotPasswordResponse =
        client.forgotPassword(fusionAuthRequest);

    // here are the response statuses:
    // https://fusionauth.io/docs/v1/tech/apis/users/#start-forgot-password-workflow
    switch (forgotPasswordResponse.status) {
      case 200 -> twilioService.sendResetPasswordEmail(
          request.getEmail(), forgotPasswordResponse.successResponse.changePasswordId);
      case 403 -> throw new RuntimeException(
          "FusionAuth password reset feature is disabled. "
              + "See Tenant > Email section to set Forgot Password template "
              + "and Customizations -> Email Templates section to create a dummy template");
      case 404 -> {
        // user cannot be found
      }
      case 422 -> throw new RuntimeException(
          "Email for this user is not set: " + forgotPasswordResponse.status);
      case 500 -> throw new RuntimeException(
          "FusionAuth internal error", forgotPasswordResponse.exception);
      default -> throw new RuntimeException(
          "unknown reset password status: " + forgotPasswordResponse.status);
    }
  }

  public void resetPassword(ResetPasswordRequest request) {
    ClientResponse<ChangePasswordResponse, Errors> changePasswordResponse =
        client.changePassword(
            request.getChangePasswordId(), new ChangePasswordRequest(request.getNewPassword()));

    switch (changePasswordResponse.status) {
      case 200 -> {}
      case 404 -> throw new ForbiddenException();
      case 500 -> throw new RuntimeException(
          "FusionAuth internal error", changePasswordResponse.exception);
      default -> throw new RuntimeException(
          "unknown reset password status: " + changePasswordResponse.status);
    }
  }

  public ClientResponse<LoginResponse, Errors> login(String loginId, @Sensitive String password) {
    LoginRequest request = new LoginRequest(getApplicationId(), loginId, password);
    return client.login(request);
  }

  public UUID getApplicationId() {
    return UUID.fromString(fusionAuthProperties.getApplicationId());
  }

  public Application getApplication() {
    ClientResponse<ApplicationResponse, Void> response =
        client.retrieveApplication(getApplicationId());
    if (response.wasSuccessful()) {
      return response.successResponse.application;
    }
    throw new InvalidRequestException(String.valueOf(response.status), response.exception);
  }
}
