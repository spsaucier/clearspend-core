package com.clearspend.capital.client.mx;

import com.clearspend.capital.client.mx.types.EnhanceTransactionResponse;
import com.clearspend.capital.client.mx.types.TransactionRecordResponse;
import com.clearspend.capital.data.model.AccountActivity;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Profile("test")
@Component
public class MxMockClient extends MxClient {

  public MxMockClient(@Qualifier("mxWebClient") WebClient webClient, ObjectMapper objectMapper) {
    super(webClient, objectMapper);
  }

  @Override
  public EnhanceTransactionResponse getCleansedMerchantName(
      String merchantName, Integer categoryCode) {
    return generateTransactionResponse(merchantName, categoryCode);
  }

  @Override
  public String getMerchantLogo(String merchantGuid) {
    return "https://example.com/" + RandomStringUtils.randomAlphabetic(15);
  }

  @Override
  public EnhanceTransactionResponse enhanceTransactions(List<AccountActivity> activities) {
    return generateTransactionResponse(
        RandomStringUtils.randomAlphabetic(20), RandomUtils.nextInt());
  }

  private EnhanceTransactionResponse generateTransactionResponse(
      String merchantName, Integer categoryCode) {
    return new EnhanceTransactionResponse(
        List.of(
            new TransactionRecordResponse(
                RandomStringUtils.randomAlphanumeric(20),
                merchantName,
                categoryCode,
                RandomStringUtils.randomAlphabetic(20),
                RandomStringUtils.randomAlphabetic(20),
                RandomStringUtils.randomAlphabetic(20))));
  }
}
