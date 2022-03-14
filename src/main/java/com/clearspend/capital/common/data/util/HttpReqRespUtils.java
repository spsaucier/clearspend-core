package com.clearspend.capital.common.data.util;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.util.Enumeration;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HttpReqRespUtils {

  private static final String[] IP_HEADER_CANDIDATES = {
    "x-forwarded-for",
    "X-FORWARDED-FOR",
    "X-Forwarded-For",
    "Proxy-Client-IP",
    "WL-Proxy-Client-IP",
    "HTTP_X_FORWARDED_FOR",
    "HTTP_X_FORWARDED",
    "HTTP_X_CLUSTER_CLIENT_IP",
    "HTTP_CLIENT_IP",
    "HTTP_FORWARDED_FOR",
    "HTTP_FORWARDED",
    "HTTP_VIA",
    "REMOTE_ADDR"
  };

  private HttpReqRespUtils() {}

  public static String getClientIpAddressIfServletRequestExist(HttpServletRequest request) {

    for (String header : IP_HEADER_CANDIDATES) {
      Enumeration<String> headers = request.getHeaders(header);
      log.info("headers: {}", headers);
      if (!headers.hasMoreElements()) {
        continue;
      }
      String ip = headers.nextElement();
      if (isNotEmpty(ip) && !"unknown".equalsIgnoreCase(ip)) {
        log.info("header {} and returned ip {}", header, ip);
        return safeCheckIp(ip);
      }
    }

    return safeCheckIp(request.getRemoteAddr());
  }

  private static String safeCheckIp(String ip) {
    if (ip != null && ip.contains(",")) {
      return ip.split(",")[0];
    }
    return ip;
  }
}
