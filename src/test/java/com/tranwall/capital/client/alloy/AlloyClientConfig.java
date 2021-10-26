package com.tranwall.capital.client.alloy;

import com.tranwall.capital.data.model.enums.KnowYourBusinessStatus;
import com.tranwall.capital.data.model.enums.KnowYourCustomerStatus;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AlloyClientConfig {

  @Bean
  public AlloyClient alloyClient() {
    AlloyClient alloyClient = Mockito.mock(AlloyClient.class);
    Mockito.when(alloyClient.onboardBusiness(Mockito.any()))
        .thenReturn(KnowYourBusinessStatus.PASS);
    Mockito.when(alloyClient.onboardIndividual(Mockito.any()))
        .thenReturn(KnowYourCustomerStatus.PASS);
    return alloyClient;
  }
}
