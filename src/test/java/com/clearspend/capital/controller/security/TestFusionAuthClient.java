package com.clearspend.capital.controller.security;

import com.clearspend.capital.client.fusionauth.FusionAuthProperties;
import com.inversoft.error.Error;
import com.inversoft.error.Errors;
import com.inversoft.rest.ClientResponse;
import io.fusionauth.domain.TwoFactorMethod;
import io.fusionauth.domain.api.LoginRequest;
import io.fusionauth.domain.api.LoginResponse;
import io.fusionauth.domain.api.TwoFactorRequest;
import io.fusionauth.domain.api.TwoFactorResponse;
import io.fusionauth.domain.api.UserResponse;
import io.fusionauth.domain.api.twoFactor.TwoFactorLoginRequest;
import io.fusionauth.domain.api.twoFactor.TwoFactorSendRequest;
import io.fusionauth.domain.api.twoFactor.TwoFactorStartRequest;
import io.fusionauth.domain.api.twoFactor.TwoFactorStartResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("test")
public class TestFusionAuthClient extends io.fusionauth.client.FusionAuthClient {

  private record TwoFactorEnable(TwoFactorSendRequest request, String code) {}

  private record TwoFactorEnabled(
      TwoFactorRequest request, List<TwoFactorMethod> methods, List<String> recoveryCodes) {}

  private record TwoFactorPending(UUID userId, Map<String, Object> context, String code) {}

  private final Map<UUID, TwoFactorEnable> pendingEnable = new HashMap<>();
  private final Map<UUID, TwoFactorEnabled> twoFactorEnabled = new HashMap<>();
  private final Map<TwoFactorId, TwoFactorPending> twoFactorPending = new HashMap<>();

  @Getter
  @EqualsAndHashCode
  private static class TwoFactorId {

    private final String twoFactorId;

    TwoFactorId() {
      this.twoFactorId = RandomStringUtils.randomAscii(15);
    }

    TwoFactorId(String twoFactorId) {
      this.twoFactorId = twoFactorId;
    }
  }

  public void reset() {
    pendingEnable.clear();
    twoFactorEnabled.clear();
    twoFactorPending.clear();
  }

  public String getTwoFactorCodeForEnable(UUID userId) {
    return pendingEnable.get(userId).code;
  }

  public String getTwoFactorCodeForLogin(String twoFactorId) {
    return twoFactorPending.get(new TwoFactorId(twoFactorId)).code;
  }

  private static String generateNextTwoFactorCode() {
    return RandomStringUtils.randomNumeric(6);
  }

  public TestFusionAuthClient(FusionAuthProperties fusionAuthProperties) {
    super(fusionAuthProperties.getApiKey(), fusionAuthProperties.getBaseUrl());
  }

  /**
   * "/api/two-factor/send"
   *
   * @param request instructions to send
   * @return status 200, no content
   */
  @Override
  public ClientResponse<Void, Errors> sendTwoFactorCodeForEnableDisable(
      TwoFactorSendRequest request) {
    String code = generateNextTwoFactorCode();

    pendingEnable.put(request.userId, new TwoFactorEnable(request, code));
    return clientResponseFactory(200);
  }

  /**
   * "/api/two-factor/start" see <a
   * href="https://fusionauth.io/docs/v1/tech/apis/two-factor#start-multi-factor">Start Multi-Factor
   * docs</a>
   *
   * @param request maybe the TenantId, loginId = email
   * @return lots of info about the user's 2FA config
   */
  @Override
  public ClientResponse<TwoFactorStartResponse, Errors> startTwoFactorLogin(
      TwoFactorStartRequest request) {
    @NonNull String loginId = request.loginId;

    final ClientResponse<UserResponse, Errors> findUserResponse = retrieveUserByLoginId(loginId);
    if (!findUserResponse.wasSuccessful()) {
      return clientResponseFactory(404);
    }
    UUID userId = findUserResponse.successResponse.user.id;
    Map<String, Object> state = request.state;

    TwoFactorId twoFactorId = new TwoFactorId();
    twoFactorPending.put(
        twoFactorId, new TwoFactorPending(userId, state, generateNextTwoFactorCode()));

    List<TwoFactorMethod> methods = twoFactorEnabled.get(userId).methods;
    String code = RandomStringUtils.randomNumeric(6);
    TwoFactorStartResponse response =
        new TwoFactorStartResponse(code, methods, twoFactorId.getTwoFactorId());

    return clientResponseFactory(200, response);
  }

