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
import java.util.UUID;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class FusionAuthService {

  public static final String BUSINESS_ID_KEY = "businessId";

  private final io.fusionauth.client.FusionAuthClient client;

  public UUID createBusinessOwner(
      TypedId<BusinessId> businessId,
      TypedId<BusinessOwnerId> businessOwnerId,
      String username,
      String password) {
    return create(businessId, businessOwnerId.toUuid(), username, password);
  }

  public UUID createUser(
      TypedId<BusinessId> businessId, TypedId<UserId> userId, String username, String password) {
    return create(businessId, userId.toUuid(), username, password);
  }

  private UUID create(
      TypedId<BusinessId> businessId, @NonNull UUID userId, String username, String password) {
    User user = new User();
    user.username = username;
    user.password = password;
    user.data.put(BUSINESS_ID_KEY, businessId.toUuid());

    ClientResponse<UserResponse, Errors> response =
        client.createUser(userId, new UserRequest(user));

    if (response.wasSuccessful()) {
      return response.successResponse.user.id;
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

  public UserResponse retrieveUserByUsername(String email) {
    ClientResponse<UserResponse, Errors> userResponseErrorsClientResponse =
        client.retrieveUserByUsername(email);
    return userResponseErrorsClientResponse.successResponse;
  }
}
