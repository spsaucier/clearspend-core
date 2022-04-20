package com.clearspend.capital.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * In a Spring application, it is critical to have the same ObjectMapper reference shared across all
 * uses. This is for two main reasons. The first is that ObjectMapper is an expensive object to
 * create, so the Jackson documentation strongly recommends only ever creating and using a single
 * instance within an application. The second is that ObjectMapper is highly configurable, and using
 * different instances can lead to inconsistent behavior with serialization/deserialization
 * throughout the application.
 *
 * <p>The purpose of this class is to expose the shared Spring ObjectMapper in a global fashion.
 * This allows classes that are not being injected to by Spring to access it. The use case that led
 * to this class's creation was the need to use this ObjectMapper in the lifecycle hooks of a JPA
 * Entity for better JSONB support, but there will certainly be others.
 */
@Component
public class GlobalObjectMapper {
  private static ObjectMapper objectMapper;

  public GlobalObjectMapper(final ObjectMapper objectMapper) {
    GlobalObjectMapper.objectMapper = objectMapper;
  }

  public static ObjectMapper get() {
    return Optional.ofNullable(objectMapper)
        .orElseThrow(
            () ->
                new IllegalStateException(
                    "ObjectMapper is null. This usually means this method has been called prior to the full completion of the Spring container (@PostConstruct?), because the global ObjectMapper is only available at the end of that process."));
  }
}
