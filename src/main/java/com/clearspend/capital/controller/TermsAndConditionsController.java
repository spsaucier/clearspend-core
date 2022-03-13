package com.clearspend.capital.controller;

import com.clearspend.capital.controller.type.termsAndConditions.TermsAndConditionsResponse;
import com.clearspend.capital.service.TermsAndConditionsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/terms-and-conditions")
@RequiredArgsConstructor
public class TermsAndConditionsController {

  private final TermsAndConditionsService termsAndConditionsService;

  @PatchMapping("")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void acceptTermsAndConditionsTimestamp() {
    termsAndConditionsService.acceptTermsAndConditionsTimestamp();
  }

  @GetMapping("/timestamp-details")
  TermsAndConditionsResponse getTermsAndConditionsTimestampDetails() {
    return termsAndConditionsService.getTermsAndConditionsTimestampDetails();
  }
}
