package com.clearspend.capital.service;

import com.clearspend.capital.client.fusionauth.FusionAuthProperties;
import com.clearspend.capital.common.error.ForbiddenException;
import com.clearspend.capital.common.error.InvalidRequestException;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.common.typedid.data.business.BusinessOwnerId;
import com.clearspend.capital.controller.type.user.ForgotPasswordRequest;
import com.clearspend.capital.controller.type.user.ResetPasswordRequest;
import com.clearspend.capital.data.model.enums.UserType;
import com.google.errorprone.annotations.RestrictedApi;
import com.inversoft.error.Errors;
import com.inversoft.rest.ClientResponse;
import io.fusionauth.domain.User;
import io.fusionauth.domain.api.UserRequest;
import io.fusionauth.domain.api.UserResponse;
import io.fusionauth.domain.api.user.ChangePasswordRequest;
import io.fusionauth.domain.api.user.ChangePasswordResponse;
import io.fusionauth.domain.api.user.ForgotPasswordResponse;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Consumer;
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
    return create(
        businessId,
        businessOwnerId.toUuid(),
        username,
        password,
        UserType.BUSINESS_OWNER,
        Collections.emptySet());
  }

  @RestrictedApi(
      explanation = "This should only ever be used by UserService",
      allowedOnPath =
          "/(test/.*)|main/java/com/clearspend/capital/(?:service/UserService.java|controller/nonprod/.*)",
      link = "")
  public UUID createUser(
      TypedId<BusinessId> businessId, TypedId<UserId> userId, String username, String password) {
    return create(
        businessId, userId.toUuid(), username, password, UserType.EMPLOYEE, Collections.emptySet());
  }

  @RestrictedApi(
      explanation = "This should only ever be used by UserService",
      allowedOnPath = "/(test/.*)|main/java/com/clearspend/capital/service/UserService.java",
      link = "")
  public UUID updateUser(
      TypedId<BusinessId> businessId,
      TypedId<UserId> userId,
      String username,
      String password,
      UserType userType,
      String fusionAuthUUID) {
    final UUID fusionAuthId = UUID.fromString(fusionAuthUUID);
    return createOrUpdateUser(
        client::updateUser,
        businessId,
        userId.toUuid(),
        (u) -> u.username = username,
        password,
        userType,
        getUserRoles(fusionAuthId),
        fusionAuthId);
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
    return Set.copyOf((List<String>) user.data.get(ROLES));
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

  private UUID create(
      TypedId<BusinessId> businessId,
      @NonNull UUID userId,
      String email,
      String password,
      UserType userType,
      Set<String> roles) {
    return createOrUpdateUser(
        client::createUser,
        businessId,
        userId,
        (u) -> u.email = email,
        password,
        userType,
        roles,
        UUID.randomUUID());
  }

  private UUID createOrUpdateUser(
      BiFunction<UUID, UserRequest, ClientResponse<UserResponse, Errors>> action,
      TypedId<BusinessId> businessId,
      @NonNull UUID userId,
      Consumer<User> setIdentifier,
      String password,
      UserType userType,
      Set<String> roles,
      UUID fusionAuthUserId) {

    User user = new User();
    setIdentifier.accept(user);
    user.password = password;
    user.data.put(BUSINESS_ID, businessId.toUuid());
    user.data.put(CAPITAL_USER_ID, userId);
    user.data.put(USER_TYPE, userType.name());
    user.data.put(ROLES, roles.toArray(String[]::new));

    final ClientResponse<UserResponse, Errors> response =
        action.apply(fusionAuthUserId, new UserRequest(user));

    validateResponse(response);
    return fusionAuthUserId;
  }

  private UserResponse validateResponse(ClientResponse<UserResponse, Errors> response) {
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
}
