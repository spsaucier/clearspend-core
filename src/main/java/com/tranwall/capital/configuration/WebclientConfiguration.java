package com.tranwall.capital.configuration;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Configuration
public class WebclientConfiguration {

  @Bean
  HttpClient httpClient() {
    return HttpClient.create()
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 300)
        .responseTimeout(Duration.ofMillis(1000))
        .doOnConnected(
            conn ->
                conn.addHandlerLast(new ReadTimeoutHandler(1000, TimeUnit.MILLISECONDS))
                    .addHandlerLast(new WriteTimeoutHandler(1000, TimeUnit.MILLISECONDS)));
  }

  @Bean
  WebClient alloyIndividualWebClient(
      @Value("${client.alloy.url}") String url,
      @Value("${client.alloy.individual.token}") String token,
      @Value("${client.alloy.individual.secret}") String secret) {
    return WebClient.builder()
        .clientConnector(new ReactorClientHttpConnector(httpClient()))
        .baseUrl(url)
        .defaultHeaders(
            headers -> {
              headers.setBasicAuth(token, secret);
              headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
            })
        .build();
  }

  @Bean
  WebClient alloyBusinessWebClient(
      @Value("${client.alloy.url}") String url,
      @Value("${client.alloy.business.token}") String token,
      @Value("${client.alloy.business.secret}") String secret) {
    return WebClient.builder()
        .clientConnector(new ReactorClientHttpConnector(httpClient()))
        .baseUrl(url)
        .defaultHeaders(
            headers -> {
              headers.setBasicAuth(token, secret);
              headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
            })
        .build();
  }

  @Bean
  WebClient i2CWebClient(
      @Value("${client.i2c.url}") String url,
      @Value("${client.alloy.business.token}") String token,
      @Value("${client.alloy.business.secret}") String secret) {
    return WebClient.builder()
        .clientConnector(new ReactorClientHttpConnector(httpClient()))
        .baseUrl(url)
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .build();
  }
}
