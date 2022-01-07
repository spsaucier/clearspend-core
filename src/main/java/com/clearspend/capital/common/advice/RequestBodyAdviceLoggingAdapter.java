package com.clearspend.capital.common.advice;

import java.lang.reflect.Type;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;

@ControllerAdvice
@Slf4j
public class RequestBodyAdviceLoggingAdapter extends RequestBodyAdviceAdapter {

  @Autowired HttpServletRequest httpServletRequest;
  @Autowired AdviceLoggingUtil adviceLoggingUtil;

  @Override
  public boolean supports(
      MethodParameter methodParameter, Type type, Class<? extends HttpMessageConverter<?>> aClass) {
    return true;
  }

  @Override
  public Object afterBodyRead(
      Object requestBody,
      HttpInputMessage inputMessage,
      MethodParameter parameter,
      Type targetType,
      Class<? extends HttpMessageConverter<?>> converterType) {

    if (log.isInfoEnabled()) {
      adviceLoggingUtil.logRequestResponse("Request", httpServletRequest, requestBody, null, null);

      // add requestBody as request attribute, so we can also log it as part of the response
      httpServletRequest.setAttribute("requestBody", requestBody);
    }

    return super.afterBodyRead(requestBody, inputMessage, parameter, targetType, converterType);
  }
}
