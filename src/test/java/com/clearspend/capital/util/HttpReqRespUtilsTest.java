package com.clearspend.capital.util;

import com.clearspend.capital.common.data.util.HttpReqRespUtils;
import java.util.Collections;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class HttpReqRespUtilsTest {

  @Test
  void testGetClientIpFromXForwardedFor() {
    HttpServletRequest httpRequest = Mockito.mock(HttpServletRequest.class);
    Mockito.when(httpRequest.getHeaders(Mockito.anyString()))
        .thenReturn(Collections.enumeration(List.of()));
    Mockito.when(httpRequest.getHeaders("X-Forwarded-For"))
        .thenReturn(Collections.enumeration(List.of("75.62.70.172", "34.120.103.5", "")));
    String clientIp = HttpReqRespUtils.getClientIpAddressIfServletRequestExist(httpRequest);
    Assertions.assertEquals("75.62.70.172", clientIp);
  }

  @Test
  void testGetClientIpFrom_x_forwarded_for() {
    HttpServletRequest httpRequest = Mockito.mock(HttpServletRequest.class);
    Mockito.when(httpRequest.getHeaders("x-forwarded-for"))
        .thenReturn(Collections.enumeration(List.of("75.62.70.172", "34.120.103.5", "")));
    String clientIp = HttpReqRespUtils.getClientIpAddressIfServletRequestExist(httpRequest);
    Assertions.assertEquals("75.62.70.172", clientIp);
  }

  @Test
  void testGetClientIpForMissingHears() {
    HttpServletRequest httpRequest = Mockito.mock(HttpServletRequest.class);
    Mockito.when(httpRequest.getHeaders(Mockito.anyString()))
        .thenReturn(Collections.enumeration(List.of()));
    Mockito.when(httpRequest.getRemoteAddr()).thenReturn("75.62.70.172,34.120.103.5,");
    String clientIp = HttpReqRespUtils.getClientIpAddressIfServletRequestExist(httpRequest);
    Assertions.assertEquals("75.62.70.172", clientIp);
  }
}
