package com.tranwall.capital.service;

import com.inversoft.error.Errors;
import com.inversoft.rest.ClientResponse;
import com.tranwall.capital.common.error.InvalidRequestException;
import com.tranwall.capital.common.typedid.data.BusinessId;
import com.tranwall.capital.common.typedid.data.BusinessOwnerId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.common.typedid.data.UserId;
import com.tranwall.capital.data.model.enums.UserType;
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

  private static final String BUSINESS_ID = "businessId";
  private static final String CAPITAL_USER_ID = "capitalUserId";
  private static final String USER_TYPE = "userType";

  private final io.fusionauth.client.FusionAuthClient client;

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

  private UUID create(
      TypedId<BusinessId> businessId,
      @NonNull UUID userId,
      String username,
      String password,
      UserType userType) {
    User user = new User();
    user.username = username;
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

  public UserResponse retrieveUserByUsername(String email) {
    ClientResponse<UserResponse, Errors> userResponseErrorsClientResponse =
        client.retrieveUserByUsername(email);
    return userResponseErrorsClientResponse.successResponse;
  }
}
