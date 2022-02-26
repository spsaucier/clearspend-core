package com.clearspend.capital.controller.nonprod;

import com.clearspend.capital.controller.nonprod.type.fusionauth.CreateBusinessOwnerRequest;
import com.clearspend.capital.controller.nonprod.type.fusionauth.CreateUserRequest;
import com.clearspend.capital.service.FusionAuthService;
import com.clearspend.capital.service.FusionAuthService.FusionAuthUserCreator;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@FusionAuthUserCreator(reviewer = "jscarbor", explanation = "non-production scope")
@RestController
@RequestMapping("/non-production/fusionauth")
@RequiredArgsConstructor
public class FusionAuthDemoController {

  private final FusionAuthService fusionAuthService;

  @PostMapping("/create-business-owner")
  String createBusinessOwner(@RequestBody CreateBusinessOwnerRequest request) {
    return fusionAuthService
        .createBusinessOwner(
            request.getBusinessId(),
            request.getBusinessOwnerId(),
            request.getUsername(),
            request.getPassword())
        .toString();
  }

  @PostMapping("/create-user")
  String createUser(@RequestBody CreateUserRequest request) {
    return fusionAuthService
        .createUser(
            request.getBusinessId(),
            request.getUserId(),
            request.getUsername(),
            request.getPassword())
        .toString();
  }
}
