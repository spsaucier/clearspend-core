package com.clearspend.capital.controller.security;

import com.inversoft.error.Error;
import com.inversoft.error.Errors;
import com.inversoft.rest.ClientResponse;
import io.fusionauth.domain.TwoFactorMethod;
import io.fusionauth.domain.api.LoginRequest;
import io.fusionauth.domain.api.LoginResponse;
import io.fusionauth.domain.api.TwoFactorRequest;
import io.fusionauth.domain.api.TwoFactorResponse;
import io.fusionauth.domain.api.twoFactor.TwoFactorLoginRequest;
import io.fusionauth.domain.api.twoFactor.TwoFactorSendRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TestFusionAuthClient extends io.fusionauth.client.FusionAuthClient {

  private static final Random random = new Random(0);

  private record TwoFactorEnable(TwoFactorSendRequest request, String code) {}

  private record TwoFactorEnabled(TwoFactorRequest request, List<String> recoveryCodes) {}

  private record TwoFactorPending(LoginResponse success, String code) {}

  private final Map<UUID, TwoFactorEnable> pendingEnable = new HashMap<>();
  private final Map<UUID, TwoFactorEnabled> twoFactorEnabled = new HashMap<>();
  private final Map<UUID, TwoFactorPending> twoFactorPending =
      new HashMap<UUID, TwoFactorPending>();

  public void reset() {
    pendingEnable.clear();
    twoFactorEnabled.clear();
    twoFactorPending.clear();
  }

  public String getTwoFactorCodeForEnable(UUID userId) {
    return pendingEnable.get(userId).code;
  }

  public String getTwoFactorCodeForLogin(UUID twoFactorId) {
    return twoFactorPending.get(twoFactorId).code;
  }

  private static String generateNextTwoFactorCode() {
    String code = String.valueOf((int) (random.nextDouble() * 1e+7) % 1e+7);
    return code;
  }

  public TestFusionAuthClient(String apiKey, String baseURL) {
    super(apiKey, baseURL);
  }

  @Override
  public ClientResponse<Void, Errors> sendTwoFactorCodeForEnableDisable(
      TwoFactorSendRequest request) {
    String code = generateNextTwoFactorCode();
    pendingEnable.put(request.userId, new TwoFactorEnable(request, code));
    ClientResponse<Void, Errors> response = new ClientResponse<>();
    response.status = 200;
    return response;
  }

  @Override
  public ClientResponse<TwoFactorResponse, Errors> enableTwoFactor(
      UUID userId, TwoFactorRequest request) {
    ClientResponse<TwoFactorResponse, Errors> response = new ClientResponse<>();

    if (!pendingEnable.containsKey(userId)) {
      response.status = 400;
      response.errorResponse = new Errors();
      Error error = new Error();
      response.errorResponse.fieldErrors.put(
          "userId", List.of(new Error("NO2FA", "No pending 2FA request - this error is a stub")));
      // Better detail in the response would be nice
      // better to wait sending it until all the errors have been identified
      return response;
    } else {
      TwoFactorSendRequest enableRequest = pendingEnable.get(userId).request;
      String sentCode = pendingEnable.get(userId).code;
      assert enableRequest.mobilePhone.equals(request.mobilePhone);
      assert enableRequest.method.equals(request.method);
      // TODO at least mock the behavior of a bad code
      if (!request.code.equals(sentCode)) {
        response.status = 421;
        return response;
      }

      response.status = 200;
      twoFactorEnabled.put(
          userId,
          new TwoFactorEnabled(
              request,
              IntStream.range(0, 10)
                  .mapToObj(i -> generateNextTwoFactorCode())
                  .collect(Collectors.toList())));
      pendingEnable.remove(userId);
      response.successResponse = new TwoFactorResponse(twoFactorEnabled.get(userId).recoveryCodes);
      response.status = 200;
      return response;
    }
  }

  @Override
  public ClientResponse<LoginResponse, Errors> login(LoginRequest request) {
    ClientResponse<LoginResponse, Errors> response = super.login(request);
    if (!response.wasSuccessful()) {
      return response;
    }
    if (twoFactorEnabled.containsKey(response.successResponse.user.id)) {
      UUID twoFactorId = UUID.randomUUID();
      twoFactorPending.put(twoFactorId, new TwoFactorPending(response.successResponse, null));

      response = new ClientResponse<>();
      response.successResponse = new LoginResponse();
      response.successResponse.twoFactorId = twoFactorId.toString();
      response.successResponse.methods = List.of(new TwoFactorMethod("sms"));

      response.status = 242;
    }
    return response;
  }

  @Override
  public ClientResponse<Void, Errors> sendTwoFactorCodeForLoginUsingMethod(
      String twoFactorId, TwoFactorSendRequest request) {
    String code = generateNextTwoFactorCode();
    final UUID twoFactorUUID = UUID.fromString(twoFactorId);
    twoFactorPending.put(
        twoFactorUUID, new TwoFactorPending(twoFactorPending.get(twoFactorUUID).success, code));
    ClientResponse<Void, Errors> response = new ClientResponse<>();
    response.status = 200;
    return response;
  }

  public ClientResponse<LoginResponse, Errors> twoFactorLogin(TwoFactorLoginRequest request) {
    ClientResponse<LoginResponse, Errors> response = new ClientResponse<>();

    UUID twoFactorId = UUID.fromString(request.twoFactorId);
    if (!twoFactorPending.containsKey(twoFactorId)) {
      response.status = 400;
      response.errorResponse = new Errors();
      response.errorResponse.addFieldError(
          "twoFactorId", "unknown", "Doesn't match an outstanding code");
      return response;
    }

    if (request.code.equals(twoFactorPending.get(twoFactorId).code)) {
      response.successResponse = twoFactorPending.remove(twoFactorId).success;
      response.status = 200;
    } else {
      response.status = 403;
    }
    return response;
  }
}
