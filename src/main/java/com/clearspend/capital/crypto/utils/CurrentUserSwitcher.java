package com.clearspend.capital.crypto.utils;

import static com.clearspend.capital.controller.Common.BUSINESS_ID;
import static com.clearspend.capital.controller.Common.CAPITAL_USER_ID;
import static com.clearspend.capital.controller.Common.ROLES;
import static com.clearspend.capital.controller.Common.USER_TYPE;

import com.clearspend.capital.data.model.User;
import com.clearspend.capital.service.security.UserRolesAndPermissionsCache;
import com.clearspend.capital.service.type.CurrentUser;
import com.google.errorprone.annotations.RestrictedApi;
import io.fusionauth.jwt.Signer;
import io.fusionauth.jwt.domain.JWT;
import io.fusionauth.jwt.rsa.RSASigner;
import io.fusionauth.pem.domain.PEM;
import java.security.interfaces.RSAPublicKey;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

// https://github.com/FusionAuth/fusionauth-example-java-jwt/blob/12ab95e98724d6080bf77ab47fb7e58b861b84fb/src/main/java/io/fusionauth/example/jwt/Rsa.java

/**
 * Set the current user for tests that call services directly. For tests using MockMvc, see {link
 * com.clearspend.capital.TestHelper#login(...)}
 *
 * <p>This class is in /src/main instead of /src/test because {link
 * com.clearspend.capital.TestDataControllerTest#createTestData} needs to be able to set the current
 * user this way.
 */
@Component
@Profile("test")
public class CurrentUserSwitcher implements AuthenticationProvider {

  public @interface SwitchesCurrentUser {

    public String reviewer();

    public String explanation();
  }

  // Typically you'd pull the private and public keys from some secrets manager.
  private static String privateKey =
      "-----BEGIN RSA PRIVATE KEY-----\n"
          + "MIIEowIBAAKCAQEAtczL3CLCFzf7yYD1lVUP0a++WpyKq7qACdZBBv8uaaBsy4MJ\n"
          + "1bL1/KG57E/bxEbtpWdi7UqyEVYdAPtoGToDJI1wD91Ya7K8hFHjwG9eV+IXo6C6\n"
          + "+9D8l2NZefYQEvaoQsj8wpMRwmzd83NufDYq0gtiqlr9rJX3PCuEuzVcVa3aFQnl\n"
          + "TjmydEYhjLNDZxYKy/BrNXx7oeFXh9G+KfWgwYYEj4GcInzrCpm2TyRoq1hJa2cj\n"
          + "Lm9cdFq16TZdzB9Ml5vds/3V1xMu4K/hp6hW9QNrPqUv0HnmbU+OumIxQfVVtAMf\n"
          + "OB2nqFHvDh3el9RZUtr/m+h2q1d+j34txsmknQIDAQABAoIBACUV7uwsJv3HjNRx\n"
          + "OyfIX16+BA3F6z/W3rGjBEbiHVysviTRyVrPlACCGURMkh86/NWF/pc7apHAyE/f\n"
          + "+T9UJzn4pEiINbZakrxHXDdwQIWa+ImHiz09R5m2SZVojaTgrjhZeb5TdAb+YFR+\n"
          + "UqaCAkAw1GK+BwuC2BltIzlxTDGp2dYf17XvNJgSCU9CojJXUdRzfc9yIuae/f7s\n"
          + "hf6pQ7iNwlTTRdmatZPSO3xyoqKJI+mR4v3RD3EYUXx9Ej9N+uOMSYukVWRKDHX5\n"
          + "VqYyQpC1bQFbv79YUCzJA3x2O/YzVfv4Qn0XlEGX2NHFygZncuciIgvWZI6NOGoM\n"
          + "yL+SwmECgYEA771YzKXTgOdCXzyxtHTA+75cfx3cUzixa8y+6tTmT0hBs+6Ed1jU\n"
          + "Ze+FTYna8dxO7wtpvWAbj+LdND/kFqScxpIFtJfmQeh+uEYPpXCTtXHow/h0sp/Z\n"
          + "4fhC5XF4O/HG20NEL/jwVlnOeTjqUa2QermJKTnU70SxJ23WIZQpO6UCgYEAwiFu\n"
          + "J8U9JcnH5IaUyNpeBGQ9MUJFXerOq+jURJzmrV9F1HT+FpTpbkoX9w1cUvV92Hmn\n"
          + "LdviOQYmMjKbggeVoVstoCi2pWch1+/Smo0Ki0fvP6UnJHPkwGaM220mE8osNxab\n"
          + "nY0mSIiDYDO1RKTXe0g+OcQNWv77iK7urBVM05kCgYEAgr5TmD/zScBVHRU/oLdv\n"
          + "q5sUXKrPRn+LmsutbuoutBwm4Nb6XRAPvYvTugatZeuNDKa/wuyP+3dwxZb2cDbT\n"
          + "3SMzg1q300fVG+/xhnsPwwtdMsrj8aplq+u1p+Wq+Z4QofFcVYFAqQcPKGMWvvaV\n"
          + "7agTmm0UqGxg5Kzs86EWn5UCgYBa5YhMet/8t5Qu/ozf1s86wyybyens5arLd4Mc\n"
          + "dVLVTszFg9oM/MCn8W1zMgBOn7/DAoMyx8gO50AvNN73bVG0cZrVaPrMS5PfJd24\n"
          + "m92aBZ6ScGP5f4JWTC4b9+liTsGRMba/eH26bKEHBG8VqxFyGgt4xMF0H/vqre33\n"
          + "CSol0QKBgDvuB4UBv15Kj4va1oauuxUT0ShtzhGV01TcOyDq+zFdx7CeexSYkgK0\n"
          + "NcZonN2+05Mtoo+RfC4l8mxiv6Du9N6VurozbjnUV5Yp8G/EPoN0EJqxc0IdusbA\n"
          + "MjX7bKM98tHqd+MrBii3c4LyWgMEtNIwk9pXapW5yxLjjKQnD93k\n"
          + "-----END RSA PRIVATE KEY-----";
  private static String publicKey =
      "-----BEGIN PUBLIC KEY-----\n"
          + "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAtczL3CLCFzf7yYD1lVUP\n"
          + "0a++WpyKq7qACdZBBv8uaaBsy4MJ1bL1/KG57E/bxEbtpWdi7UqyEVYdAPtoGToD\n"
          + "JI1wD91Ya7K8hFHjwG9eV+IXo6C6+9D8l2NZefYQEvaoQsj8wpMRwmzd83NufDYq\n"
          + "0gtiqlr9rJX3PCuEuzVcVa3aFQnlTjmydEYhjLNDZxYKy/BrNXx7oeFXh9G+KfWg\n"
          + "wYYEj4GcInzrCpm2TyRoq1hJa2cjLm9cdFq16TZdzB9Ml5vds/3V1xMu4K/hp6hW\n"
          + "9QNrPqUv0HnmbU+OumIxQfVVtAMfOB2nqFHvDh3el9RZUtr/m+h2q1d+j34txsmk\n"
          + "nQIDAQAB\n"
          + "-----END PUBLIC KEY-----";

