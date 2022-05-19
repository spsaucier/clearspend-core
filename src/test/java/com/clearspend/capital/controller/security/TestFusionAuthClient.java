package com.clearspend.capital.controller.security;

import com.clearspend.capital.client.fusionauth.FusionAuthProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inversoft.error.Error;
import com.inversoft.error.Errors;
import com.inversoft.rest.ClientResponse;
import io.fusionauth.domain.TwoFactorMethod;
import io.fusionauth.domain.User;
import io.fusionauth.domain.UserTwoFactorConfiguration;
import io.fusionauth.domain.api.LoginRequest;
import io.fusionauth.domain.api.LoginResponse;
import io.fusionauth.domain.api.TwoFactorDisableRequest;
import io.fusionauth.domain.api.TwoFactorRecoveryCodeResponse;
import io.fusionauth.domain.api.TwoFactorRequest;
import io.fusionauth.domain.api.TwoFactorResponse;
import io.fusionauth.domain.api.UserResponse;
import io.fusionauth.domain.api.twoFactor.TwoFactorLoginRequest;
import io.fusionauth.domain.api.twoFactor.TwoFactorSendRequest;
import io.fusionauth.domain.api.twoFactor.TwoFactorStartRequest;
import io.fusionauth.domain.api.twoFactor.TwoFactorStartResponse;
import io.fusionauth.domain.api.user.ChangePasswordRequest;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("test")
public class TestFusionAuthClient extends io.fusionauth.client.FusionAuthClient {

  /**
   * We have a beautiful explanation of why there is a single object mapper for the entire app - 1.
   * it takes awhile to instantiate and 2. it should be consistent throughout the app. Well, this
   * one should be a little different - configurable to match up with whatever quirks the FusionAuth
   * library offers.
   */
  private static final ObjectMapper objectMapper = new ObjectMapper();

  private record TwoFactorEnable(TwoFactorSendRequest request, String code) {}

  private record TwoFactorPending(
      UUID userId, String state, LoginResponse loginResponse, String code, String trustChallenge) {}

