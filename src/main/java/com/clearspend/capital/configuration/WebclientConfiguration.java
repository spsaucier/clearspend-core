package com.clearspend.capital.configuration;

import com.clearspend.capital.client.codat.types.CodatProperties;
import com.clearspend.capital.client.mx.types.MxProperties;
import com.clearspend.capital.client.stripe.StripeProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
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
@Slf4j
public class WebclientConfiguration {

  private final ObjectMapper mapper;
  private final WeblcientProperties properties;

  @Bean
  HttpClient httpClient() {
    return createNewHttpClient(
        properties.getConnectTimeout(),
        properties.getResponseTimeout(),
        properties.getReadTimeout(),
        properties.getWriteTimeout());
  }

  private HttpClient createNewHttpClient(
      int connectTimeout, int responseTimeout, int readTimeout, int writeTimeout) {
    return HttpClient.create()
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeout)
        .responseTimeout(Duration.ofMillis(responseTimeout))
        .doOnConnected(
            conn ->
                conn.addHandlerLast(new ReadTimeoutHandler(readTimeout, TimeUnit.MILLISECONDS))
                    .addHandlerLast(new WriteTimeoutHandler(writeTimeout, TimeUnit.MILLISECONDS)));
  }

  @Bean
  WebClient clearbitWebClient(@Value("${client.clearbit.api-key}") String apiKey) {
    return WebClient.builder()
        .exchangeStrategies(exchangeStrategies())
        .clientConnector(new ReactorClientHttpConnector(httpClient()))
        .baseUrl("https://company.clearbit.com/v1")
        .defaultHeaders(
            headers -> {
              headers.setBasicAuth(apiKey, StringUtils.EMPTY);
              headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
            })
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .build();
  }

  @Bean
  WebClient stripeTreasuryWebClient(StripeProperties stripeProperties) {
    return WebClient.builder()
        .exchangeStrategies(exchangeStrategies())
        .clientConnector(
            new ReactorClientHttpConnector(
                createNewHttpClient(
                    stripeProperties.getConnectTimeout(),
                    stripeProperties.getReadTimeout(),
                    stripeProperties.getReadTimeout(),
                    properties.getWriteTimeout())))
        .baseUrl("https://api.stripe.com/v1")
        .defaultHeaders(
            headers -> {
              headers.setBasicAuth(stripeProperties.getApiKey(), StringUtils.EMPTY);
              headers.add("Stripe-Version", "2020-08-27;financial_accounts_beta=v3");
            })
        .build();
  }

  @Bean
  WebClient codatWebClient(CodatProperties codatProperties) {
    return WebClient.builder()
        .exchangeStrategies(exchangeStrategies())
        .clientConnector(
            new ReactorClientHttpConnector(
                createNewHttpClient(
                    codatProperties.getConnectTimeout(),
                    codatProperties.getResponseTimeout(),
                    codatProperties.getReadTimeout(),
                    codatProperties.getWriteTimeout())))
        .baseUrl(codatProperties.getBaseUrl())
        .defaultHeaders(
            headers -> {
              headers.setBasicAuth(codatProperties.getAuthToken());
              headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
            })
        .build();
  }

  @Bean
  @Qualifier("mxWebClient")
  WebClient mxWebClient(MxProperties mxProperties) {
    return WebClient.builder()
        .exchangeStrategies(exchangeStrategies())
        .clientConnector(
            new ReactorClientHttpConnector(
                createNewHttpClient(
                    mxProperties.getConnectTimeout(),
                    mxProperties.getResponseTimeout(),
                    mxProperties.getReadTimeout(),
                    mxProperties.getWriteTimeout())))
        .baseUrl(mxProperties.getBaseUrl())
        .defaultHeaders(
            headers -> {
              headers.setBasicAuth(mxProperties.getAuthSecret());
              headers.setAccept(List.of(new MediaType("application", "vnd.mx.api.v1+json")));
            })
        .build();
  }

  @Bean
  @ConditionalOnExpression(
      "T(org.apache.commons.lang3.StringUtils).isNotEmpty('${client.stripe.auth-fallback-url}')")
  WebClient authFallbackClient(StripeProperties stripeProperties) {
    return WebClient.builder()
        .exchangeStrategies(exchangeStrategies())
        .clientConnector(new ReactorClientHttpConnector(httpClient()))
        .baseUrl(stripeProperties.getAuthFallbackUrl())
        .defaultHeaders(headers -> headers.add("skip-stripe-header-verification", "true"))
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
