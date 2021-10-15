package com.tranwall.capital.client.fusionauth;

import com.tranwall.capital.common.typedid.data.BusinessId;
import com.tranwall.capital.common.typedid.data.BusinessOwnerId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.common.typedid.data.UserId;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class FusionAuthClient {

  public String createBusinessOwner(
      TypedId<BusinessId> businessId,
      TypedId<BusinessOwnerId> businessOwnerId,
      String username,
      String password) {
    return UUID.randomUUID().toString();
  }

  public String createUser(
      TypedId<BusinessId> businessId, TypedId<UserId> userId, String username, String password) {
    return UUID.randomUUID().toString();
  }
}
