package com.clearspend.capital.configuration;

import com.clearspend.capital.service.security.UserRolesAndPermissionsCache;
import java.util.Collection;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@EnableWebSecurity()
@Slf4j
public class SecurityConfig extends WebSecurityConfigurerAdapter {

  public static final String ACCESS_TOKEN_COOKIE_NAME = "X-Auth-Token";
  public static final String REFRESH_TOKEN_COOKIE_NAME = "X-Refresh-Token";

  @Override
  public void configure(WebSecurity web) {
    web.ignoring()
        .antMatchers(
            "/actuator/**",
            "/authentication/login",
            "/authentication/change-password/*",
            "/authentication/two-factor/login",
            "/authentication/forgot-password",
            "/authentication/reset-password",
            "/authentication/logout",
            "/business-prospects",
            "/business-prospects/*/password",
            "/business-prospects/*/phone",
            "/business-prospects/*/validate-identifier",
            "/business-prospects/*/*/resend-otp",
            "/non-production/**",
            "/stripe/webhook/**",
            "/codat-webhook/**",
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/**");
  }

  private static class RolesAndPermissionsCachingAuthentication extends JwtAuthenticationToken {

    private final AbstractAuthenticationToken token;
    private final UserRolesAndPermissionsCache userRolesAndPermissionsCache =
        new UserRolesAndPermissionsCache();

    RolesAndPermissionsCachingAuthentication(AbstractAuthenticationToken token, Jwt jwt) {
      super(jwt);
      this.token = token;
    }

    @Override
    public void setDetails(Object details) {
      log.warn("Not setting details " + details.getClass().getName());
    }

    @Override
    public Object getCredentials() {
      return token.getCredentials();
    }

    @Override
    public Object getDetails() {
      return userRolesAndPermissionsCache;
    }

    @Override
    public Object getPrincipal() {
      return token.getPrincipal();
    }

    @Override
    public Collection<GrantedAuthority> getAuthorities() {
      return token.getAuthorities();
    }

    @Override
    public String getName() {
      return token.getName();
    }

    @Override
    public boolean isAuthenticated() {
      return token.isAuthenticated();
    }

    @Override
    public void setAuthenticated(boolean authenticated) {
      token.setAuthenticated(authenticated);
    }

    @Override
    public void eraseCredentials() {
      token.eraseCredentials();
    }

    @Override
    public boolean equals(Object obj) {
      return token.equals(obj);
    }

    @Override
    public int hashCode() {
      return token.hashCode();
    }

    @Override
    public String toString() {
      return token.toString();
    }
  }

  public static class CapitalAuthenticationConverter
      implements Converter<Jwt, RolesAndPermissionsCachingAuthentication> {

    private final JwtAuthenticationConverter delegate = new JwtAuthenticationConverter();

    @Override
    public RolesAndPermissionsCachingAuthentication convert(@NonNull Jwt source) {
      final AbstractAuthenticationToken convert = delegate.convert(source);
      return new RolesAndPermissionsCachingAuthentication(convert, source);
    }
  }

  @Override
  protected void configure(HttpSecurity http) throws Exception {
    http // .addFilter(new BearerTokenAuthenticationFilter().setAuthenticationDetailsSource())
        .oauth2ResourceServer(
            c ->
                c.bearerTokenResolver(new CookieTokenResolver())
                    .jwt()
                    // This can be useful if we need to populate context with permissions from our
                    // DB
                    .jwtAuthenticationConverter(new CapitalAuthenticationConverter()))
        .sessionManagement()
        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        .and()
        .authorizeRequests()
        .antMatchers("/**")
        .authenticated();
  }
}
