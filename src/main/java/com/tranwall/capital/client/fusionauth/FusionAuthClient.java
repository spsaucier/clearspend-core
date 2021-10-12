package com.tranwall.capital.client.fusionauth;

import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class FusionAuthClient {

  public String createBusinessOwner(
      UUID businessId, UUID businessOwnerId, String username, String password) {
    return UUID.randomUUID().toString();
  }

  public String createUser(UUID businessId, UUID userId, String username, String password) {
    return UUID.randomUUID().toString();
  }
}
