package com.clearspend.capital.configuration.filter;

import com.clearspend.capital.common.data.util.HttpReqRespUtils;
import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class RequestFilter implements Filter {

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    HttpServletRequest req = (HttpServletRequest) request;
    String ip = HttpReqRespUtils.getClientIpAddressIfServletRequestExist(req);
    log.info(
        "Request ip: {} and userAgent: {} / {}",
        ip,
        req.getHeader(HttpHeaders.USER_AGENT),
        req.getHeader(HttpHeaders.USER_AGENT.toLowerCase()));
    chain.doFilter(request, response);
  }
}
