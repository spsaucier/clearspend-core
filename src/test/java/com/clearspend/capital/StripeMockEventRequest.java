package com.clearspend.capital;

import java.nio.charset.StandardCharsets;
import org.springframework.mock.web.MockHttpServletRequest;

public class StripeMockEventRequest extends MockHttpServletRequest {

  public StripeMockEventRequest(String content) {
    addHeader("skip-stripe-header-verification", true);
    setContent(content.getBytes(StandardCharsets.UTF_8));
  }
}
