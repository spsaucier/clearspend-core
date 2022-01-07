package com.clearspend.capital.common.advice;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@ControllerAdvice
@Slf4j
public class ResponseBodyAdviceLoggingAdapter implements ResponseBodyAdvice<Object> {

  @Autowired AdviceLoggingUtil adviceLoggingUtil;

  @Override
  public boolean supports(
      MethodParameter methodParameter, Class<? extends HttpMessageConverter<?>> aClass) {
    return true;
  }

  @Override
  public Object beforeBodyWrite(
      Object responseBody,
      MethodParameter methodParameter,
      MediaType mediaType,
      Class<? extends HttpMessageConverter<?>> aClass,
      ServerHttpRequest serverHttpRequest,
      ServerHttpResponse serverHttpResponse) {
    if (log.isInfoEnabled()
        && serverHttpRequest instanceof ServletServerHttpRequest
        && serverHttpResponse instanceof ServletServerHttpResponse) {

      HttpServletRequest httpServletRequest =
          ((ServletServerHttpRequest) serverHttpRequest).getServletRequest();
      HttpServletResponse httpServletResponse =
          ((ServletServerHttpResponse) serverHttpResponse).getServletResponse();

      adviceLoggingUtil.logRequestResponse(
          "Response",
          httpServletRequest,
          httpServletRequest.getAttribute("requestBody"),
          httpServletResponse,
          responseBody);
    }

    return responseBody;
  }
}
