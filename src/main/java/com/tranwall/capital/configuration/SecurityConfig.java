package com.tranwall.capital.configuration;

import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

@Profile("!test")
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {
  public static final String ACCESS_TOKEN_COOKIE_NAME = "X-Auth-Token";
  public static final String REFRESH_TOKEN_COOKIE_NAME = "X-Refresh-Token";

  @Override
  public void configure(WebSecurity web) {
    web.ignoring()
        .antMatchers(
            "/authentication/*",
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/**",
            "/business-prospects",
            "/business-prospects/*/validate-identifier",
            "/business-prospects/*/phone",
            "/business-prospects/*/password",
            "/non-production/**");
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
        .sessionCreationPolicy(SessionCreationPolicy.NEVER)
        .and()
        .authorizeRequests()
        .antMatchers("/**")
        .authenticated();
  }
}
