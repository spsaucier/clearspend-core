package com.clearspend.capital.controller;

import com.clearspend.capital.common.advice.AssignWebhookSecurityContextAdvice.SecureWebhook;
import com.clearspend.capital.service.PlaidService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.Charset;
import java.security.Principal;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/plaid")
@RequiredArgsConstructor
@SecureWebhook
@Slf4j
public class PlaidController {
  private final PlaidService plaidService;
  private final ObjectMapper objectMapper;

  @SneakyThrows
  @RequestMapping(value = "/webhook", method = RequestMethod.POST)
  void webhook(HttpServletRequest request) throws RuntimeException {
    Principal principal = request.getUserPrincipal();
    // This request json looks like a good candidate for a custom deserializer
    // https://www.baeldung.com/jackson-deserialization
    Map<String, Object> json =
        objectMapper.readValue(
            StreamUtils.copyToString(request.getInputStream(), Charset.defaultCharset()),
            new TypeReference<>() {});
    plaidService.webhook(json);
  }
}