  private final Map<UUID, TwoFactorEnable> pendingEnable = new HashMap<>();
  private final Map<UUID, UserTwoFactorConfiguration> twoFactorConfiguration = new HashMap<>();
  private final Map<TwoFactorId, TwoFactorPending> twoFactorPending = new HashMap<>();
  private final Map<String, String> trustTokensToChallenges = new HashMap<>();

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
    twoFactorConfiguration.clear();
    twoFactorPending.clear();
    trustTokensToChallenges.clear();
  }

  public String getTwoFactorCodeForEnable(UUID userId) {
    return pendingEnable.get(userId).code;
  }

  public String getTwoFactorCodeForLogin(String twoFactorId) {
    return twoFactorPending.get(new TwoFactorId(twoFactorId)).code;
  }

  private static String generateNextRecoveryCode() {
    return (RandomStringUtils.randomAlphanumeric(5) + "-" + RandomStringUtils.randomAlphanumeric(5))
        .toUpperCase(Locale.ROOT);
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
    String state =
        Optional.ofNullable(request.state)
            .map(
                s -> {
                  try {
                    return objectMapper.writeValueAsString(s);
                  } catch (JsonProcessingException e) {
                    throw new RuntimeException();
                  }
                })
            .orElse("{}");

    String code = generateNextTwoFactorCode();
    TwoFactorId twoFactorId = new TwoFactorId();
    twoFactorPending.put(
        twoFactorId, new TwoFactorPending(userId, state, null, code, request.trustChallenge));

    List<TwoFactorMethod> methods =
        Optional.ofNullable(twoFactorConfiguration.get(userId))
            .map(cfg -> cfg.methods)
            .orElse(List.of());
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
    TwoFactorDisableRequest request = new TwoFactorDisableRequest();
    request.methodId = methodId;
    request.code = code;
    /*
     * It might be appropriate to pull out common code between these two methods as
     * a private function, but that's not necessary since it is not calling FA
     * but it's mocking the functionality since it's not passing work up to the
     * superclass.
     */
    return disableTwoFactorWithRequest(userId, request);
  }

  @Override
  public ClientResponse<Void, Errors> disableTwoFactorWithRequest(
      UUID userId, TwoFactorDisableRequest request) {
    String code = request.code;
    String methodId = request.methodId;

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
        pendingAuth == null
            && Optional.ofNullable(twoFactorConfiguration.get(userId))
                .map(c -> c.recoveryCodes.remove(code))
                .orElse(false);

    if (pendingAuth == null && !disableAll) {
      return clientResponseFactory(421);
    } else {
      // This is the place to validate tokens, but FA doesn't support that (yet?)

      // good to go

      if (pendingAuth != null) {
        twoFactorPending.remove(pendingAuth.getKey());
      }
      if (disableAll) {
        twoFactorConfiguration.remove(userId);
      } else {
        Iterator<TwoFactorMethod> methods = twoFactorConfiguration.get(userId).methods.iterator();
        while (methods.hasNext()) {
          TwoFactorMethod m = methods.next();
          if (m.id.equals(methodId)) {
            methods.remove();
            break;
          }
        }
        if (twoFactorConfiguration.get(userId).methods.isEmpty()) {
          twoFactorConfiguration.remove(userId);
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

      init2Factor(userId);
      twoFactorConfiguration.get(userId).methods.add(method);
      pendingEnable.remove(userId);
      return clientResponseFactory(
          200, new TwoFactorResponse(twoFactorConfiguration.get(userId).recoveryCodes));
    }
  }

  private void init2Factor(UUID userId) {
    if (!twoFactorConfiguration.containsKey(userId)) {
      UserTwoFactorConfiguration config = new UserTwoFactorConfiguration();
      config.recoveryCodes.addAll(generateRecoveryCodes());
      twoFactorConfiguration.put(userId, config);
    }
  }

  @SneakyThrows
  @Override
  public ClientResponse<UserResponse, Errors> patchUser(UUID userId, Map<String, Object> request) {
    Map<String, Object> userMod = (Map<String, Object>) request.get("user");
    User patch = objectMapper.readValue(objectMapper.writeValueAsString(userMod), User.class);

    List<TwoFactorMethod> methods =
        Optional.ofNullable(patch.twoFactor)
            .flatMap(t -> Optional.ofNullable(t.methods))
            .orElse(Collections.emptyList());

    if (!methods.isEmpty()) {
      init2Factor(userId);
      twoFactorConfiguration.get(userId).methods.addAll(methods);
      patch.twoFactor = null;
    }

    return super.patchUser(
        userId,
        objectMapper.readValue(
            objectMapper.writeValueAsString(patch), new TypeReference<Map<String, Object>>() {}));
  }

  @NotNull
  private List<String> generateRecoveryCodes() {
    return IntStream.range(0, 10)
        .mapToObj(i -> generateNextRecoveryCode())
        .collect(Collectors.toList());
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
        && twoFactorConfiguration.containsKey(response.successResponse.user.id)) {
      UserTwoFactorConfiguration twoFactorEnableRec =
          twoFactorConfiguration.get(response.successResponse.user.id);
      TwoFactorId twoFactorId = new TwoFactorId();
      twoFactorPending.put(
          twoFactorId,
          new TwoFactorPending(
              response.successResponse.user.id, "{}", response.successResponse, null, null));

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
        twoFactorConfiguration.get(pending.userId).methods.stream()
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
        new TwoFactorPending(
            pending.userId,
            twoFactorPending.get(twoFactorId1).state,
            twoFactorPending.get(twoFactorId1).loginResponse,
            code,
            pending.trustChallenge));

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
    if (twoFactorPending == null) {
      response.status = 421;
    } else if (twoFactorPending.code.equals(request.code)
        || spendRecoveryCode(twoFactorPending.userId, request.code)) {
      this.twoFactorPending.remove(twoFactorId);
      /* for a normal login, this mock will cache the result of the login pending 2FA validation
       * and then dress the user with their mocked 2FA status.
       */
      response.successResponse =
          Optional.ofNullable(twoFactorPending.loginResponse)
              .map(
                  lr -> {
                    apply2FA(lr.user);
                    return lr;
                  })
              .orElse(
                  new LoginResponse(retrieveUser(twoFactorPending.userId).successResponse.user));
      Optional.ofNullable(twoFactorPending.trustChallenge)
          .ifPresent(
              t -> {
                String trustToken = RandomStringUtils.randomPrint(24);
                response.successResponse.trustToken = trustToken;
                this.trustTokensToChallenges.put(trustToken, twoFactorPending.trustChallenge);
              });
      response.status = 200;
      try {
        response.successResponse.state =
            objectMapper.readValue(
                twoFactorPending.state(), new TypeReference<Map<String, Object>>() {});
      } catch (JsonProcessingException e) {
        throw new RuntimeException(e);
      }
    } else {
      response.status = 421;
    }
    return response;
  }

  /**
   * Spend an outstanding recovery code.
   *
   * @param userId The user whose code to spend
   * @param code The code being spent
   * @return True if the code was spent, false if it didn't match outstanding codes
   */
  private boolean spendRecoveryCode(@NonNull UUID userId, @NonNull String code) {
    return twoFactorConfiguration.get(userId).recoveryCodes.remove(code);
  }

  @Override
  public ClientResponse<Void, Errors> changePasswordByIdentity(ChangePasswordRequest request) {
    /*
    "{\n \"fieldErrors\" : { },\n \"generalErrors\" : [ {\n \"code\" : \"[TrustTokenRequired]\",\n \"message\" : \"This request requires a Trust Token. Use the Start Two-Factor API to obtain a Trust Token required to complete this request.\"\n } ]\n}"
     */
    if (retrieveUserByLoginId(request.loginId).successResponse.user.twoFactorEnabled()) {
      if (StringUtils.isEmpty(request.trustToken)) {
        return clientResponseFactoryErr(
            400,
            "trustToken",
            "[TrustTokenRequired]",
            "This request requires a Trust Token. Use the Start Two-Factor API to obtain a Trust Token required to complete this request.");
      } else {
        if (!validateTrustPair(request.trustChallenge, request.trustToken)) {
          return clientResponseFactoryErr(400, "trustToken", "token mismatch", "Gotta fix that.");
        }
      }
    }
    request.trustChallenge = null;
    request.trustToken = null;
    return super.changePasswordByIdentity(request);
  }

  private boolean validateTrustPair(String trustChallenge, String trustToken) {
    if (trustTokensToChallenges.get(trustToken).equals(trustChallenge)) {
      trustTokensToChallenges.remove(trustToken);
      return true;
    }
    return false;
  }

  @Override
  public ClientResponse<UserResponse, Errors> retrieveUser(UUID userId) {
    return apply2FA(super.retrieveUser(userId));
  }

  @Override
  public ClientResponse<UserResponse, Errors> retrieveUserByChangePasswordId(
      String changePasswordId) {
    return apply2FA(super.retrieveUserByChangePasswordId(changePasswordId));
  }

  @Override
  public ClientResponse<UserResponse, Errors> retrieveUserByEmail(String email) {
    return apply2FA(super.retrieveUserByEmail(email));
  }

  @Override
  public ClientResponse<UserResponse, Errors> retrieveUserByUsername(String username) {
    return apply2FA(super.retrieveUserByUsername(username));
  }

  @Override
  public ClientResponse<TwoFactorRecoveryCodeResponse, Errors> generateTwoFactorRecoveryCodes(
      UUID userId) {
    List<String> codes = twoFactorConfiguration.get(userId).recoveryCodes;
    codes.clear();
    codes.addAll(generateRecoveryCodes());
    return retrieveTwoFactorRecoveryCodes(userId);
  }

  @Override
  public ClientResponse<TwoFactorRecoveryCodeResponse, Errors> retrieveTwoFactorRecoveryCodes(
      UUID userId) {
    List<String> codes = twoFactorConfiguration.get(userId).recoveryCodes;
    return clientResponseFactory(200, new TwoFactorRecoveryCodeResponse(codes));
  }

  @Override
  public ClientResponse<UserResponse, Errors> retrieveUserByVerificationId(String verificationId) {
    return apply2FA(super.retrieveUserByVerificationId(verificationId));
  }

  @Override
  public ClientResponse<UserResponse, Errors> retrieveUserByLoginId(String loginId) {
    return apply2FA(super.retrieveUserByLoginId(loginId));
  }

  /**
   * For mocking fidelity - retrieving user should yeild 2FA config
   *
   * @param response to be checked for a user and decorated
   * @return the same response, if successful, with mock 2FA config
   */
  private ClientResponse<UserResponse, Errors> apply2FA(
      ClientResponse<UserResponse, Errors> response) {
    if (response.wasSuccessful()) {
      apply2FA(response.successResponse.user);
    }
    return response;
  }

  /**
   * For mocking fidelity - retrieving user should yield 2FA config
   *
   * @param user having some 2FA config
   */
  private void apply2FA(User user) {
    Optional.ofNullable(twoFactorConfiguration.get(user.id))
        .ifPresent(config -> user.twoFactor = config);
  }
}
