package com.clearspend.capital.service.type;

import static com.clearspend.capital.controller.Common.BUSINESS_ID;
import static com.clearspend.capital.controller.Common.CAPITAL_USER_ID;
import static com.clearspend.capital.controller.Common.ROLES;
import static com.clearspend.capital.controller.Common.USER_TYPE;

import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.data.model.enums.UserType;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;
import com.nimbusds.jwt.SignedJWT;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

public record CurrentUser(
    UserType userType, TypedId<UserId> userId, TypedId<BusinessId> businessId, Set<String> roles) {

  @SneakyThrows
  public static CurrentUser get() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    JWT jwt = JWTParser.parse(((JwtAuthenticationToken) authentication).getToken().getTokenValue());
    Map<String, Object> jsonObject = ((SignedJWT) jwt).getPayload().toJSONObject();
    UserType userType = UserType.valueOf((String) jsonObject.get(USER_TYPE));
    String userId = (String) jsonObject.get(CAPITAL_USER_ID);
    String businessId = (String) jsonObject.get(BUSINESS_ID);

    // TODO verify CurrentUser.globalUserPermissions works.
    // From the JWT example
    // https://fusionauth.io/docs/v1/tech/core-concepts/authentication-authorization/ it looks right
    @SuppressWarnings("unchecked")
    List<Object> roleList = (List<Object>) jsonObject.get(ROLES);
    Set<String> roles =
        roleList == null
            ? Collections.emptySet()
            : roleList.stream().map(Object::toString).collect(Collectors.toUnmodifiableSet());

    return new CurrentUser(
        userType, new TypedId<>(userId), new TypedId<>(UUID.fromString(businessId)), roles);
  }
}
