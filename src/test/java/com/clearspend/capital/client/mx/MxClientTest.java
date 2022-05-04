package com.clearspend.capital.client.mx;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.clearspend.capital.client.mx.types.EnhanceTransactionResponse;
import com.clearspend.capital.client.mx.types.GetMerchantDetailsResponse;
import com.clearspend.capital.client.mx.types.MxMerchantDetails;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.function.Function;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.RequestBodySpec;
import org.springframework.web.reactive.function.client.WebClient.RequestBodyUriSpec;
import org.springframework.web.reactive.function.client.WebClient.RequestHeadersSpec;
import org.springframework.web.reactive.function.client.WebClient.RequestHeadersUriSpec;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
public class MxClientTest {

  private WebClient mockWebClient;
  private ObjectMapper mapper;

  private MxClient underTest;

  @Captor private ArgumentCaptor<Function<ClientResponse, Mono<?>>> functionCaptor;

  @BeforeEach
  public void setup() {
    mockWebClient = Mockito.mock(WebClient.class);
    mapper = new ObjectMapper();

    underTest = new MxClient(mockWebClient, mapper);
  }

  @Test
  @SneakyThrows
  public void getCleansedMerchantName_whenOk_validResultIsReturned() {
    RequestBodyUriSpec mockUriSpec = mock(RequestBodyUriSpec.class);
    RequestBodySpec mockBodySpec = mock(RequestBodySpec.class);
    RequestHeadersSpec mockHeaderSpec = mock(RequestHeadersSpec.class);
    ClientResponse mockResponse = mock(ClientResponse.class);
    when(mockWebClient.post()).thenReturn(mockUriSpec);
    when(mockUriSpec.uri(anyString())).thenReturn(mockBodySpec);
    when(mockBodySpec.body(any())).thenReturn(mockHeaderSpec);
    when(mockHeaderSpec.exchangeToMono(any(Function.class)))
        .thenReturn(Mono.just(new EnhanceTransactionResponse(Collections.emptyList())));
    when(mockResponse.statusCode()).thenReturn(HttpStatus.OK);
    when(mockResponse.bodyToMono(eq(EnhanceTransactionResponse.class)))
        .thenReturn(Mono.just(new EnhanceTransactionResponse(Collections.emptyList())));

    underTest.getCleansedMerchantName("test", 123);

    verify(mockHeaderSpec, times(1)).exchangeToMono(functionCaptor.capture());

    assertThat(functionCaptor.getValue().apply(mockResponse).block())
        .isNotNull()
        .isInstanceOf(EnhanceTransactionResponse.class);
  }

  @Test
  @SneakyThrows
  public void getCleansedMerchantName_whenError_ExceptionIsThrown() {
    RequestBodyUriSpec mockUriSpec = mock(RequestBodyUriSpec.class);
    RequestBodySpec mockBodySpec = mock(RequestBodySpec.class);
    RequestHeadersSpec mockHeaderSpec = mock(RequestHeadersSpec.class);
    ClientResponse mockResponse = mock(ClientResponse.class);
    when(mockWebClient.post()).thenReturn(mockUriSpec);
    when(mockUriSpec.uri(anyString())).thenReturn(mockBodySpec);
    when(mockBodySpec.body(any())).thenReturn(mockHeaderSpec);
    when(mockHeaderSpec.exchangeToMono(any(Function.class)))
        .thenReturn(Mono.just(new EnhanceTransactionResponse(Collections.emptyList())));
    when(mockResponse.statusCode()).thenReturn(HttpStatus.NOT_FOUND);
    when(mockResponse.createException())
        .thenReturn(Mono.just(new WebClientResponseException(404, "Not Found", null, null, null)));

    underTest.getCleansedMerchantName("test", 123);

    verify(mockHeaderSpec, times(1)).exchangeToMono(functionCaptor.capture());

    assertThrows(
        WebClientResponseException.class,
        () -> functionCaptor.getValue().apply(mockResponse).block());
  }

  @Test
  @SneakyThrows
  public void getMerchantLogo_whenOk_validResultReturned() {
    RequestHeadersUriSpec mockUriSpec = mock(RequestHeadersUriSpec.class);
    RequestHeadersSpec mockHeaderSpec = mock(RequestHeadersUriSpec.class);
    ClientResponse mockResponse = mock(ClientResponse.class);

    when(mockWebClient.get()).thenReturn(mockUriSpec);
    when(mockUriSpec.uri(anyString())).thenReturn(mockHeaderSpec);
    GetMerchantDetailsResponse parsedResponse = new GetMerchantDetailsResponse();
    MxMerchantDetails details = new MxMerchantDetails();
    details.setLogoUrl("logo-url");
    parsedResponse.setDetails(details);
    when(mockHeaderSpec.exchangeToMono(any(Function.class))).thenReturn(Mono.just(parsedResponse));
    when(mockResponse.statusCode()).thenReturn(HttpStatus.OK);
    when(mockResponse.bodyToMono(eq(GetMerchantDetailsResponse.class)))
        .thenReturn(Mono.just(parsedResponse));

    assertThat(underTest.getMerchantLogo("test")).isEqualTo("logo-url");

    verify(mockHeaderSpec, times(1)).exchangeToMono(functionCaptor.capture());

    assertThat(functionCaptor.getValue().apply(mockResponse).block())
        .isNotNull()
        .isInstanceOf(GetMerchantDetailsResponse.class);
  }
}
