package com.clearspend.capital.common.data.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

@Slf4j
public class SqlResourceLoader {

  /**
   * A cache that doesn't expire. It doesn't expire because these resources come from the classpath
   * anyway, so a new deployment is the only reason they are expected to change.
   */
  private static final ConcurrentMap<String, String> cache = new ConcurrentHashMap<>();

  public static String load(Resource resource) {
    return cache.computeIfAbsent(resource.getDescription(), key -> getContent(resource));
  }

  public static String load(String path) {
    return load(new ClassPathResource(path));
  }

  private static String getContent(Resource resource) {
    try (InputStream inputStream = resource.getInputStream()) {
      return new String(inputStream.readAllBytes(), Charset.defaultCharset())
          .replaceAll("::", "\\:\\:");
    } catch (IOException e) {
      throw new RuntimeException("Failed to read resource content", e);
    }
  }
}
