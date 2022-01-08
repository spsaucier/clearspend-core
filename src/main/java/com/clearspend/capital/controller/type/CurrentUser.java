package com.clearspend.capital.controller.type;

import static com.clearspend.capital.controller.Common.BUSINESS_ID;
import static com.clearspend.capital.controller.Common.CAPITAL_USER_ID;
import static com.clearspend.capital.controller.Common.USER_TYPE;

import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.data.model.enums.UserType;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;
import com.nimbusds.jwt.SignedJWT;
import java.util.Map;
import java.util.UUID;
import lombok.SneakyThrows;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

public record CurrentUser(
    UserType userType, TypedId<UserId> userId, TypedId<BusinessId> businessId) {

  @SneakyThrows
  public static CurrentUser get() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    JWT jwt = JWTParser.parse(((JwtAuthenticationToken) authentication).getToken().getTokenValue());
    Map<String, Object> jsonObject = ((SignedJWT) jwt).getPayload().toJSONObject();
    UserType userType = UserType.valueOf((String) jsonObject.get(USER_TYPE));
    String userId = (String) jsonObject.get(CAPITAL_USER_ID);
    String businessId = (String) jsonObject.get(BUSINESS_ID);
    return new CurrentUser(
        userType, new TypedId<>(userId), new TypedId<>(UUID.fromString(businessId)));
  }
}
