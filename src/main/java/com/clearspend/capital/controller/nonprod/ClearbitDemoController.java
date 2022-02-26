package com.clearspend.capital.controller.nonprod;

import com.clearspend.capital.client.clearbit.ClearbitClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Profile("!prod")
@RestController
@RequestMapping("/non-production/clearbit")
@RequiredArgsConstructor
@Slf4j
public class ClearbitDemoController {

  private final ClearbitClient clearbitClient;

  @PostMapping("/search-merchant-logo")
  String createUser(@RequestBody String merchantName) {
    return clearbitClient.getLogo(merchantName);
  }
}
