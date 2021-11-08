package com.tranwall.capital.configuration;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.web.util.WebUtils;

/**
 * Provides support for getting OAuth token from the auth cookie that is supplied by UI. In order to
 * maintain OAuth compatibility fallback to the default resolver if cookie is not found
 */
public class CookieTokenResolver implements BearerTokenResolver {

  private static final DefaultBearerTokenResolver defaultTokenResolver =
      new DefaultBearerTokenResolver();

  @Override
  public String resolve(HttpServletRequest request) {
    Cookie cookie = WebUtils.getCookie(request, SecurityConfig.ACCESS_TOKEN_COOKIE_NAME);
    // Ideally it should contain && cookie.getSecure() condition
    // but in order to make local swagger work without https keep this check less strict
    if (cookie != null && cookie.isHttpOnly()) {
      return cookie.getValue();
    } else {
      return defaultTokenResolver.resolve(request);
    }
  }
}
