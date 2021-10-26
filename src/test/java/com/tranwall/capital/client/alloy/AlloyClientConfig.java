package com.tranwall.capital.client.alloy;

import com.tranwall.capital.data.model.Business;
import com.tranwall.capital.data.model.BusinessOwner;
import com.tranwall.capital.data.model.enums.KnowYourBusinessStatus;
import com.tranwall.capital.data.model.enums.KnowYourCustomerStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AlloyClientConfig {

  @Bean
  public AlloyClient alloyClient() {
    return new AlloyClient(null, null) {
      @Override
      public KnowYourCustomerStatus onboardIndividual(BusinessOwner owner) {
        return KnowYourCustomerStatus.PASS;
      }

      @Override
      public KnowYourBusinessStatus onboardBusiness(Business business) {
        return KnowYourBusinessStatus.PASS;
      }
    };
  }
}
