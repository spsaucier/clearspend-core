package com.clearspend.capital.common.data.util;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;

@Slf4j
public class SqlResourceLoader {

  /**
   * A cache that doesn't expire. It doesn't expire because these resources come from the classpath
   * anyway, so a new deployment is the only reason they are expected to change.
   */
  private static final Map<String, String> cache = new HashMap<>();

  public static String load(final Resource resource) throws IOException {
    if (!cache.containsKey(resource.getDescription())) {
      cache.put(
          resource.getDescription(),
          new String(resource.getInputStream().readAllBytes(), Charset.defaultCharset())
              .replaceAll("::", "\\:\\:"));
    }
    return cache.get(resource.getDescription());
  }
}
