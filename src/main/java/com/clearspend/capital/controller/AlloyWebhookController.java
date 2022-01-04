package com.clearspend.capital.controller;

import com.clearspend.capital.controller.type.review.AlloyWebHookResponse;
import com.clearspend.capital.controller.type.review.GroupManualReviewOutcome;
import com.clearspend.capital.service.AlloyWebHookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/alloy/webhook")
public class AlloyWebhookController {

  private final AlloyWebHookService alloyWebHookService;

  @PostMapping
  public String helloWebhook(@RequestBody String body) {
    log.info("---> {}", body);
    return "Hello world";
  }

  @PatchMapping("")
  public void updateHookFromAlloy(@RequestBody AlloyWebHookResponse webHookResponse) {
    alloyWebHookService.processWebHookFromAlloy(
        GroupManualReviewOutcome.getEnumByValue(webHookResponse.getData().getOutcome()),
        webHookResponse.getData());
  }
}
