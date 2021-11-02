package com.tranwall.capital.controller.nonprod;

import com.tranwall.capital.controller.nonprod.type.fusionauth.CreateBusinessOwnerRequest;
import com.tranwall.capital.controller.nonprod.type.fusionauth.CreateUserRequest;
import com.tranwall.capital.service.FusionAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/non-production/fusionauth")
@RequiredArgsConstructor
public class FusionAuthDemoController {

  private final FusionAuthService fusionAuthService;

  @PostMapping("/create-business-owner")
  private String createBusinessOwner(@RequestBody CreateBusinessOwnerRequest request) {
    return fusionAuthService
        .createBusinessOwner(
            request.getBusinessId(),
            request.getBusinessOwnerId(),
            request.getUsername(),
            request.getPassword())
        .toString();
  }

  @PostMapping("/create-user")
  private String createUser(@RequestBody CreateUserRequest request) {
    return fusionAuthService
        .createUser(
            request.getBusinessId(),
            request.getUserId(),
            request.getUsername(),
            request.getPassword())
        .toString();
  }
}
