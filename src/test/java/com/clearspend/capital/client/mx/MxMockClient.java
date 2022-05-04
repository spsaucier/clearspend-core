package com.clearspend.capital.client.mx;

import com.clearspend.capital.client.mx.types.EnhanceTransactionResponse;
import com.clearspend.capital.data.model.AccountActivity;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Profile("test")
@Component
public class MxMockClient extends MxClient {

  @Setter private String mockMerchantLogo;
  @Setter private EnhanceTransactionResponse mockTransactionResponse;

  public MxMockClient(@Qualifier("mxWebClient") WebClient webClient, ObjectMapper objectMapper) {
    super(webClient, objectMapper);
  }

  @Override
  public EnhanceTransactionResponse getCleansedMerchantName(
      String merchantName, Integer categoryCode) {
    return mockTransactionResponse;
  }

  @Override
  public String getMerchantLogo(String merchantGuid) {
    return mockMerchantLogo;
  }

  @Override
  public EnhanceTransactionResponse enhanceTransactions(List<AccountActivity> activities) {
    return mockTransactionResponse;
  }
}
