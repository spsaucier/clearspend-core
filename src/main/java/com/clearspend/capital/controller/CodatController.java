package com.clearspend.capital.controller;

import com.clearspend.capital.service.CodatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/codat")
@RequiredArgsConstructor
@Slf4j
public class CodatController {
  private final CodatService codatService;

  @GetMapping("/qbo")
  private String getQboConnectionLink() {
    return codatService.createQboConnectionForBusiness();
  }

  @GetMapping("/connection-status")
  private Boolean getIntegrationConnectionStatus() {
    return codatService.getIntegrationConnectionStatus();
  }
}
