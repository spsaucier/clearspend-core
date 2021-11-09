package com.tranwall.capital.configuration.nonprod;

import com.inversoft.error.Errors;
import com.inversoft.rest.ClientResponse;
import com.tranwall.capital.client.fusionauth.FusionAuthProperties;
import io.fusionauth.client.FusionAuthClient;
import io.fusionauth.domain.User;
import io.fusionauth.domain.UserRegistration;
import io.fusionauth.domain.api.UserDeleteRequest;
import io.fusionauth.domain.api.UserDeleteResponse;
import io.fusionauth.domain.api.user.SearchRequest;
import io.fusionauth.domain.api.user.SearchResponse;
import io.fusionauth.domain.search.UserSearchCriteria;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.api.callback.Callback;
import org.flywaydb.core.api.callback.Context;
import org.flywaydb.core.api.callback.Event;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

// inspired by: https://www.baeldung.com/flyway-callbacks
// we need to drop all non admin users from FusionAuth when we change DB schema
// until we switch to incremental migration scripts
@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class ClearFusionAuthCallback implements Callback {

  private final FusionAuthProperties fusionAuthProperties;
  private final DataSourceProperties dataSourceProperties;
  private final FusionAuthClient fusionAuthClient;

  private final Set<Event> supportedEvents = Set.of(Event.BEFORE_VALIDATE, Event.BEFORE_CLEAN);

  // the variable is set to 'true' for dev environment to be sure
  // we don't accidentally drop all the users from dev env
  // while working at local environment during development
  private final String devEnvVariableName = "CAPITAL_CORE_DEV_ENV";

  @Override
  public boolean supports(Event event, Context context) {
    return supportedEvents.contains(event);
  }

  @Override
  public boolean canHandleInTransaction(Event event, Context context) {
    return true;
  }

  @Override
  public void handle(Event event, Context context) {
    switch (event) {
      case BEFORE_VALIDATE -> {
        log.info("FusionAuth base url:  {}", fusionAuthProperties.getBaseUrl());
        log.info("PostgreSQL url:       {}", dataSourceProperties.getUrl());
        log.info("{}: {}", devEnvVariableName, System.getenv(devEnvVariableName));
      }
      case BEFORE_CLEAN -> {
        if (!isDevEnv() && isDevEnvFusionAuth()) {
          log.info("not clearing FusionAuth: not dev env using dev env FusionAuth");
        } else if (!isDevEnv() && isLocalFusionAuth()) {
          log.info("clearing local FusionAuth instance...");
          clearFusionAuth();
        } else if (isDevEnv()) {
          log.info("clearing dev env FusionAuth instance...");
          clearFusionAuth();
        } else {
          log.info("not clearing FusionAuth");
        }
      }
    }
  }

  private boolean isDevEnv() {
    return Objects.equals(System.getenv(devEnvVariableName), "true");
  }

  @SneakyThrows
  private void clearFusionAuth() {
    SearchRequest searchRequest = new SearchRequest();
    UserSearchCriteria userSearchCriteria = new UserSearchCriteria();
    userSearchCriteria.queryString = "*";
    userSearchCriteria.accurateTotal = false;
    userSearchCriteria.numberOfResults = 100000;
    searchRequest.search = userSearchCriteria;
    List<User> users;
    ClientResponse<SearchResponse, Errors> response =
        fusionAuthClient.searchUsersByQuery(searchRequest);
    if (response.wasSuccessful()) {
      users = response.successResponse.users;
      log.debug("users found: {}", users.size());
    } else if (response.exception != null) {
      throw response.exception;
    } else if (response.errorResponse != null) {
      throw new RuntimeException("could not clear FusionAuth: " + response.errorResponse);
    } else {
      throw new RuntimeException("could not clear FusionAuth with unknown reason");
    }
    users.forEach(
        user -> {
          List<UserRegistration> registrations = user.getRegistrations();
          Set<String> roles =
              registrations.stream()
                  .map(userRegistration -> userRegistration.roles)
                  .flatMap(Collection::stream)
                  .collect(Collectors.toSet());
          log.debug("{} -> {}; isAdmin: {}", user.username, roles, roles.contains("admin"));
        });
    List<User> admins =
        users.stream()
            .filter(
                user ->
                    user.getRegistrations().stream()
                            .map(userRegistration -> userRegistration.roles)
                            .flatMap(Collection::stream)
                            .collect(Collectors.toSet())
                            .contains("admin")
                        || (user.username != null
                            && user.username.toLowerCase().contains("utkarsh"))
                        || Objects.equals(user.username, "admin@tranwall.com"))
            .collect(Collectors.toList());
    log.debug("admins[{}]:", admins.size());
    List<UUID> usersToDelete =
        users.stream().filter(u -> !admins.contains(u)).map(u -> u.id).collect(Collectors.toList());
    log.info(
        "users total: {}; admins: {}; to be deleted: {}",
        users.size(),
        admins.size(),
        usersToDelete.size());
    if (usersToDelete.isEmpty()) {
      log.info("no users to delete from FusionAuth");
    } else {
      ClientResponse<UserDeleteResponse, Errors> deleteResponse =
          fusionAuthClient.deleteUsersByQuery(new UserDeleteRequest(usersToDelete, true));
      if (deleteResponse.wasSuccessful()) {
        log.info("FusionAuth has been cleared");
      } else if (response.exception != null) {
        throw response.exception;
      } else if (response.errorResponse != null) {
        throw new RuntimeException("could not clear FusionAuth: " + response.errorResponse);
      } else {
        throw new RuntimeException("could not clear FusionAuth with unknown reason");
      }
    }
  }

  private boolean isLocalFusionAuth() {
    return fusionAuthProperties.getBaseUrl().contains("localhost");
  }

  private boolean isDevEnvFusionAuth() {
    return fusionAuthProperties.getBaseUrl().contains("https://fa.capital.dev.tranwall.net");
  }

  @Override
  public String getCallbackName() {
    return ClearFusionAuthCallback.class.getSimpleName();
  }
}
