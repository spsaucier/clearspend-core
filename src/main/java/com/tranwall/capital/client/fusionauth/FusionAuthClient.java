package com.tranwall.capital.client.fusionauth;

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
public class FusionAuthClient {

  private static final String BUSINESS_ID_KEY = "businessId";

  private final io.fusionauth.client.FusionAuthClient client;

  public String createBusinessOwner(
      TypedId<BusinessId> businessId,
      TypedId<BusinessOwnerId> businessOwnerId,
      String username,
      String password) {
    return create(businessId, businessOwnerId.toUuid(), username, password);
  }

  public String createUser(
      TypedId<BusinessId> businessId, TypedId<UserId> userId, String username, String password) {
    return create(businessId, userId.toUuid(), username, password);
  }

  private String create(
      TypedId<BusinessId> businessId, @NonNull UUID userId, String username, String password) {
    UserRequest userRequest = new UserRequest();
    User user = new User();
    user.id = userId;
    user.username = username;
    user.password = password;
    user.data.put(BUSINESS_ID_KEY, businessId.toUuid());
    userRequest.user = user;
    ClientResponse<UserResponse, Errors> response = client.createUser(userId, userRequest);

    if (response.wasSuccessful()) {
      return response.successResponse.user.id.toString();
    }

    if (response.errorResponse != null) {
      Errors errors = response.errorResponse;
      throw new InvalidRequestException(errors.toString());
    } else if (response.exception != null) {
      Exception exception = response.exception;
      throw new RuntimeException(exception);
    } else {
      throw new RuntimeException("shouldn't have got here");
    }
  }
}
