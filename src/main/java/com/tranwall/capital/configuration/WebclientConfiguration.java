package com.tranwall.capital.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Configuration
@RequiredArgsConstructor
public class WebclientConfiguration {

  private final ObjectMapper mapper;
  private final WeblcientProperties properties;

  @Bean
  HttpClient httpClient() {
    return HttpClient.create()
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, properties.getConnectTimeout())
        .responseTimeout(Duration.ofMillis(properties.getResponseTimeout()))
        .doOnConnected(
            conn ->
                conn.addHandlerLast(
                        new ReadTimeoutHandler(properties.getReadTimeout(), TimeUnit.MILLISECONDS))
                    .addHandlerLast(
                        new WriteTimeoutHandler(
                            properties.getWriteTimeout(), TimeUnit.MILLISECONDS)));
  }

  @Bean
  WebClient alloyIndividualWebClient(
      @Value("${client.alloy.url}") String url,
      @Value("${client.alloy.individual.token}") String token,
      @Value("${client.alloy.individual.secret}") String secret) {
    return WebClient.builder()
        .exchangeStrategies(exchangeStrategies())
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
        .exchangeStrategies(exchangeStrategies())
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
  WebClient alloyDocumentWebClient(
      @Value("${client.alloy.url}") String url,
      @Value("${client.alloy.document.token}") String token,
      @Value("${client.alloy.document.secret}") String secret) {
    return WebClient.builder()
        .exchangeStrategies(exchangeStrategies())
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
  WebClient alloyGroupWebClient(
      @Value("${client.alloy.url}") String url,
      @Value("${client.alloy.group.token}") String token,
      @Value("${client.alloy.group.secret}") String secret) {
    return WebClient.builder()
        .exchangeStrategies(exchangeStrategies())
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
  WebClient i2CWebClient(@Value("${client.i2c.url}") String url) {
    return WebClient.builder()
        .exchangeStrategies(exchangeStrategies())
        .clientConnector(new ReactorClientHttpConnector(httpClient()))
        .baseUrl(url)
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .build();
  }

  @Bean
  WebClient fusionAuthWebClient(
      @Value("${client.fusionauth.base-url}") String url,
      @Value("${spring.security.oauth2.client.clientId}") String clientId,
      @Value("${spring.security.oauth2.client.secret}") String secret) {
    return WebClient.builder()
        .exchangeStrategies(exchangeStrategies())
        .clientConnector(new ReactorClientHttpConnector(httpClient()))
        .baseUrl(url)
        .defaultHeaders(
            headers -> {
              headers.setBasicAuth(clientId, secret);
              headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
            })
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .build();
  }

  // Somehow weblcient doesn't pick the correct mapper and uses the one that sends dates as
  // timestamps. Using this bean in order to make sure that correct mapper is used
  @Bean
  ExchangeStrategies exchangeStrategies() {
    return ExchangeStrategies.builder()
        .codecs(
            clientDefaultCodecsConfigurer -> {
              clientDefaultCodecsConfigurer
                  .defaultCodecs()
                  .jackson2JsonEncoder(new Jackson2JsonEncoder(mapper, MediaType.APPLICATION_JSON));
              clientDefaultCodecsConfigurer
                  .defaultCodecs()
                  .jackson2JsonDecoder(new Jackson2JsonDecoder(mapper, MediaType.APPLICATION_JSON));
            })
        .build();
  }
}
