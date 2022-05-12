package com.clearspend.capital;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Duration;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;

@Configuration
@Profile("test")
public class TestCacheConfiguration {

  @Bean
  public RedisCacheConfiguration redisCacheConfiguration() {
    return RedisCacheConfiguration.defaultCacheConfig()
        .entryTtl(Duration.ZERO)
        .disableCachingNullValues()
        .serializeValuesWith(
            SerializationPair.fromSerializer(
                new GenericJackson2JsonRedisSerializer(
                    new ObjectMapper().registerModule(new JavaTimeModule()))));
  }

  @Bean
  public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer() {
    return (builder) ->
        builder
            .withCacheConfiguration(
                "globalRole",
                RedisCacheConfiguration.defaultCacheConfig()
                    .entryTtl(Duration.ofMinutes(10))
                    .serializeValuesWith(
                        SerializationPair.fromSerializer(new JdkSerializationRedisSerializer())))
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
