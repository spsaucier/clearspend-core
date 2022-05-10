package com.clearspend.capital;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import javax.servlet.http.Cookie;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@Component
@RequiredArgsConstructor
public class MockMvcHelper {

  private final MockMvc mvc;
  private final ObjectMapper objectMapper;

  @SneakyThrows
  public <T> T queryObject(
      String uri, HttpMethod httpMethod, Cookie userCookie, Object body, Class<T> responseClass) {
    MockHttpServletResponse response = queryBackendFor200(uri, httpMethod, userCookie, body);

    return objectMapper.readValue(response.getContentAsString(), responseClass);
  }

  @SneakyThrows
  public <T> T queryObject(
      final String uri,
      final HttpMethod httpMethod,
      final Cookie userCookie,
      final Object body,
      final JavaType responseType) {
    MockHttpServletResponse response = queryBackendFor200(uri, httpMethod, userCookie, body);

    return objectMapper.readValue(response.getContentAsString(), responseType);
  }

  @SneakyThrows
  public <T> T queryObject(
      String uri, HttpMethod httpMethod, Cookie userCookie, Class<T> responseClass) {
    return queryObject(uri, httpMethod, userCookie, null, responseClass);
  }

  @SneakyThrows
  public <T> T queryObject(
      final String uri,
      final HttpMethod httpMethod,
      final Cookie userCookie,
      final JavaType responseType) {
    return queryObject(uri, httpMethod, userCookie, null, responseType);
  }

  @SneakyThrows
  public ResultActions query(
      final String uri, final HttpMethod httpMethod, final Cookie userCookie) {
    return queryBackend(uri, httpMethod, userCookie, null);
  }

  @SneakyThrows
  public ResultActions query(
      final String uri, final HttpMethod httpMethod, final Cookie userCookie, final Object body) {
    return queryBackend(uri, httpMethod, userCookie, body);
  }

  @SneakyThrows
  public <T> List<T> queryList(
      String uri,
      HttpMethod httpMethod,
      Cookie userCookie,
      Object body,
      TypeReference<List<T>> typeReference) {
    MockHttpServletResponse response = queryBackendFor200(uri, httpMethod, userCookie, body);

    return objectMapper.readValue(response.getContentAsString(), typeReference);
  }

  private MockHttpServletResponse queryBackendFor200(
      final String uri, final HttpMethod httpMethod, final Cookie userCookie, final Object body)
      throws Exception {
    return queryBackend(uri, httpMethod, userCookie, body)
        .andExpect(status().isOk())
        .andReturn()
        .getResponse();
  }

  private ResultActions queryBackend(
      String uri, HttpMethod httpMethod, Cookie userCookie, Object body) throws Exception {
    final MockHttpServletRequestBuilder builder =
        switch (httpMethod) {
          case GET -> MockMvcRequestBuilders.get(uri);
          case PUT -> MockMvcRequestBuilders.put(uri);
          case HEAD -> MockMvcRequestBuilders.head(uri);
          case POST -> MockMvcRequestBuilders.post(uri);
          case PATCH -> MockMvcRequestBuilders.patch(uri);
          case DELETE -> MockMvcRequestBuilders.delete(uri);
          case OPTIONS -> MockMvcRequestBuilders.options(uri);
          case TRACE -> throw new RuntimeException("Trace http method is not supported");
        };
    builder.contentType("application/json").cookie(userCookie);

    if (body != null) {
      builder.content(objectMapper.writeValueAsString(body));
    }

    return mvc.perform(builder);
  }
}
