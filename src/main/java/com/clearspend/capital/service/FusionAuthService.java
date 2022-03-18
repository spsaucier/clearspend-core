package com.clearspend.capital.service;

import com.clearspend.capital.client.fusionauth.FusionAuthProperties;
import com.clearspend.capital.common.error.ForbiddenException;
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
import com.google.errorprone.annotations.RestrictedApi;
import com.inversoft.error.Errors;
import com.inversoft.rest.ClientResponse;
import io.fusionauth.domain.Application;
import io.fusionauth.domain.ChangePasswordReason;
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
import io.fusionauth.domain.api.user.ChangePasswordRequest;
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
import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

  private static User userFactory(
      Optional<String> email, Optional<String> password, UUID fusionAuthId) {
    return userFactory(email, password, fusionAuthId, Optional.empty());
  }

  private static User userFactory(
      @NonNull String email,
      @NonNull String password,
      @NonNull UUID fusionAuthId,
      @NonNull Optional<ChangePasswordReason> changePasswordReason) {
    return userFactory(
        Optional.of(email), Optional.of(password), fusionAuthId, changePasswordReason);
  }

  private static User userFactory(
      Optional<String> email,
      Optional<String> password,
      @NonNull UUID fusionAuthId,
      Optional<ChangePasswordReason> changePasswordReason) {
    User user = new User();

    user.id = fusionAuthId;

    email.ifPresent(s -> user.email = s);
    password.ifPresent(s -> user.password = s);

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
      explanation = "Only for sending 2FA code for initializing 2FA",
      link =
          "https://tranwall.atlassian.net/wiki/spaces/CAP/pages/2088828965/Dev+notes+Service+method+security",
      allowlistAnnotations = {FusionAuthUserModifier.class})
  public void sendInitialTwoFactorCode(
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
      com.clearspend.capital.data.model.User user, Map<String, Object> state) {
    TwoFactorStartRequest request = new TwoFactorStartRequest();
    request.applicationId = getApplicationId();
    request.loginId = user.getEmail().getEncrypted();
    request.state = Optional.ofNullable(state).orElse(Collections.emptyMap());
    return validateResponse(client.startTwoFactorLogin(request));
  }

  /**
   * Preceded by {@link #startTwoFactorLogin(com.clearspend.capital.data.model.User, Map)}
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
      explanation = "Only for starting 2FA cycle",
      link =
          "https://tranwall.atlassian.net/wiki/spaces/CAP/pages/2088828965/Dev+notes+Service+method+security",
      allowlistAnnotations = {FusionAuthUserModifier.class})
  public TwoFactorResponse validateFirstTwoFactorCode(
      @NonNull UUID fusionAuthUserId,
      @NonNull String code,
      @NonNull FusionAuthService.TwoFactorAuthenticationMethod method,
      @NonNull String destination) {

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
      @NonNull Optional<String> email,
      @NonNull @Sensitive Optional<String> password,
      UserType userType,
      String fusionAuthUserIdStr) {
    UUID fusionAuthUserId = UUID.fromString(fusionAuthUserIdStr);

    User user = userFactory(email, password, fusionAuthUserId);

    final ClientResponse<UserResponse, Errors> response =
        client.updateUser(fusionAuthUserId, new UserRequest(user));

    validateResponse(response);

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

  public User retrieveUserByEmail(String email) {
    ClientResponse<UserResponse, Errors> userResponseErrorsClientResponse =
        client.retrieveUserByEmail(email);
    return validateResponse(userResponseErrorsClientResponse).user;
  }

  public User getUser(UUID fusionAuthUserId) {
    ClientResponse<UserResponse, Errors> user = client.retrieveUser(fusionAuthUserId);
    return validateResponse(user).user;
  }

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

  public void resetPassword(ResetPasswordRequest request) {

    ClientResponse<UserResponse, Errors> fusionAuthUser =
        client.retrieveUserByChangePasswordId(request.getChangePasswordId());

    ClientResponse<ChangePasswordResponse, Errors> changePasswordResponse =
        client.changePassword(
            request.getChangePasswordId(), new ChangePasswordRequest(request.getNewPassword()));

    switch (changePasswordResponse.status) {
      case 200 -> twilioService.sendPasswordResetSuccessEmail(
          fusionAuthUser.successResponse.user.email, "");
      case 404 -> throw new ForbiddenException();
      case 500 -> throw new RuntimeException(
          "FusionAuth internal error", changePasswordResponse.exception);
      default -> throw new RuntimeException(
          "unknown reset password status: " + changePasswordResponse.status);
    }
  }

  public void changePassword(String loginId, String currentPassword, String password) {
    ClientResponse<Void, Errors> changePasswordResponse =
        client.changePasswordByIdentity(
            new ChangePasswordRequest(loginId, currentPassword, password));
    StringBuilder builder = new StringBuilder();
    builder.append("[status: ").append(changePasswordResponse.status);
    builder.append(", success response: ").append(changePasswordResponse.successResponse);
    builder.append(", error response: ").append(changePasswordResponse.errorResponse);
    builder.append(", successful:").append(changePasswordResponse.wasSuccessful());
    log.debug("clientResponse : {}", builder);
    if (changePasswordResponse.status == 404) {
      throw new InvalidRequestException("Incorrect password");
    }
    validateResponse(changePasswordResponse);
  }

  public ChangePasswordResponse changePassword(
      String changePasswordId, String loginId, String currentPassword, String password) {
    final ChangePasswordRequest request =
        new ChangePasswordRequest(loginId, currentPassword, password);
    request.applicationId = getApplicationId();
    ClientResponse<ChangePasswordResponse, Errors> changePasswordResponse =
        client.changePassword(changePasswordId, request);
    return validateResponse(changePasswordResponse);
  }

  public ClientResponse<LoginResponse, Errors> login(String loginId, @Sensitive String password) {
    LoginRequest request = new LoginRequest(getApplicationId(), loginId, password);
    return client.login(request);
  }

  public void sendTwoFactorCodeUsingMethod(String twoFactorId, String methodId) {
    TwoFactorSendRequest request = new TwoFactorSendRequest(methodId);
    validateResponse(client.sendTwoFactorCodeForLoginUsingMethod(twoFactorId, request));
  }

  public UUID getApplicationId() {
    return UUID.fromString(fusionAuthProperties.getApplicationId());
  }

  public Application getApplication() {
    ClientResponse<ApplicationResponse, Void> response =
        client.retrieveApplication(getApplicationId());
    if (response.wasSuccessful()) {
      return response.successResponse.application;
    }
    throw new InvalidRequestException(String.valueOf(response.status), response.exception);
  }
}
