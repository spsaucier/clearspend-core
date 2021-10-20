package com.tranwall.capital.service;

import com.inversoft.error.Errors;
import com.inversoft.rest.ClientResponse;
import com.tranwall.capital.common.error.InvalidRequestException;
import com.tranwall.capital.common.typedid.data.BusinessId;
import com.tranwall.capital.common.typedid.data.BusinessOwnerId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.common.typedid.data.UserId;
import io.fusionauth.domain.User;
import io.fusionauth.domain.api.UserRequest;
import io.fusionauth.domain.api.UserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class FusionAuthService {

  private final io.fusionauth.client.FusionAuthClient client;

  public String createBusinessOwner(
      TypedId<BusinessId> businessId,
      TypedId<BusinessOwnerId> businessOwnerId,
      String username,
      String password) {
    UserRequest userRequest = new UserRequest();
    User user = new User();
    user.id = businessOwnerId.toUuid();
    user.username = username;
    user.password = password;
    user.data.put("business_id", businessId.toUuid());
    userRequest.user = user;
    ClientResponse<UserResponse, Errors> response =
        client.createUser(businessOwnerId.toUuid(), userRequest);
    if (response.wasSuccessful()) {
      log.info("success: {}", response.successResponse.user);
      return response.successResponse.user.id.toString();
    } else if (response.errorResponse != null) {
      // Error Handling
      Errors errors = response.errorResponse;
      log.error("errors: {}", errors);
      throw new InvalidRequestException(errors.toString());
    } else if (response.exception != null) {
      // Exception Handling
      Exception exception = response.exception;
      log.error("exception", exception);
      throw new RuntimeException(exception);
    } else {
      throw new RuntimeException();
    }
  }

  public String createUser(
      TypedId<BusinessId> businessId, TypedId<UserId> userId, String username, String password) {
    UserRequest userRequest = new UserRequest();
    User user = new User();
    user.id = userId.toUuid();
    user.username = username;
    user.password = password;
    user.data.put("business_id", businessId.toUuid());
    userRequest.user = user;
    ClientResponse<UserResponse, Errors> response = client.createUser(userId.toUuid(), userRequest);
    if (response.wasSuccessful()) {
      log.info("success: {}", response.successResponse.user);
      return response.successResponse.user.id.toString();
    } else if (response.errorResponse != null) {
      // Error Handling
      Errors errors = response.errorResponse;
      log.error("errors: {}", errors);
      throw new InvalidRequestException(errors.toString());
    } else if (response.exception != null) {
      // Exception Handling
      Exception exception = response.exception;
      log.error("exception", exception);
      throw new RuntimeException(exception);
    } else {
      throw new RuntimeException();
    }
  }

  public UserResponse findUser(String email) {
    ClientResponse<UserResponse, Errors> userResponseErrorsClientResponse =
        client.retrieveUserByEmail(email);
    return userResponseErrorsClientResponse.successResponse;
  }
}