  private static <T, U> ClientResponse<T, U> clientResponseFactory(int httpStatus) {
    ClientResponse<T, U> response = new ClientResponse<>();
    response.status = httpStatus;
    return response;
  }

  private static <T, U> ClientResponse<T, U> clientResponseFactory(
      @Max(399) int httpStatus, T successResponse) {
    ClientResponse<T, U> response = new ClientResponse<>();
    response.status = httpStatus;
    response.successResponse = successResponse;
    return response;
  }

  private static <T, U> ClientResponse<T, U> clientResponseFactoryErr(
      @Min(300) int httpStatus, U errorResponse) {
    ClientResponse<T, U> response = new ClientResponse<>();
    response.status = httpStatus;
    response.errorResponse = errorResponse;
    return response;
  }

  @Override
  public ClientResponse<Void, Errors> disableTwoFactor(UUID userId, String methodId, String code) {
    if (!retrieveUser(userId).wasSuccessful()) {
      return clientResponseFactory(404);
    }
    // This is stupid slow, but it doesn't matter because it's not going to get prod traffic
    Entry<TwoFactorId, TwoFactorPending> pendingAuth =
        twoFactorPending.entrySet().stream()
            .filter(p -> p.getValue().userId.equals(userId) && p.getValue().code.equals(code))
            .findFirst()
            .orElse(null);

    boolean disableAll =
        pendingAuth == null && twoFactorEnabled.get(userId).recoveryCodes.remove(code);

    if (pendingAuth == null && !disableAll) {
      return clientResponseFactory(421);
    } else {
      if (pendingAuth != null) {
        twoFactorPending.remove(pendingAuth.getKey());
      }
      if (disableAll) {
        twoFactorEnabled.remove(userId);
      } else {
        Iterator<TwoFactorMethod> methods = twoFactorEnabled.get(userId).methods.iterator();
        while (methods.hasNext()) {
          TwoFactorMethod m = methods.next();
          if (m.id.equals(methodId)) {
            methods.remove();
            break;
          }
        }
        if (twoFactorEnabled.get(userId).methods.isEmpty()) {
          twoFactorEnabled.remove(userId);
        }
      }
    }
    return clientResponseFactory(200);
  }

  /**
   * "/api/user/two-factor"
   *
   * @param userId FusionAuth UUID for the user
   * @param request some particulars
   * @return about the same as what FusionAuth returns
   */
  @Override
  public ClientResponse<TwoFactorResponse, Errors> enableTwoFactor(
      UUID userId, TwoFactorRequest request) {

    if (!pendingEnable.containsKey(userId)) {
      if (!retrieveUser(userId).wasSuccessful()) {
        return clientResponseFactory(404);
      }
      return clientResponseFactoryErr(
          400, "userId", "NO2FA", "No pending 2FA request - this error is a stub");
      // Better detail in the response would be nice
      // better to wait sending it until all the errors have been identified
    } else {
      TwoFactorSendRequest enableRequest = pendingEnable.get(userId).request;
      String sentCode = pendingEnable.get(userId).code;
      assert enableRequest.mobilePhone.equals(request.mobilePhone);
      assert enableRequest.method.equals(request.method);

      if (!request.code.equals(sentCode)) {
        return clientResponseFactory(421);
      }

      TwoFactorMethod method = new TwoFactorMethod(request.method);
      method.id = RandomStringUtils.randomAlphanumeric(5);
      method.mobilePhone = enableRequest.mobilePhone;
      method.email = enableRequest.email;

      if (!twoFactorEnabled.containsKey(userId)) {
        twoFactorEnabled.put(
            userId,
            new TwoFactorEnabled(
                request,
                new ArrayList<>(),
                IntStream.range(0, 10)
                    .mapToObj(i -> generateNextTwoFactorCode())
                    .collect(Collectors.toList())));
      }
      twoFactorEnabled.get(userId).methods.add(method);
      pendingEnable.remove(userId);
      return clientResponseFactory(
          200, new TwoFactorResponse(twoFactorEnabled.get(userId).recoveryCodes));
    }
  }

