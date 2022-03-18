package com.clearspend.capital.common.advice;

import com.clearspend.capital.common.masking.MaskAnnotationIntrospector;
import com.clearspend.capital.common.typedid.codec.TypedIdModule;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdviceLoggingUtil {

  private final ObjectMapper objectMapper =
      new ObjectMapper()
          .registerModule(new JavaTimeModule())
          .registerModule(new TypedIdModule())
          .registerModule(new Jdk8Module())
          .registerModule(new ParameterNamesModule())
          .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
          .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
          .configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false)
          .configure(JsonGenerator.Feature.IGNORE_UNKNOWN, true)
          .configure(JsonParser.Feature.IGNORE_UNDEFINED, true)
          .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
          .setAnnotationIntrospector(new MaskAnnotationIntrospector());

  void logRequestResponse(
      String prefix,
      HttpServletRequest httpServletRequest,
      Object requestBody,
      HttpServletResponse httpServletResponse,
      Object responseBody) {

    StringBuilder stringBuilder =
        new StringBuilder(prefix)
            .append(" >>>>>>>>>>>>>>>>>>>> [Operation: ")
            .append(httpServletRequest.getMethod())
            .append(" ")
            .append(httpServletRequest.getRequestURI())
            .append("\n");

    if (httpServletResponse != null) {
      stringBuilder.append("Status: [").append(httpServletResponse.getStatus()).append("]\n");
    }

    Map<String, String> headersMap =
        httpServletResponse != null
            ? buildHeadersMap(httpServletResponse)
            : buildHeadersMap(httpServletRequest);
    if (!headersMap.isEmpty()) {
      stringBuilder.append("Headers: [").append(headersMap).append("]\n");
    }

    Map<String, String> claims = buildClaimsMap(httpServletRequest);
    if (!claims.isEmpty()) {
      stringBuilder.append("JWT Claims: [").append(claims).append("]\n");
    }

    Map<String, String> parameters = buildParametersMap(httpServletRequest);
    if (!parameters.isEmpty()) {
      stringBuilder.append("Request Parameters: [").append(parameters).append("]\n");
    }

    appendBody("Request", httpServletRequest, requestBody, stringBuilder);
    appendBody("Response", httpServletRequest, responseBody, stringBuilder);

    stringBuilder.append(prefix).append(" <<<<<<<<<<<<<<<<<<<< ]");

    log.info(stringBuilder.toString());
  }

  private void appendBody(
      String type,
      HttpServletRequest httpServletRequest,
      Object body,
      StringBuilder stringBuilder) {
    if (body == null) {
      return;
    }

    stringBuilder.append(type);
    stringBuilder.append(" Body: [");
    if (httpServletRequest.getRequestURI().startsWith("/images/receipts")) {
      stringBuilder.append("<binary>");
    } else {
      try {
        stringBuilder.append(objectMapper.writeValueAsString(body));
      } catch (Exception e) {
        stringBuilder.append("<error: ");
        stringBuilder.append(e.getMessage());
        stringBuilder.append(">");
      }
    }
    stringBuilder.append("]\n");
  }

  Map<String, String> buildHeadersMap(HttpServletRequest request) {
    Map<String, String> map = new HashMap<>();

    request
        .getHeaderNames()
        .asIterator()
        .forEachRemaining(
            key -> {
              if (!"authorization".equalsIgnoreCase(key)) {
                map.put(key, request.getHeader(key));
              }
            });

    return map;
  }

  Map<String, String> buildHeadersMap(HttpServletResponse response) {
    return response.getHeaderNames().stream()
        .collect(Collectors.toMap(key -> key, response::getHeader));
  }

  Map<String, String> buildClaimsMap(HttpServletRequest request) {
    Map<String, String> claims = new HashMap<>();

    request
        .getHeaderNames()
        .asIterator()
        .forEachRemaining(
            key -> {
              String value = request.getHeader(key);
              try {
                if (StringUtils.isNotEmpty(key)
                    && "authorization".equalsIgnoreCase(key)
                    && StringUtils.isNotEmpty(value)
                    && value.toLowerCase().contains("bearer")) {
                  String token = value.substring("bearer".length() + 1);
                  token = new String(Base64.decodeBase64(token.split("\\.")[1]));
                  Map<String, Object> stringObjectMap =
                      objectMapper.readValue(token, new TypeReference<>() {});
                  for (Entry<String, Object> entry : stringObjectMap.entrySet()) {
                    claims.put(entry.getKey(), entry.getValue().toString());
                  }
                }
              } catch (Exception e) {
                log.warn("Could not parse claims from bearer token: {}", value);
              }
            });

    return claims;
  }

  Map<String, String> buildParametersMap(HttpServletRequest httpServletRequest) {
    Map<String, String> resultMap = new HashMap<>();

    httpServletRequest
        .getParameterNames()
        .asIterator()
        .forEachRemaining(key -> resultMap.put(key, httpServletRequest.getParameter(key)));

    return resultMap;
  }
}
