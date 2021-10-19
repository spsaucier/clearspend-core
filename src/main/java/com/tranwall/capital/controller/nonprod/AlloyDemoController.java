package com.tranwall.capital.controller.nonprod;

import com.tranwall.capital.client.alloy.AlloyClient;
import com.tranwall.capital.client.alloy.request.OnboardBusinessRequest;
import com.tranwall.capital.client.alloy.request.OnboardIndividualRequest;
import com.tranwall.capital.client.alloy.response.OnboardResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Profile("!prod")
@RestController
@RequestMapping("/non-production")
@RequiredArgsConstructor
public class AlloyDemoController {

  public final AlloyClient alloyClient;

  @PostMapping("/alloy/onboard/individual")
  public OnboardResponse onboardIndividual(@RequestBody OnboardIndividualRequest request) {
    return alloyClient.onboardIndividual(request);
  }

  @PostMapping("/alloy/onboard/business")
  public OnboardResponse onboardBusiness(@RequestBody OnboardBusinessRequest request) {
    return alloyClient.onboardBusiness(request);
  }
}
