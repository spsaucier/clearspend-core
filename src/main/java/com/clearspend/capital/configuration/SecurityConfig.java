package com.clearspend.capital.configuration;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {
  public static final String ACCESS_TOKEN_COOKIE_NAME = "X-Auth-Token";
  public static final String REFRESH_TOKEN_COOKIE_NAME = "X-Refresh-Token";

  @Override
  public void configure(WebSecurity web) {
    web.ignoring()
        .antMatchers(
            "/actuator/**",
            "/authentication/*",
            "/business-prospects",
            "/business-prospects/*/password",
            "/business-prospects/*/phone",
            "/business-prospects/*/validate-identifier",
            "/i2c/push/**",
            "/manual-review/alloy/web-hook",
            "/non-production/**",
            "/stripe/webhook/**",
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/**");
  }

  @Override
  protected void configure(HttpSecurity http) throws Exception {
    http.oauth2ResourceServer(
            c ->
                c.bearerTokenResolver(new CookieTokenResolver())
                    .jwt()
                    // This can be useful if we need to populate context with permissions from our
                    // DB
                    .jwtAuthenticationConverter(new JwtAuthenticationConverter()))
        .sessionManagement()
        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        .and()
        .authorizeRequests()
        .antMatchers("/**")
        .authenticated();
  }
}
