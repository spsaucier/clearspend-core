package com.clearspend.capital.service;

import com.clearspend.capital.client.fusionauth.FusionAuthProperties;
import com.clearspend.capital.common.error.InvalidRequestException;
import com.clearspend.capital.common.error.RecordNotFoundException;
import com.clearspend.capital.common.error.Table;
import com.clearspend.capital.common.masking.annotation.Sensitive;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.controller.type.SteppedUpRequest;
import com.clearspend.capital.controller.type.user.ForgotPasswordRequest;
import com.clearspend.capital.controller.type.user.ResetPasswordRequest;
import com.clearspend.capital.data.model.OwnerRelated;
import com.clearspend.capital.permissioncheck.annotations.OpenAccessAPI;
import com.clearspend.capital.service.UserService.FusionAuthUserLookup;
import com.clearspend.capital.service.type.CurrentUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.errorprone.annotations.RestrictedApi;
import com.inversoft.error.Errors;
import com.inversoft.rest.ClientResponse;
import io.fusionauth.domain.Application;
import io.fusionauth.domain.ChangePasswordReason;
import io.fusionauth.domain.TwoFactorMethod;
import io.fusionauth.domain.User;
import io.fusionauth.domain.UserRegistration;
import io.fusionauth.domain.api.LoginRequest;
import io.fusionauth.domain.api.LoginResponse;
import io.fusionauth.domain.api.TwoFactorRequest;
import io.fusionauth.domain.api.TwoFactorResponse;
import io.fusionauth.domain.api.UserResponse;
import io.fusionauth.domain.api.twoFactor.TwoFactorLoginRequest;
import io.fusionauth.domain.api.twoFactor.TwoFactorSendRequest;
import io.fusionauth.domain.api.twoFactor.TwoFactorStartRequest;
import io.fusionauth.domain.api.twoFactor.TwoFactorStartResponse;
import io.fusionauth.domain.api.user.ChangePasswordResponse;
import io.fusionauth.domain.api.user.ForgotPasswordResponse;
import io.fusionauth.domain.api.user.RegistrationRequest;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.BiConsumer;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class FusionAuthService {

  private final CoreFusionAuthService coreFusionAuthService;
  private final UserService userService;

  public @interface FusionAuthUserCreator {

    String reviewer();

    String explanation();
  }

  public @interface FusionAuthUserModifier {

    String reviewer();

    String explanation();
  }

  public @interface FusionAuthUserAccessor {

    String reviewer();

    String explanation();
  }

  @interface FusionAuthRoleAdministrator {

    String reviewer();

    String explanation();
  }

  private final io.fusionauth.client.FusionAuthClient client;
  private final FusionAuthProperties fusionAuthProperties;
  private final ObjectMapper objectMapper;
  private final TwilioService twilioService;

  /**
   * 1:1 mapping to the FusionAuth ChangePasswordReason, in case we want to change out FusionAuth
   * later or to isolate from FA changes.
   */
  @Getter
  public enum CapitalChangePasswordReason {
    Administrative(ChangePasswordReason.Administrative),
    Breached(ChangePasswordReason.Breached),
    Expired(ChangePasswordReason.Expired),
    Validation(ChangePasswordReason.Validation);

    public final ChangePasswordReason fusionAuthReason;

    CapitalChangePasswordReason(ChangePasswordReason fusionAuthReason) {
      this.fusionAuthReason = fusionAuthReason;
    }
  }

  private Map<String, Object> getRegistrationData(UUID userId) {
    return validateResponse(client.retrieveRegistration(userId, getApplicationId()))
        .registration
        .data;
  }

  private void setRegistrationData(User user, Map<String, Object> data) {
    UserRegistration registration = user.getRegistrationForApplication(getApplicationId());
    registration.data.clear();
    registration.data.putAll(data);
    validateResponse(
        client.updateRegistration(user.id, new RegistrationRequest(user, registration)));
  }

  private void persistStepUp(User user) {
    Map<String, Object> data = getRegistrationData(user.id);
    data.put(
        "stepUpExpiry",
        (long)
            (System.currentTimeMillis() / 1000.0
                + fusionAuthProperties.getStepUpValidPeriodSecs()));
    setRegistrationData(user, data);
  }

  private boolean isSteppedUp(FusionAuthUser user) {
    return isSteppedUp(user.getFusionAuthId());
  }

  private boolean isSteppedUp(UUID userId) {
    return Optional.ofNullable(getRegistrationData(userId).get("stepUpExpiry"))
        .map(e -> (((Number) e).doubleValue() >= System.currentTimeMillis() / 1000.0))
        .orElse(false);
  }

  /**
   * Completing a 2-factor sequence by calling "/api/two-factor/login"
   *
   * @param request with parameters including a code and twoFactorId
   * @return the User if successful, several codes in the 400s for bad submissions
   */
  @RestrictedApi(
      explanation = "This should only ever be used by AuthenticationController",
      allowlistAnnotations = {FusionAuthUserAccessor.class, FusionAuthUserModifier.class},
      link =
          "https://tranwall.atlassian.net/wiki/spaces/CAP/pages/2088828965/Dev+notes+Service+method+security")
  public LoginResponse twoFactorLogin(TwoFactorLoginRequest request) {
    return validateResponse(client.twoFactorLogin(request));
  }

  public enum TwoFactorAuthenticationMethod {
    email,
    sms,
    authenticator
  }

  @RestrictedApi(
      explanation = "This should only ever be used by AuthenticationController",
      allowlistAnnotations = {FusionAuthUserModifier.class},
      link =
          "https://tranwall.atlassian.net/wiki/spaces/CAP/pages/2088828965/Dev+notes+Service+method+security")
  public void sendInitialTwoFactorCode(
      @NonNull UUID fusionAuthUserId,
      @NonNull FusionAuthService.TwoFactorAuthenticationMethod method,
      @NotNull String destination) {
    if (!fusionAuthUserId.equals(CurrentUser.getFusionAuthUserId())) {
      throw new AccessDeniedException("");
    }
    if (twoFactorEnabled(FusionAuthUser.fromCurrentUser())) {
      throw new AccessDeniedException("2-factor enabled");
    }
    sendTwoFactorCodeToNewDestination(fusionAuthUserId, method, destination);
  }

  /** For adding a new method */
  private void sendTwoFactorCodeToNewDestination(
      @NonNull UUID fusionAuthUserId,
      @NonNull FusionAuthService.TwoFactorAuthenticationMethod method,
      @NotNull String destination) {
    TwoFactorSendRequest request = new TwoFactorSendRequest();
    request.method = method.name();
    switch (method) {
      case email -> request.email = destination;
      case sms -> request.mobilePhone = destination;
      case authenticator -> throw new InvalidRequestException(
          "Authenticator does not support sending");
      default -> throw new IllegalArgumentException("bad method " + method);
    }
    request.userId = fusionAuthUserId;

    validateResponse(client.sendTwoFactorCodeForEnableDisable(request));
  }

  private TwoFactorStartResponse startTwoFactorLogin(
      FusionAuthUser user, Map<String, Object> state, String trustChallenge) {
    TwoFactorStartRequest request = new TwoFactorStartRequest();
    request.trustChallenge = trustChallenge;
    request.applicationId = getApplicationId();
    request.loginId = user.getEmail();
    request.state = Optional.ofNullable(state).orElse(Collections.emptyMap());
    return validateResponse(client.startTwoFactorLogin(request));
  }

  /**
   * Preceded by {@link #startTwoFactorLogin(FusionAuthUser, Map, String)}
   *
   * @param user the user to modify
   * @param methodId the method ID to disable (from the request initiating the flow)
   * @param code the 2FA code
   */
  @PreAuthorize("hasGlobalPermission('CUSTOMER_SERVICE') or isSelfOwned(#user)")
  public void disableTwoFactor(
      com.clearspend.capital.data.model.User user, String methodId, String code) {
    validateResponse(
        client.disableTwoFactor(UUID.fromString(user.getSubjectRef()), methodId, code));
  }

  @PreAuthorize("isSelfOwned(#user)")
  public TwoFactorResponse validateFirstTwoFactorCode(
      @NonNull FusionAuthUser user,
      @NonNull String code,
      @NonNull FusionAuthService.TwoFactorAuthenticationMethod method,
      @NonNull String destination) {

    TwoFactorRequest request = new TwoFactorRequest();
    request.code = code;
    request.method = method.name();
    request.applicationId = getApplicationId();
    switch (method) {
      case email -> request.email = destination;
      case sms -> request.mobilePhone = destination;
      case authenticator -> request.secret = destination;
    }

    return validateResponse(client.enableTwoFactor(user.getFusionAuthId(), request));
  }

  public enum RoleChange {
    GRANT,
    REVOKE
  }

  /**
   * @param user the ClearSpend user of interest
   * @return the FusionAuth user record
   */
  User getUser(FusionAuthUser user) {
    return coreFusionAuthService.getUser(UUID.fromString(user.getSubjectRef()));
  }

  @OpenAccessAPI(
      explanation = "Users need to be able to reset their password not logged in",
      reviewer = "jscarbor")
  public void forgotPassword(ForgotPasswordRequest request) {
    io.fusionauth.domain.api.user.ForgotPasswordRequest fusionAuthRequest =
        new io.fusionauth.domain.api.user.ForgotPasswordRequest();
    fusionAuthRequest.loginId = request.getEmail();
    fusionAuthRequest.sendForgotPasswordEmail = false;
    fusionAuthRequest.applicationId = getApplicationId();
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

  @OpenAccessAPI(
      explanation =
          "Users are authenticated in this call by a changePasswordId sent to their email",
      reviewer = "jscarbor")
  @FusionAuthUserLookup(
      reviewer = "Craig Miller",
      explanation =
          "We may not have a security context properly available during the reset password flow.")
  public Optional<com.clearspend.capital.controller.type.twofactor.TwoFactorStartResponse>
      resetPassword(final ResetPasswordRequest request) {
    final ClientResponse<UserResponse, Errors> fusionAuthUserResponse =
        client.retrieveUserByChangePasswordId(request.getChangePasswordId());
    final String fusionAuthId = validateResponse(fusionAuthUserResponse).user.id.toString();
    final com.clearspend.capital.data.model.User user =
        userService
            .retrieveUserBySubjectRef(fusionAuthId)
            .orElseThrow(() -> new RecordNotFoundException(Table.USER, fusionAuthId));
    final FusionAuthUser fusionAuthUser = FusionAuthUser.fromUser(user);

    return stepUpRequest(
        fusionAuthUser,
        request,
        (updatedRequest, trustToken) -> {
          final io.fusionauth.domain.api.user.ChangePasswordRequest faRequest =
              new io.fusionauth.domain.api.user.ChangePasswordRequest(
                  updatedRequest.getNewPassword());
          faRequest.trustToken = trustToken;
          faRequest.trustChallenge = updatedRequest.getTrustChallenge();
          faRequest.applicationId = getApplicationId();

          final ClientResponse<ChangePasswordResponse, Errors> changePasswordResponse =
              client.changePassword(updatedRequest.getChangePasswordId(), faRequest);

          validateResponse(changePasswordResponse);
          // If no exception is thrown above it is successful
          twilioService.sendPasswordResetSuccessEmail(
              user.getEmail().getEncrypted(), user.getFirstName().getEncrypted());
        });
  }

  /**
   * This is for things requiring a very fresh code, such as disabling 2FA and changing password.
   * This is not how a 2FA login begins - that begins the same as every other login.
   *
   * @param user the User object corresponding to CurrentUser
   * @param state any state to be returned after successful authentication
   * @return codes required to proceed
   */
  @PreAuthorize("isSelfOwned(#user)")
  public com.clearspend.capital.controller.type.twofactor.TwoFactorStartResponse beginStepUp(
      FusionAuthUser user, Map<String, Object> state) {
    String trustChallenge = RandomStringUtils.randomPrint(24);
    TwoFactorStartResponse initResponse = startTwoFactorLogin(user, state, trustChallenge);
    final List<TwoFactorMethod> userMethods = initResponse.methods;
    final String twoFactorId = initResponse.twoFactorId;

    final String methodId = sendTwoFactorCodeUsingMethod(twoFactorId, userMethods);
    return new com.clearspend.capital.controller.type.twofactor.TwoFactorStartResponse(
        twoFactorId, methodId, trustChallenge);
  }

  private String sendTwoFactorCodeUsingMethod(
      String twoFactorId, Collection<TwoFactorMethod> userMethods) {
    final String methodId =
        userMethods.stream()
            .filter(m -> m.method.equals(TwoFactorMethod.SMS))
            .findFirst()
            .map(m -> m.id)
            .orElseThrow();
    sendTwoFactorCodeUsingMethod(twoFactorId, methodId);
    return methodId;
  }

  @Value
  public static class ChangeMethodRequest
      implements SteppedUpRequest<ChangeMethodRequest>, OwnerRelated {
    TypedId<BusinessId> businessId;
    TypedId<UserId> ownerId;
    String destination;
    TwoFactorAuthenticationMethod method;
    String twoFactorCode;
    String twoFactorId;
    String trustChallenge;

    boolean matches(TwoFactorMethod method) {
      if (!this.method.name().equals(method.method)) {
        return false;
      }
      return switch (this.method) {
        case sms -> this.destination.equalsIgnoreCase(method.mobilePhone);
        case email -> this.destination.equalsIgnoreCase(method.email);
        default -> false;
      };
    }

    @Override
    public ChangeMethodRequest withTwoFactorParams(
        final String trustChallenge, final String twoFactorId, final String twoFactorCode) {
      return new ChangeMethodRequest(
          businessId, ownerId, destination, method, twoFactorCode, twoFactorId, trustChallenge);
    }
  }

  @Value
  public static class ChangePasswordRequest implements SteppedUpRequest<ChangePasswordRequest> {
    String currentPassword;
    String newPassword;
    String trustChallenge;
    String twoFactorId;
    String twoFactorCode;

    @Override
    public ChangePasswordRequest withTwoFactorParams(
        final String trustChallenge, final String twoFactorId, final String twoFactorCode) {
      return new ChangePasswordRequest(
          currentPassword, newPassword, trustChallenge, twoFactorId, twoFactorCode);
    }
  }

  private boolean is2FAStartRequest(final SteppedUpRequest<?> request) {
    return StringUtils.isAnyEmpty(
        request.getTrustChallenge(), request.getTwoFactorId(), request.getTwoFactorCode());
  }

  @SneakyThrows
  private Map<String, Object> createStepUpPersistentState(final SteppedUpRequest<?> request) {
    return Map.of("stepUpRequest", objectMapper.writeValueAsString(request));
  }

  @FusionAuthUserAccessor(
      reviewer = "Craig Miller",
      explanation = "Need to perform a 2 factor login here")
  private LoginResponse twoFactorStepUpLogin(
      final FusionAuthUser actor, final SteppedUpRequest<?> request) {
    final TwoFactorLoginRequest twoFactorLoginRequest =
        new TwoFactorLoginRequest(
            getApplicationId(), request.getTwoFactorCode(), request.getTwoFactorId());
    twoFactorLoginRequest.userId = actor.getFusionAuthId();
    final LoginResponse twoFactorLoginResponse = twoFactorLogin(twoFactorLoginRequest);
    persistStepUp(getUser(actor));
    return twoFactorLoginResponse;
  }

  @FusionAuthUserModifier(reviewer = "jscarbor", explanation = "modifying user")
  @SneakyThrows
  private <T extends SteppedUpRequest<T>>
      Optional<com.clearspend.capital.controller.type.twofactor.TwoFactorStartResponse>
          stepUpRequest(final FusionAuthUser actor, T request, BiConsumer<T, String> operation) {
    @SuppressWarnings("unchecked")
    final Class<T> requestClass = (Class<T>) request.getClass();

    if (twoFactorEnabled(actor) && is2FAStartRequest(request) && !isSteppedUp(actor)) {
      return Optional.of(beginStepUp(actor, createStepUpPersistentState(request)));
    }

    final T requestWithStepUpParams;
    final LoginResponse twoFactorLoginResponse;
    // isSteppedUp will be true here
    if (twoFactorEnabled(actor) && is2FAStartRequest(request)) {
      final String trustChallenge = RandomStringUtils.randomPrint(24);
      final TwoFactorStartResponse initResponse =
          startTwoFactorLogin(actor, createStepUpPersistentState(request), trustChallenge);
      requestWithStepUpParams =
          request.withTwoFactorParams(trustChallenge, initResponse.twoFactorId, initResponse.code);
      twoFactorLoginResponse = twoFactorStepUpLogin(actor, requestWithStepUpParams);
    } else if (twoFactorEnabled(actor)) {
      twoFactorLoginResponse = twoFactorStepUpLogin(actor, request);
      requestWithStepUpParams =
          objectMapper
              .readValue((String) twoFactorLoginResponse.state.get("stepUpRequest"), requestClass)
              .withTwoFactorParams(
                  request.getTrustChallenge(),
                  request.getTwoFactorId(),
                  request.getTwoFactorCode());
    } else {
      requestWithStepUpParams = request;
      twoFactorLoginResponse = null;
    }

    operation.accept(
        requestWithStepUpParams,
        Optional.ofNullable(twoFactorLoginResponse).map(l -> l.trustToken).orElse(null));
    return Optional.empty();
  }

  /**
   * A user wants to change their own password, and they remember the old one.
   *
   * @param request For the initial request, just the user, old and new passwords need to be
   *     specified. Submit again to finalize, and then only the other parameters (trustChallenge,
   *     trustToken, and twoFactorCode) need to be submitted since the others are cached between
   *     calls.
   * @return null upon success, non-null if 2FA is needed
   */
  @PreAuthorize("isSelfOwned(#user)")
  @SneakyThrows
  public Optional<com.clearspend.capital.controller.type.twofactor.TwoFactorStartResponse>
      changePassword(FusionAuthUser user, final ChangePasswordRequest request) {
    return stepUpRequest(
        user,
        request,
        (updatedRequest, trustToken) -> {
          // Perform the password change
          final io.fusionauth.domain.api.user.ChangePasswordRequest changePasswordRequest =
              new io.fusionauth.domain.api.user.ChangePasswordRequest(
                  user.getEmail(),
                  updatedRequest.getCurrentPassword(),
                  updatedRequest.getNewPassword());
          changePasswordRequest.trustToken = trustToken;
          changePasswordRequest.trustChallenge = updatedRequest.getTrustChallenge();
          changePasswordRequest.applicationId = getApplicationId();
          validateResponse(client.changePasswordByIdentity(changePasswordRequest));
        });
  }

  @PreAuthorize("isSelfOwned(#request)")
  public Optional<com.clearspend.capital.controller.type.twofactor.TwoFactorStartResponse>
      addMethod(final FusionAuthUser user, final ChangeMethodRequest request) {
    requireTwoFactorEnabled(user);
    return stepUpRequest(
        user,
        request,
        (updatedRequest, trustToken) ->
            sendTwoFactorCodeToNewDestination(
                user.getFusionAuthId(),
                updatedRequest.getMethod(),
                updatedRequest.getDestination()));
  }

  private void requireTwoFactorEnabled(FusionAuthUser fusionAuthUser) {
    if (!twoFactorEnabled(fusionAuthUser)) {
      throw new InvalidRequestException("Enable two-factor first");
    }
  }

  @PreAuthorize("isSelfOwned(#request)")
  public Optional<com.clearspend.capital.controller.type.twofactor.TwoFactorStartResponse>
      removeMethod(final FusionAuthUser user, final ChangeMethodRequest request) {
    UUID userId = UUID.fromString(user.getSubjectRef());
    requireTwoFactorEnabled(user);
    return stepUpRequest(
        user,
        request,
        (updatedRequest, trustToken) -> {
          String recoveryCode =
              validateResponse(client.retrieveTwoFactorRecoveryCodes(userId)).recoveryCodes.stream()
                  .findAny()
                  .or(
                      () ->
                          validateResponse(client.generateTwoFactorRecoveryCodes(userId))
                              .recoveryCodes
                              .stream()
                              .findAny())
                  .orElseThrow();
          User faUser = getUser(userId);
          String methodId =
              faUser.twoFactor.methods.stream()
                  .filter(updatedRequest::matches)
                  .map(m -> m.id)
                  .findFirst()
                  .orElseThrow(() -> new NoSuchElementException("2 factor method not found"));
          List<TwoFactorMethod> remainingMethods =
              faUser.twoFactor.methods.stream().filter(m -> !m.id.equals(methodId)).toList();

          // Using the recovery code wipes all methods
          // Not doing so requires they get the code from the old number
          client.disableTwoFactor(userId, methodId, recoveryCode);

          // If we didn't remove the last method, put the others back
          if (!remainingMethods.isEmpty()) {
            client.patchUser(
                userId, Map.of("user", Map.of("twoFactor", Map.of("methods", remainingMethods))));
          }
        });
  }

  /**
   * Change an expired password
   *
   * @param changePasswordId The ID of the change request
   * @param loginId The user's email
   * @param currentPassword The current password
   * @param password The new password
   * @return the FusionAuth ChangePasswordResponse
   */
  @RestrictedApi(
      explanation = "This should only ever be used by AuthenticationController",
      allowlistAnnotations = {FusionAuthUserModifier.class},
      link =
          "https://tranwall.atlassian.net/wiki/spaces/CAP/pages/2088828965/Dev+notes+Service+method+security")
  public ChangePasswordResponse changePassword(
      String changePasswordId, String loginId, String currentPassword, String password) {
    final io.fusionauth.domain.api.user.ChangePasswordRequest request =
        new io.fusionauth.domain.api.user.ChangePasswordRequest(loginId, currentPassword, password);
    request.applicationId = getApplicationId();
    ClientResponse<ChangePasswordResponse, Errors> changePasswordResponse =
        client.changePassword(changePasswordId, request);
    return validateResponse(changePasswordResponse);
  }

  @OpenAccessAPI(explanation = "This is the front door", reviewer = "jscarbor")
  public ClientResponse<LoginResponse, Errors> login(String loginId, @Sensitive String password) {
    LoginRequest request = new LoginRequest(getApplicationId(), loginId, password);
    return client.login(request);
  }

  @OpenAccessAPI(
      explanation = "Second step of login, authenticated by the twoFactorId provided",
      reviewer = "jscarbor")
  public void sendTwoFactorCodeUsingMethod(String twoFactorId, String methodId) {
    TwoFactorSendRequest request = new TwoFactorSendRequest(methodId);
    validateResponse(client.sendTwoFactorCodeForLoginUsingMethod(twoFactorId, request));
  }

  @PreAuthorize("hasPermission(#user, 'VIEW_OWN|MANAGE_USERS|CUSTOMER_SERVICE')")
  public boolean twoFactorEnabled(@NonNull FusionAuthUser user) {
    return getUser(user).twoFactorEnabled();
  }

  User getUser(@NonNull UUID fusionAuthUserId) {
    return coreFusionAuthService.getUser(fusionAuthUserId);
  }

  private Application getApplication() {
    return coreFusionAuthService.getApplication();
  }

  @OpenAccessAPI(explanation = "directory information about the app", reviewer = "jscarbor")
  public int getJWTRefreshTokenTimeToLiveInMinutes() {
    return getApplication().jwtConfiguration.refreshTokenTimeToLiveInMinutes;
  }

  @OpenAccessAPI(explanation = "directory information about the app", reviewer = "jscarbor")
  public UUID getApplicationId() {
    return coreFusionAuthService.getApplicationId();
  }

  @OpenAccessAPI(explanation = "processing specific to FusionAuth", reviewer = "jscarbor")
  public static <T> T validateResponse(ClientResponse<T, Errors> response) {
    return CoreFusionAuthService.validateResponse(response);
  }

  /**
   * For wrapping CurrentUser and User with a common interface (yes, the irony is not lost, but
   * CurrentUser can't implement because it has static implementations of some of the methods we
   * need on the instance) that contains the IDs for FusionAuth and permissions checking
   */
  @Value
  @AllArgsConstructor(access = AccessLevel.PRIVATE)
  public static class FusionAuthUser implements OwnerRelated {
    String subjectRef;
    TypedId<UserId> userId;
    TypedId<BusinessId> businessId;
    @Sensitive String email;

    public UUID getFusionAuthId() {
      return UUID.fromString(getSubjectRef());
    }

    @Override
    public TypedId<UserId> getOwnerId() {
      return getUserId();
    }

    public static FusionAuthUser fromCurrentUser() {
      return new FusionAuthUser(
          CurrentUser.getFusionAuthUserId().toString(),
          CurrentUser.getUserId(),
          CurrentUser.get().homeBusinessId(),
          CurrentUser.getEmail());
    }

    public static FusionAuthUser fromUser(com.clearspend.capital.data.model.User user) {
      return new FusionAuthUser(
          user.getSubjectRef(), user.getId(), user.getBusinessId(), user.getEmail().getEncrypted());
    }
  }
}
