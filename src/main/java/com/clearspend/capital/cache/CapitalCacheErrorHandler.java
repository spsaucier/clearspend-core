package com.clearspend.capital.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;

/**
 * This will log and prevent the propagation of exceptions from Spring caching operations. This
 * prevents issues with the Redis cache from crashing an operation.
 */
@Slf4j
public class CapitalCacheErrorHandler implements CacheErrorHandler {
  @Override
  public void handleCacheGetError(
      final RuntimeException exception, final Cache cache, final Object key) {
    log.error("Cache Get Error", exception);
  }

  @Override
  public void handleCachePutError(
      final RuntimeException exception, final Cache cache, final Object key, final Object value) {
    log.error("Cache Put Error", exception);
  }

  @Override
  public void handleCacheEvictError(
      final RuntimeException exception, final Cache cache, final Object key) {
    log.error("Cache Evict Error", exception);
  }

  @Override
  public void handleCacheClearError(final RuntimeException exception, final Cache cache) {
    log.error("Cache Clear Error", exception);
  }
}
