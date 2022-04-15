package com.clearspend.capital.service.type;

import static com.clearspend.capital.controller.Common.BUSINESS_ID;
import static com.clearspend.capital.controller.Common.CAPITAL_USER_ID;
import static com.clearspend.capital.controller.Common.EMAIL;
import static com.clearspend.capital.controller.Common.ROLES;
import static com.clearspend.capital.controller.Common.USER_ID;
import static com.clearspend.capital.controller.Common.USER_TYPE;

import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.data.model.enums.UserType;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import lombok.SneakyThrows;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

public record CurrentUser(
    UserType userType, TypedId<UserId> userId, TypedId<BusinessId> businessId, Set<String> roles) {

  @SneakyThrows
  @SuppressWarnings("unchecked")
  public static CurrentUser get() {
    // TODO verify CurrentUser.globalUserPermissions works.
    // From the JWT example
    // https://fusionauth.io/docs/v1/tech/core-concepts/authentication-authorization/ it looks right

    return getClaims().map(CurrentUser::get).orElse(null);
  }

  public static CurrentUser get(Map<String, Object> claims) {
    return new CurrentUser(
        UserType.valueOf(claims.get(USER_TYPE).toString()),
        new TypedId<>(claims.get(CAPITAL_USER_ID).toString()),
        new TypedId<>(claims.get(BUSINESS_ID).toString()),
        StreamSupport.stream(
                ((Iterable<Object>) claims.getOrDefault(ROLES, Collections.emptyList()))
                    .spliterator(),
                false)
            .map(Object::toString)
            .collect(Collectors.toUnmodifiableSet()));
  }

  public static TypedId<BusinessId> getBusinessId() {
    return new TypedId<>(getClaim(BUSINESS_ID).toString());
  }

  public static TypedId<UserId> getUserId() {
    return new TypedId<>(getClaim(CAPITAL_USER_ID).toString());
  }

  public static Set<String> getRoles() {
    return get().roles();
  }

  public static UserType getUserType() {
    return UserType.valueOf(getClaim(USER_TYPE).toString());
  }

  private static Object getClaim(String name) {
    return getClaims().map(claims -> claims.get(name)).orElse(null);
  }

  private static Optional<Map<String, Object>> getClaims() {
    // TODO https://github.com/fusionauth/fusionauth-jwt#verify-and-decode-a-jwt-using-hmac
    return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
        .map(auth -> (JwtAuthenticationToken) auth)
        .map(JwtAuthenticationToken::getToken)
        .map(Jwt::getClaims);
  }

  public static UUID getFusionAuthUserId() {
    return getClaims()
        .map(claims -> claims.get(USER_ID))
        .map(String::valueOf)
        .map(UUID::fromString)
        .orElse(null);
  }

  public static String getEmail() {
    return getClaims().map(claims -> claims.get(EMAIL)).map(String::valueOf).orElse(null);
  }
}
