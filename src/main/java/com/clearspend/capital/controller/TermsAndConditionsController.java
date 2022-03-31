package com.clearspend.capital.controller;

import com.clearspend.capital.controller.type.termsAndConditions.TermsAndConditionsResponse;
import com.clearspend.capital.service.TermsAndConditionsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/terms-and-conditions")
@RequiredArgsConstructor
public class TermsAndConditionsController {

  private final TermsAndConditionsService termsAndConditionsService;

  @PatchMapping("")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void acceptTermsAndConditionsTimestamp() {
    termsAndConditionsService.acceptTermsAndConditions();
  }

  @GetMapping("/timestamp-details")
  TermsAndConditionsResponse getTermsAndConditionsTimestampDetails() {
    return TermsAndConditionsResponse.of(
        termsAndConditionsService.userAcceptedTermsAndConditions());
  }
}
