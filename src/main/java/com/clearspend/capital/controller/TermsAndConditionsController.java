package com.clearspend.capital.controller;

import com.clearspend.capital.common.data.util.HttpReqRespUtils;
import com.clearspend.capital.common.error.InvalidRequestException;
import com.clearspend.capital.controller.type.termsAndConditions.TermsAndConditionsResponse;
import com.clearspend.capital.service.TermsAndConditionsService;
import com.clearspend.capital.service.type.CurrentUser;
import javax.servlet.http.HttpServletRequest;
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
  @ResponseStatus(HttpStatus.ACCEPTED)
  TermsAndConditionsResponse acceptTermsAndConditionsTimestamp(
      HttpServletRequest httpServletRequest) {
    String clientIp = HttpReqRespUtils.getClientIpAddressIfServletRequestExist(httpServletRequest);
    String userAgent = HttpReqRespUtils.getUserAgent(httpServletRequest);
    if (userAgent == null) {
      throw new InvalidRequestException("userAgent should not be null");
    }
    log.info(
        "Accept terms and condition from ip of client: {} and userAgent: {}", clientIp, userAgent);
    termsAndConditionsService.acceptTermsAndConditions(
        CurrentUser.getUserId(), clientIp, userAgent);
    return getTermsAndConditionsTimestampDetails();
  }

  @GetMapping("/timestamp-details")
  TermsAndConditionsResponse getTermsAndConditionsTimestampDetails() {
    return TermsAndConditionsResponse.of(
        termsAndConditionsService.userAcceptedTermsAndConditions());
  }
}
