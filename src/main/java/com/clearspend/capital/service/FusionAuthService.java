package com.clearspend.capital.service;

import com.clearspend.capital.client.fusionauth.FusionAuthProperties;
import com.clearspend.capital.common.error.FusionAuthException;
import com.clearspend.capital.common.error.InvalidRequestException;
import com.clearspend.capital.common.masking.annotation.Sensitive;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.common.typedid.data.business.BusinessOwnerId;
import com.clearspend.capital.controller.Common;
import com.clearspend.capital.controller.type.user.ForgotPasswordRequest;
import com.clearspend.capital.controller.type.user.ResetPasswordRequest;
import com.clearspend.capital.data.model.enums.UserType;
import com.clearspend.capital.permissioncheck.annotations.OpenAccessAPI;
import com.clearspend.capital.service.type.CurrentUser;
import com.google.errorprone.annotations.RestrictedApi;
import com.inversoft.error.Errors;
import com.inversoft.rest.ClientResponse;
import io.fusionauth.domain.Application;
import io.fusionauth.domain.ChangePasswordReason;
import io.fusionauth.domain.TwoFactorMethod;
import io.fusionauth.domain.User;
import io.fusionauth.domain.UserRegistration;
import io.fusionauth.domain.api.ApplicationResponse;
import io.fusionauth.domain.api.LoginRequest;
import io.fusionauth.domain.api.LoginResponse;
import io.fusionauth.domain.api.TwoFactorRequest;
import io.fusionauth.domain.api.TwoFactorResponse;
import io.fusionauth.domain.api.UserRequest;
import io.fusionauth.domain.api.UserResponse;
import io.fusionauth.domain.api.twoFactor.TwoFactorLoginRequest;
import io.fusionauth.domain.api.twoFactor.TwoFactorSendRequest;
import io.fusionauth.domain.api.twoFactor.TwoFactorStartRequest;
import io.fusionauth.domain.api.twoFactor.TwoFactorStartResponse;
import io.fusionauth.domain.api.user.ChangePasswordResponse;
import io.fusionauth.domain.api.user.ForgotPasswordResponse;
import io.fusionauth.domain.api.user.RegistrationRequest;
import io.fusionauth.domain.api.user.RegistrationResponse;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class FusionAuthService {

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

  private static final String BUSINESS_ID = "businessId";
  private static final String CAPITAL_USER_ID = "capitalUserId";
  private static final String USER_TYPE = "userType";
  private static final String ROLES = "roles";

  private final io.fusionauth.client.FusionAuthClient client;
  private final FusionAuthProperties fusionAuthProperties;

  private final TwilioService twilioService;

  @RestrictedApi(
      explanation = "This should only ever be used by UserService",
      allowedOnPath = "test/.*",
      allowlistAnnotations = {FusionAuthUserCreator.class},
      link =
          "https://tranwall.atlassian.net/wiki/spaces/CAP/pages/2088828965/Dev+notes+Service+method+security")
  public UUID createBusinessOwner(
      TypedId<BusinessId> businessId,
      TypedId<BusinessOwnerId> businessOwnerId,
      String username,
      String password) {
    return createUser(
        businessId,
        new TypedId<>(businessOwnerId.toUuid()),
        username,
        password,
        UserType.BUSINESS_OWNER,
        Optional.empty());
  }

  @RestrictedApi(
      explanation = "This should only ever be used by UserService",
      allowedOnPath = "/test/.*",
      allowlistAnnotations = {FusionAuthUserCreator.class},
      link =
          "https://tranwall.atlassian.net/wiki/spaces/CAP/pages/2088828965/Dev+notes+Service+method+security")
  public UUID createUser(
      TypedId<BusinessId> businessId, TypedId<UserId> userId, String email, String password) {
    return createUser(businessId, userId, email, password, UserType.EMPLOYEE, Optional.empty());
  }

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

  UUID createUser(
      TypedId<BusinessId> businessId,
      TypedId<UserId> userId,
      String email,
      String password,
      UserType userType,
      Optional<CapitalChangePasswordReason> changePasswordReason) {

    UUID fusionAuthId = UUID.randomUUID();

    RegistrationRequest request =
        new RegistrationRequest(
            userFactory(
                email,
                password,
                fusionAuthId,
                changePasswordReason.map(CapitalChangePasswordReason::getFusionAuthReason)),
            userRegistrationFactory(
                businessId, userId.toUuid(), userType, fusionAuthId, Collections.emptySet()));
    validateResponse(client.register(fusionAuthId, request));
    return fusionAuthId;
  }

  private UserRegistration userRegistrationFactory(
      TypedId<BusinessId> businessId,
      UUID userId,
      UserType userType,
      UUID fusionAuthId,
      Set<String> roles) {
    UserRegistration registration = new UserRegistration();
    registration.data.put(Common.CAPITAL_USER_ID, userId.toString());
    registration.data.put(BUSINESS_ID, businessId.toUuid());
    registration.data.put(USER_TYPE, userType.name());
    registration.data.put(ROLES, roles.toArray(String[]::new));
    registration.applicationId = getApplicationId();
    registration.id = fusionAuthId;
    return registration;
  }

  private static User userFactory(String email, String password, UUID fusionAuthId) {
    return userFactory(email, password, fusionAuthId, Optional.empty());
  }

  private static User userFactory(
      String email,
      String password,
      @NonNull UUID fusionAuthId,
      Optional<ChangePasswordReason> changePasswordReason) {
    User user = new User();

    user.id = fusionAuthId;

    Optional.ofNullable(email).ifPresent(s -> user.email = s);
    Optional.ofNullable(password).ifPresent(s -> user.password = s);

    changePasswordReason.ifPresent(
        r -> {
          user.passwordChangeRequired = true;
          user.passwordChangeReason = r;
        });

    return user;
  }

  @RestrictedApi(
      explanation = "This should only ever be used by RolesAndPermissionsService",
      allowedOnPath = "test/.*",
      allowlistAnnotations = {FusionAuthUserAccessor.class},
      link =
          "https://tranwall.atlassian.net/wiki/spaces/CAP/pages/2088828965/Dev+notes+Service+method+security")
  public Set<String> getUserRoles(UUID fusionAuthUserId) {
    return getUserRoles(getUser(fusionAuthUserId));
  }

  @SuppressWarnings("unchecked")
  private Set<String> getUserRoles(User user) {
    return Set.copyOf((List<String>) user.data.getOrDefault(ROLES, Collections.emptyList()));
  }

  /**
   * "/api/two-factor/login"
   *
   * @param request with parameters including a code and twoFactorId
   * @return the User if successful, several codes in the 400s for bad submissions
   */
  @RestrictedApi(
      explanation = "This should only ever be used by AuthenticationController",
      allowlistAnnotations = {FusionAuthUserAccessor.class, FusionAuthUserModifier.class},
      link =
          "https://tranwall.atlassian.net/wiki/spaces/CAP/pages/2088828965/Dev+notes+Service+method+security")
  public ClientResponse<LoginResponse, Errors> twoFactorLogin(TwoFactorLoginRequest request) {
    final ClientResponse<LoginResponse, Errors> response = client.twoFactorLogin(request);
    validateResponse(response);
    return response;
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
    TwoFactorSendRequest request = new TwoFactorSendRequest();
    request.method = method.name();
    switch (method) {
      case email -> request.email = destination;
      case sms -> request.mobilePhone = destination;
      case authenticator -> throw new InvalidRequestException(
          "Authenticator does not support sending");
    }
    request.userId = fusionAuthUserId;

    validateResponse(client.sendTwoFactorCodeForEnableDisable(request));
  }

  @RestrictedApi(
      explanation = "Only for preparing 2FA cycle for disabling 2FA or changing password",
      link =
          "https://tranwall.atlassian.net/wiki/spaces/CAP/pages/2088828965/Dev+notes+Service+method+security",
      allowlistAnnotations = {FusionAuthUserModifier.class})
  public TwoFactorStartResponse startTwoFactorLogin(
      com.clearspend.capital.data.model.User user,
      Map<String, Object> state,
      String trustChallenge) {
    TwoFactorStartRequest request = new TwoFactorStartRequest();
    request.trustChallenge = trustChallenge;
    request.applicationId = getApplicationId();
    request.loginId = user.getEmail().getEncrypted();
    request.state = Optional.ofNullable(state).orElse(Collections.emptyMap());
    return validateResponse(client.startTwoFactorLogin(request));
  }

  /**
   * Preceded by {@link #startTwoFactorLogin(com.clearspend.capital.data.model.User, Map, String)}
   *
   * @param userId The FusionAuth User ID (clearspend User.getSubjectRef())
   * @param methodId the method ID to disable (from the request initiating the flow)
   * @param code the 2FA code
   */
  @RestrictedApi(
      explanation = "Only for turning off 2FA",
      link =
          "https://tranwall.atlassian.net/wiki/spaces/CAP/pages/2088828965/Dev+notes+Service+method+security",
      allowlistAnnotations = {FusionAuthUserModifier.class})
  public void disableTwoFactor(UUID userId, String methodId, String code) {
    client.disableTwoFactor(userId, methodId, code);
  }

  @RestrictedApi(
      explanation = "This should only ever be used by AuthenticationController",
      allowlistAnnotations = {FusionAuthUserModifier.class},
      link =
          "https://tranwall.atlassian.net/wiki/spaces/CAP/pages/2088828965/Dev+notes+Service+method+security")
  public TwoFactorResponse validateFirstTwoFactorCode(
      @NonNull UUID fusionAuthUserId,
      @NonNull String code,
      @NonNull FusionAuthService.TwoFactorAuthenticationMethod method,
      @NonNull String destination) {
    if (!fusionAuthUserId.equals(CurrentUser.getFusionAuthUserId())) {
      throw new AccessDeniedException("");
    }

    TwoFactorRequest request = new TwoFactorRequest();
    request.code = code;
    request.method = method.name();
    switch (method) {
      case email -> request.email = destination;
      case sms -> request.mobilePhone = destination;
      case authenticator -> request.secret = destination;
    }
    return validateResponse(client.enableTwoFactor(fusionAuthUserId, request));
  }

  public enum RoleChange {
    GRANT,
    REVOKE
  }

  /**
   * @param change Either GRANT or REVOKE.
   * @param fusionAuthUserId the FusionAuth user ID to change.
   * @param changingRole the role to change.
   * @return true if the role is granted, false if it was already there
   */
  @RestrictedApi(
      explanation = "This should only ever be used by RolesAndPermissionsService",
      allowedOnPath = "/test/.*",
      allowlistAnnotations = {FusionAuthRoleAdministrator.class},
      link =
          "https://tranwall.atlassian.net/wiki/spaces/CAP/pages/2088828965/Dev+notes+Service+method+security")
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

  @FusionAuthUserModifier(reviewer = "jscarbor", explanation = "Delegation")
  UUID updateUser(com.clearspend.capital.data.model.User user, @Nullable String password) {
    return updateUser(
        user.getBusinessId(),
        user.getUserId(),
        user.getEmail().getEncrypted(),
        password,
        user.getType(),
        user.getSubjectRef());
  }

  /**
   * @param businessId the user's business
   * @param userId capital's number
   * @param email null for no change, correct value will be persisted
   * @param password null for no change, correct value will be persisted
   * @param userType the compiler will tell you if this is wrong.
   * @param fusionAuthUserIdStr fusionAuth's UUID for the user (subjectRef)
   * @return the FusionAuth UUID for the user
   */
  @FusionAuthUserAccessor(reviewer = "jscarbor", explanation = "Keeping roles consistent")
  @RestrictedApi(
      explanation = "This should only ever be used by UserService",
      allowedOnPath = "/test/.*",
      allowlistAnnotations = {FusionAuthUserModifier.class, FusionAuthUserCreator.class},
      link =
          "https://tranwall.atlassian.net/wiki/spaces/CAP/pages/2088828965/Dev+notes+Service+method+security")
  public UUID updateUser(
      TypedId<BusinessId> businessId,
      @NonNull TypedId<UserId> userId,
      @Sensitive String email,
      @Sensitive String password,
      UserType userType,
      @NonNull String fusionAuthUserIdStr) {
    UUID fusionAuthUserId = UUID.fromString(fusionAuthUserIdStr);

    User user = userFactory(email, password, fusionAuthUserId);
    if (email != null || password != null) {

      final ClientResponse<UserResponse, Errors> response =
          client.updateUser(fusionAuthUserId, new UserRequest(user));

      validateResponse(response);
    }

    final ClientResponse<RegistrationResponse, Errors> response1 =
        client.updateRegistration(
            fusionAuthUserId,
            new RegistrationRequest(
                user,
                userRegistrationFactory(
                    businessId,
                    userId.toUuid(),
                    userType,
                    fusionAuthUserId,
                    getUserRoles(fusionAuthUserId))));

    validateResponse(response1);

    return fusionAuthUserId;
  }

  private <T> T validateResponse(ClientResponse<T, Errors> response) {
    if (response.wasSuccessful()) {
      return response.successResponse;
    }

    throw new FusionAuthException(response.status, response.errorResponse, response.exception);
  }

  @RestrictedApi(
      explanation = "User information is generally PII",
      allowlistAnnotations = {FusionAuthUserAccessor.class},
      link =
          "https://tranwall.atlassian.net/wiki/spaces/CAP/pages/2088828965/Dev+notes+Service+method+security")
  public User retrieveUserByEmail(String email) {
    ClientResponse<UserResponse, Errors> userResponseErrorsClientResponse =
        client.retrieveUserByEmail(email);
    return validateResponse(userResponseErrorsClientResponse).user;
  }

  /**
   * @param user the ClearSpend user of interest
   * @return the FusionAuth user record
   */
  User getUser(com.clearspend.capital.data.model.User user) {
    return getUser(UUID.fromString(user.getSubjectRef()));
  }

  private User getUser(UUID fusionAuthUserId) {
    ClientResponse<UserResponse, Errors> user = client.retrieveUser(fusionAuthUserId);
    return validateResponse(user).user;
  }

  @OpenAccessAPI(
      explanation = "Users need to be able to reset their password not logged in",
      reviewer = "jscarbor")
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

  @OpenAccessAPI(
      explanation =
          "Users are authenticated in this call by a changePasswordId sent to their email",
      reviewer = "jscarbor")
  public void resetPassword(ResetPasswordRequest request) {

    ClientResponse<UserResponse, Errors> fusionAuthUser =
        client.retrieveUserByChangePasswordId(request.getChangePasswordId());

    ClientResponse<ChangePasswordResponse, Errors> changePasswordResponse =
        client.changePassword(
            request.getChangePasswordId(),
            new io.fusionauth.domain.api.user.ChangePasswordRequest(request.getNewPassword()));

    switch (changePasswordResponse.status) {
      case 200 -> {
        twilioService.sendPasswordResetSuccessEmail(fusionAuthUser.successResponse.user.email, "");
      }
      case 404 -> throw new AccessDeniedException("");
      case 500 -> throw new RuntimeException(
          "FusionAuth internal error", changePasswordResponse.exception);
      default -> throw new RuntimeException(
          "unknown reset password status: " + changePasswordResponse.status);
    }
  }

  /**
   * Some 2FA actions are done while the user is logged in. These have a twoFactorId and possibly a
   * methodId which could be needed for follow-up with the code.
   */
  public record TwoFactorStartLoggedInResponse(
      String twoFactorId, String methodId, String trustChallenge) {}

  /**
   * This is for things requiring a very fresh code, such as disabling 2FA and changing password.
   * This is not how a 2FA login begins - that begins the same as every other login.
   *
   * @param user the User object corresponding to CurrentUser
   * @param state any state to be returned after successful authentication
   * @return codes required to proceed
   */
  @RestrictedApi(
      explanation =
          "This should only ever be used by AuthenticationController to step up"
              + "permissions for an imminent risky operation",
      allowlistAnnotations = {FusionAuthUserModifier.class},
      link =
          "https://tranwall.atlassian.net/wiki/spaces/CAP/pages/2088828965/Dev+notes+Service+method+security")
  @FusionAuthUserModifier(reviewer = "jscarbor", explanation = "calling other restricted functions")
  public TwoFactorStartLoggedInResponse sendCodeToBegin2FA(
      com.clearspend.capital.data.model.User user, Map<String, Object> state) {
    if (!user.getId().equals(CurrentUser.getUserId())) {
      throw new IllegalArgumentException("user");
    }
    String trustChallenge = RandomStringUtils.randomPrint(24);
    TwoFactorStartResponse initResponse = startTwoFactorLogin(user, state, trustChallenge);
    final String methodId =
        initResponse.methods.stream()
            .filter(m -> m.method.equals(TwoFactorMethod.SMS))
            .findFirst()
            .map(m -> m.id)
            .orElseThrow();
    sendTwoFactorCodeUsingMethod(initResponse.twoFactorId, methodId);
    return new TwoFactorStartLoggedInResponse(initResponse.twoFactorId, methodId, trustChallenge);
  }

  public record ChangePasswordRequest(
      com.clearspend.capital.data.model.User user,
      String currentPassword,
      String newPassword,
      String trustChallenge,
      String twoFactorId,
      String twoFactorCode) {}

  /**
   * A user wants to change their own password and they remember the old one.
   *
   * @param request For the initial request, just the user, old and new passwords need to be
   *     specified. Submit again to finalize, and then only the other parameters (trustChallenge,
   *     trustToken, and twoFactorCode) need to be submitted since the others are cached between
   *     calls.
   * @return null upon success, non-null if 2FA is needed
   */
  @RestrictedApi(
      explanation =
          "This should only ever be used by AuthenticationController, and only "
              + "after validating that the logged-in user is making the change.",
      allowlistAnnotations = {FusionAuthUserModifier.class},
      link =
          "https://tranwall.atlassian.net/wiki/spaces/CAP/pages/2088828965/Dev+notes+Service+method+security")
  @FusionAuthUserModifier(
      reviewer = "jscarbor",
      explanation = "delegating some work within the class")
  public TwoFactorStartLoggedInResponse changePassword(ChangePasswordRequest request) {
    LoginResponse twoFactorLogin = null;
    if (twoFactorEnabled()) {
      if (StringUtils.isAnyEmpty(
          request.trustChallenge, request.twoFactorId, request.twoFactorCode)) {
        return sendCodeToBegin2FA(request.user, Map.of("changeRequest", request));
      }
      TwoFactorLoginRequest twoFactorLoginRequest =
          new TwoFactorLoginRequest(getApplicationId(), request.twoFactorCode, request.twoFactorId);
      ClientResponse<LoginResponse, Errors> twoFactorLoginResponse =
          twoFactorLogin(twoFactorLoginRequest);
      if (!twoFactorLoginResponse.wasSuccessful()) {
        throw new InvalidRequestException("Unauthorized");
      }
      twoFactorLogin = twoFactorLoginResponse.successResponse;
      ChangePasswordRequest oldRequest =
          (ChangePasswordRequest) twoFactorLogin.state.get("changeRequest");
      request =
          new ChangePasswordRequest(
              request.user,
              oldRequest.currentPassword,
              oldRequest.newPassword,
              request.trustChallenge,
              request.twoFactorId,
              request.twoFactorCode);
    }

    final io.fusionauth.domain.api.user.ChangePasswordRequest changePasswordRequest =
        new io.fusionauth.domain.api.user.ChangePasswordRequest(
            request.user.getEmail().getEncrypted(), request.currentPassword, request.newPassword);
    changePasswordRequest.trustToken =
        Optional.ofNullable(twoFactorLogin).map(l -> l.trustToken).orElse(null);
    changePasswordRequest.trustChallenge = request.trustChallenge;
    changePasswordRequest.applicationId = getApplicationId();
    validateResponse(client.changePasswordByIdentity(changePasswordRequest));
    return null;
  }

  /**
   * Change an expired password
   *
   * @param changePasswordId The ID of the change request
   * @param loginId The user's email
   * @param currentPassword The current password
   * @param password The new password
   * @return
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

  @OpenAccessAPI(
      explanation = "This is directory information about the application",
      reviewer = "jscarbor")
  public UUID getApplicationId() {
    return UUID.fromString(fusionAuthProperties.getApplicationId());
  }

  @OpenAccessAPI(explanation = "Returns info about the current user only", reviewer = "jscarbor")
  public boolean twoFactorEnabled() {
    return getUser(CurrentUser.getFusionAuthUserId()).twoFactorEnabled();
  }

  public Application getApplication() {
    ClientResponse<ApplicationResponse, Void> response =
        client.retrieveApplication(getApplicationId());
    if (response.wasSuccessful()) {
      return response.successResponse.application;
    }
    throw new InvalidRequestException(String.valueOf(response.status), response.exception);
  }

  public void updateEmail(com.clearspend.capital.data.model.User user, String oldEmail) {}
}