  /**
   * /api/login
   *
   * @param request the request
   * @return the response
   * @throws com.clearspend.capital.common.error.FusionAuthException if things don't work out
   */
  @Override
  public ClientResponse<LoginResponse, Errors> login(LoginRequest request) {
    ClientResponse<LoginResponse, Errors> response = super.login(request);
    if (!response.wasSuccessful()) {
      return response;
    }
    if (response.successResponse != null
        && response.successResponse.user != null
        && twoFactorEnabled.containsKey(response.successResponse.user.id)) {
      TwoFactorEnabled twoFactorEnableRec = twoFactorEnabled.get(response.successResponse.user.id);
      TwoFactorId twoFactorId = new TwoFactorId();
      twoFactorPending.put(
          twoFactorId,
          new TwoFactorPending(
              response.successResponse.user.id,
              Map.of("successResponse", response.successResponse),
              null));

      LoginResponse loginResponse = new LoginResponse();
      loginResponse.twoFactorId = twoFactorId.getTwoFactorId();
      loginResponse.methods = twoFactorEnableRec.methods;

      response = clientResponseFactory(242, loginResponse);
    }
    return response;
  }

  /**
   * /api/two-factor/send
   *
   * @param twoFactorId the ID sent to the client with the 242 login response
   * @param request only the MethodId
   * @return not much, errors only
   */
  @Override
  public ClientResponse<Void, Errors> sendTwoFactorCodeForLoginUsingMethod(
      String twoFactorId, TwoFactorSendRequest request) {
    TwoFactorId twoFactorId1 = new TwoFactorId(twoFactorId);
    TwoFactorPending pending = twoFactorPending.get(twoFactorId1);
    if (pending == null) {
      return clientResponseFactoryErr(
          400, "twoFactorId", "[invalid]twoFactorId", "The given twoFactorId was not found.");
    }
    TwoFactorMethod method =
        twoFactorEnabled.get(pending.userId).methods.stream()
            .filter(fa -> fa.id.equals(request.methodId))
            .findFirst()
            .orElse(null);

    if (method == null) {
      // this is the actual message we got when sending "sms" instead of the code:
      // {"message":"{\n  \"fieldErrors\" : {\n    \"methodId\" : [ {\n      \"code\" :
      // \"[invalid]methodId\",\n      \"message\" : \"The [methodId] is not valid. No two-factor
      // method with this Id was found enabled for the user.\"\n    } ]\n  },\n  \"generalErrors\" :
      // [ ]\n}"}
      return clientResponseFactoryErr(
          400,
          "methodId",
          "[invalid]methodId",
          "The [methodId] is not valid. No two-factor method with this Id was found enabled for the user.");
    }
    String code = generateNextTwoFactorCode();

    twoFactorPending.put(
        twoFactorId1,
        new TwoFactorPending(pending.userId, twoFactorPending.get(twoFactorId1).context, code));

    return clientResponseFactory(200);
  }

  @NotNull
  private <T> ClientResponse<T, Errors> clientResponseFactoryErr(
      int httpStatus, String field, String code, String message) {
    Errors errors = new Errors();
    errors.fieldErrors.put(field, List.of(new Error(code, message)));
    return clientResponseFactoryErr(httpStatus, errors);
  }

  /**
   * /api/two-factor/login
   *
   * @param request the request, with a 2FA code
   * @return the response, 200 if it's ducky.
   * @throws com.clearspend.capital.common.error.FusionAuthException if things go sideways
   */
  public ClientResponse<LoginResponse, Errors> twoFactorLogin(TwoFactorLoginRequest request) {
    ClientResponse<LoginResponse, Errors> response = new ClientResponse<>();

    TwoFactorId twoFactorId = new TwoFactorId(request.twoFactorId);
    if (!twoFactorPending.containsKey(twoFactorId)) {
      return clientResponseFactoryErr(
          404, "twoFactorId", "[invalid]twoFactorId", "Doesn't match an outstanding code");
    }

    final TwoFactorPending twoFactorPending = this.twoFactorPending.get(twoFactorId);
    if (request.code.equals(twoFactorPending.code)
        || twoFactorEnabled.get(twoFactorPending.userId).recoveryCodes.contains(request.code)) {
      response.successResponse =
          (LoginResponse) this.twoFactorPending.remove(twoFactorId).context.get("successResponse");
      response.status = 200;
    } else {
      response.status = 421;
    }
    return response;
  }
}
