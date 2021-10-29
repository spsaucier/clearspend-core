package com.tranwall.capital.controller.type;

import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;
import com.nimbusds.jwt.SignedJWT;
import com.tranwall.capital.common.typedid.data.BusinessId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.service.FusionAuthService;
import java.util.UUID;
import lombok.SneakyThrows;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

public record CurrentUser(TypedId<BusinessId> businessId) {

  @SneakyThrows
  public static CurrentUser get() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    JWT jwt = JWTParser.parse(((JwtAuthenticationToken) authentication).getToken().getTokenValue());
    String businessId =
        (String)
            ((SignedJWT) jwt).getPayload().toJSONObject().get(FusionAuthService.BUSINESS_ID_KEY);
    return new CurrentUser(new TypedId<>(UUID.fromString(businessId)));
  }
}