  @RestrictedApi(
      explanation = "This is only for manipulating the CurrentUser during testing.",
      link =
          "https://tranwall.atlassian.net/wiki/spaces/CAP/pages/2088828965/Dev+notes+Service+method+security",
      allowedOnPath = "/test/.*",
      allowlistAnnotations = {SwitchesCurrentUser.class})
  public static void setCurrentUser(CurrentUser user) {
    SecurityContextHolder.getContext().setAuthentication(mockAuthentication(user));
  }

  @RestrictedApi(
      explanation = "This is only for manipulating the CurrentUser during testing.",
      link =
          "https://tranwall.atlassian.net/wiki/spaces/CAP/pages/2088828965/Dev+notes+Service+method+security",
      allowedOnPath = "/test/.*",
      allowlistAnnotations = {SwitchesCurrentUser.class})
  public static void clearCurrentUser() {
    SecurityContextHolder.clearContext();
  }

  private static Authentication mockAuthentication(CurrentUser user) {
    Signer signer = RSASigner.newSHA256Signer(privateKey);
    NimbusJwtDecoder jwtDecoder =
        NimbusJwtDecoder.withPublicKey((RSAPublicKey) PEM.decode(publicKey).publicKey).build();

    // Build a new JWT with an issuer(iss), issued at(iat), subject(sub) and expiration(exp)
    JWT jwt =
        new JWT()
            .setIssuer("fusionauth.io")
            .setIssuedAt(ZonedDateTime.now(ZoneOffset.UTC))
            .setAudience("238d4793-70de-4183-9707-48ed8ecd19d9")
            .setSubject("19016b73-3ffa-4b26-80d8-aa9287738677")
            .setExpiration(ZonedDateTime.now(ZoneOffset.UTC).plusMinutes(5));

    jwt.getOtherClaims().put("name", "Dan Moore");
    jwt.getOtherClaims().put(ROLES, new ArrayList<>(user.roles()).toArray(String[]::new));
    jwt.getOtherClaims().put(USER_TYPE, user.userType());
    jwt.getOtherClaims().put(CAPITAL_USER_ID, user.userId());
    jwt.getOtherClaims().put(BUSINESS_ID, user.homeBusinessId());

    // Sign and encode the JWT to a JSON string representation
    String encodedJWT = JWT.getEncoder().encode(jwt, signer);

    final JwtAuthenticationToken jwtAuthenticationToken =
        new JwtAuthenticationToken(jwtDecoder.decode(encodedJWT));
    jwtAuthenticationToken.setDetails(new UserRolesAndPermissionsCache());
    return jwtAuthenticationToken;
  }

  @RestrictedApi(
      explanation = "This is only for manipulating the CurrentUser during testing.",
      link =
          "https://tranwall.atlassian.net/wiki/spaces/CAP/pages/2088828965/Dev+notes+Service+method+security",
      allowedOnPath = "test/.*",
      allowlistAnnotations = {SwitchesCurrentUser.class})
  @SwitchesCurrentUser(reviewer = "jscarbor", explanation = "delegation")
  public static void setCurrentUser(User user, Set<String> globalRoles) {
    setCurrentUser(
        new CurrentUser(user.getType(), user.getId(), user.getBusinessId(), globalRoles));
  }

  @RestrictedApi(
      explanation = "This is only for manipulating the CurrentUser during testing.",
      link =
          "https://tranwall.atlassian.net/wiki/spaces/CAP/pages/2088828965/Dev+notes+Service+method+security",
      allowedOnPath = "/test/.*",
      allowlistAnnotations = {SwitchesCurrentUser.class})
  @SwitchesCurrentUser(reviewer = "jscarbor", explanation = "delegation")
  public static void setCurrentUser(User user) {
    setCurrentUser(user, Collections.emptySet());
  }

  @Override
  public Authentication authenticate(Authentication authentication) throws AuthenticationException {
    return mockAuthentication(CurrentUser.get());
  }

  @Override
  public boolean supports(Class<?> authentication) {
    return JwtAuthenticationToken.class.isAssignableFrom(authentication);
  }
}
