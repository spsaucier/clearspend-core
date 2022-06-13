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
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.annotation.Nullable;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
public record CurrentUser(
    UserType userType,
    TypedId<UserId> userId,
    TypedId<BusinessId> homeBusinessId,
    Set<String> roles) {

  @SneakyThrows
  @Nullable
  public static CurrentUser get() {
    return getClaims().map(CurrentUser::get).orElse(null);
  }

  public static CurrentUser get(Map<String, Object> claims) {
    final UserType userType =
        Optional.ofNullable(claims.get(USER_TYPE))
            .map(Object::toString)
            .map(UserType::valueOf)
            .orElse(null);
    final TypedId<UserId> userId =
        Optional.ofNullable(claims.get(CAPITAL_USER_ID))
            .map(Object::toString)
            .map(value -> new TypedId<UserId>(value))
            .orElse(null);
    final TypedId<BusinessId> businessId =
        Optional.ofNullable(claims.get(BUSINESS_ID))
            .map(Object::toString)
            .map(value -> new TypedId<BusinessId>(value))
            .orElse(null);
    final Set<String> globalRoles =
        Optional.ofNullable(claims.get(ROLES))
            .map(roles -> (Iterable<Object>) roles)
            .map(Iterable::spliterator)
            .map(spliterator -> StreamSupport.stream(spliterator, false))
            .stream()
            .flatMap(Function.identity())
            .map(Object::toString)
            .collect(Collectors.toUnmodifiableSet());

    return new CurrentUser(userType, userId, businessId, globalRoles);
  }

  @Nullable
  public static TypedId<BusinessId> getActiveBusinessId() {
    String businessIdHeader = null;
    try {
      businessIdHeader =
          ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
              .getRequest()
              .getHeader(BUSINESS_ID);
    } catch (Exception e) {
      log.warn("Being called outside of spring runtine environmnet", e);
    }

    return Optional.ofNullable(businessIdHeader)
        .map(TypedId<BusinessId>::new)
        .or(
            () ->
                Optional.ofNullable(getClaim(BUSINESS_ID)).map(Object::toString).map(TypedId::new))
        .orElse(null);
  }

  @Nullable
  public static TypedId<UserId> getUserId() {
    return Optional.ofNullable(getClaim(CAPITAL_USER_ID))
        .map(Object::toString)
        .map(value -> new TypedId<UserId>(value))
        .orElse(null);
  }

  @Nullable
  public static Set<String> getRoles() {
    return Optional.ofNullable(get()).map(CurrentUser::roles).orElse(null);
  }

  @Nullable
  public static UserType getUserType() {
    return Optional.ofNullable(getClaim(USER_TYPE))
        .map(Object::toString)
        .map(UserType::valueOf)
        .orElse(null);
  }

  @Nullable
  private static Object getClaim(String name) {
    return getClaims().map(claims -> claims.get(name)).orElse(null);
  }

  public static Optional<Map<String, Object>> getClaims() {
    // TODO https://github.com/fusionauth/fusionauth-jwt#verify-and-decode-a-jwt-using-hmac
    return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
        .map(auth -> (JwtAuthenticationToken) auth)
        .map(JwtAuthenticationToken::getToken)
        .map(Jwt::getClaims);
  }

  @Nullable
  public static UUID getFusionAuthUserId() {
    return getClaims()
        .map(claims -> claims.get(USER_ID))
        .map(String::valueOf)
        .map(UUID::fromString)
        .orElse(null);
  }

  @Nullable
  public static String getEmail() {
    return getClaims().map(claims -> claims.get(EMAIL)).map(String::valueOf).orElse(null);
  }
}
