package com.clearspend.capital.common.data.util;

import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Template;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

public class MustacheResourceLoader {
  private static final ConcurrentMap<String, Template> cache = new ConcurrentHashMap<>();

  public static Template load(final Resource resource) {
    return cache.computeIfAbsent(
        resource.getDescription(),
        key -> Mustache.compiler().compile(SqlResourceLoader.load(resource)));
  }

  public static Template load(final String path) {
    return load(new ClassPathResource(path));
  }
}
