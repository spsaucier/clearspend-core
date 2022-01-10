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
import com.inversoft.error.Errors;
import com.inversoft.rest.ClientResponse;
import io.fusionauth.domain.User;
import io.fusionauth.domain.api.UserRequest;
import io.fusionauth.domain.api.UserResponse;
import io.fusionauth.domain.api.user.ChangePasswordRequest;
import io.fusionauth.domain.api.user.ChangePasswordResponse;
import io.fusionauth.domain.api.user.ForgotPasswordResponse;
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

  private final io.fusionauth.client.FusionAuthClient client;
  private final FusionAuthProperties fusionAuthProperties;

  private final TwilioService twilioService;

  public UUID createBusinessOwner(
      TypedId<BusinessId> businessId,
      TypedId<BusinessOwnerId> businessOwnerId,
      String username,
      String password) {
    return create(
        businessId, businessOwnerId.toUuid(), username, password, UserType.BUSINESS_OWNER);
  }

  public UUID createUser(
      TypedId<BusinessId> businessId, TypedId<UserId> userId, String username, String password) {
    return create(businessId, userId.toUuid(), username, password, UserType.EMPLOYEE);
  }

  public UUID updateUser(
      TypedId<BusinessId> businessId,
      TypedId<UserId> userId,
      String username,
      String password,
      UserType userType,
      String fusionAuth) {
    return update(
        businessId, userId.toUuid(), username, password, userType, UUID.fromString(fusionAuth));
  }

  private UUID create(
      TypedId<BusinessId> businessId,
      @NonNull UUID userId,
      String email,
      String password,
      UserType userType) {
    User user = new User();
    user.email = email;
    user.password = password;
    user.data.put(BUSINESS_ID, businessId.toUuid());
    user.data.put(CAPITAL_USER_ID, userId);
    user.data.put(USER_TYPE, userType.name());
    UUID fusionAuthUserId = UUID.randomUUID();

    ClientResponse<UserResponse, Errors> response =
        client.createUser(fusionAuthUserId, new UserRequest(user));

    if (response.wasSuccessful()) {
      return fusionAuthUserId;
    }

    if (response.errorResponse != null) {
      Errors errors = response.errorResponse;
      throw new InvalidRequestException(errors.toString());
    }

    if (response.exception != null) {
      Exception exception = response.exception;
      throw new RuntimeException(exception);
    }

    throw new RuntimeException("shouldn't have got here");
  }

  private UUID update(
      TypedId<BusinessId> businessId,
      @NonNull UUID userId,
      String username,
      String password,
      UserType userType,
      UUID fusionAuthUserId) {
    User user = new User();
    user.username = username;
    user.password = password;
    user.data.put(BUSINESS_ID, businessId.toUuid());
    user.data.put(CAPITAL_USER_ID, userId);
    user.data.put(USER_TYPE, userType.name());

    ClientResponse<UserResponse, Errors> response =
        client.updateUser(fusionAuthUserId, new UserRequest(user));

    if (response.wasSuccessful()) {
      return fusionAuthUserId;
    }

    if (response.errorResponse != null) {
      Errors errors = response.errorResponse;
      throw new InvalidRequestException(errors.toString());
    }

    if (response.exception != null) {
      Exception exception = response.exception;
      throw new RuntimeException(exception);
    }
    throw new RuntimeException("shouldn't have got here");
  }

  public UserResponse retrieveUserByEmail(String email) {
    ClientResponse<UserResponse, Errors> userResponseErrorsClientResponse =
        client.retrieveUserByEmail(email);
    return userResponseErrorsClientResponse.successResponse;
  }

  public void forgotPassword(ForgotPasswordRequest request) {
    io.fusionauth.domain.api.user.ForgotPasswordRequest fusionAuthRequest =
        new io.fusionauth.domain.api.user.ForgotPasswordRequest();
    fusionAuthRequest.loginId = request.getEmail();
    fusionAuthRequest.sendForgotPasswordEmail = false;
    fusionAuthRequest.applicationId = UUID.fromString(fusionAuthProperties.getApplicationId());
    log.debug("FusionAuthRequest for forgot password {} ", fusionAuthRequest);
    ClientResponse<ForgotPasswordResponse, Errors> forgotPasswordResponse =
        client.forgotPassword(fusionAuthRequest);
    log.debug("FusionAuthResponse for forgot password {} ", forgotPasswordResponse);
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
