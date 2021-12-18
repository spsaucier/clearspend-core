package com.clearspend.capital.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/alloy/wh")
public class AlloyWebhookController {

  @PostMapping
  public String helloWebhook(@RequestBody String body) {
    log.info("---> {}", body);
    return "Hello world";
  }

  @GetMapping("/hello")
  public String helloWorld() {
    return "qwerty";
  }
}
