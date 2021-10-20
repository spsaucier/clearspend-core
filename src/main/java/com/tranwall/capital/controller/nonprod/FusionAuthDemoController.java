package com.tranwall.capital.controller.nonprod;

import com.tranwall.capital.client.fusionauth.FusionAuthClient;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.controller.nonprod.fusionauth.CreateBusinessOwnerRequest;
import com.tranwall.capital.controller.nonprod.fusionauth.CreateUserRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/fusionauth-demo")
@RequiredArgsConstructor
public class FusionAuthDemoController {

  private final FusionAuthClient fusionAuthClient;

  @PostMapping("/create-business-owner")
  private String createBusinessOwner(@RequestBody CreateBusinessOwnerRequest request) {
    return fusionAuthClient.createBusinessOwner(
        new TypedId<>(request.getBusinessId()),
        new TypedId<>(request.getBusinessOwnerId()),
        request.getUsername(),
        request.getPassword());
  }

  @PostMapping("/create-user")
  private String createUser(@RequestBody CreateUserRequest request) {
    return fusionAuthClient.createBusinessOwner(
        new TypedId<>(request.getBusinessId()),
        new TypedId<>(request.getUserId()),
        request.getUsername(),
        request.getPassword());
  }
}
