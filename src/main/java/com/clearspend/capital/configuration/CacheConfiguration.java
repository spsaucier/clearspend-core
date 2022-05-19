package com.clearspend.capital.configuration;

import com.clearspend.capital.cache.CapitalCacheErrorHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;

@Configuration
@Profile("!test")
public class CacheConfiguration {

  @Bean
  public RedisCacheConfiguration redisCacheConfiguration(ObjectMapper objectMapper) {
    return RedisCacheConfiguration.defaultCacheConfig()
        .entryTtl(Duration.ofHours(1))
        .disableCachingNullValues()
        .serializeValuesWith(
            SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer(objectMapper)));
  }

  @Bean
  public CachingConfigurerSupport cachingConfigurerSupport() {
    return new CachingConfigurerSupport() {
      @Override
      public CacheErrorHandler errorHandler() {
        return new CapitalCacheErrorHandler();
      }
    };
  }

  @Bean
  public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer() {
    return (builder) ->
        builder
            .withCacheConfiguration(
                "globalRole",
                RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofMinutes(10)))
            .withCacheConfiguration(
                "mx-merchant-name",
                RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofMinutes(60)))
            .withCacheConfiguration(
                "mx-merchant-logo",
                RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofMinutes(60)))
            .withCacheConfiguration(
                "codat-suppliers",
                RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofMinutes(60)));
  }
}
