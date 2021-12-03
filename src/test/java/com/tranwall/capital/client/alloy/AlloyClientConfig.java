package com.tranwall.capital.client.alloy;

import com.tranwall.capital.data.model.Business;
import com.tranwall.capital.data.model.BusinessOwner;
import com.tranwall.capital.data.model.enums.KnowYourBusinessStatus;
import com.tranwall.capital.data.model.enums.KnowYourCustomerStatus;
import java.util.Collections;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AlloyClientConfig {

  @Bean
  public AlloyClient alloyClient() {
    return new AlloyClient(null, null, null) {
      @Override
      public KycEvaluationResponse onboardIndividual(BusinessOwner owner, String alloyGroup) {
        return new KycEvaluationResponse("", KnowYourCustomerStatus.PASS, Collections.emptyList());
      }

      @Override
      public KybEvaluationResponse onboardBusiness(Business business) {
        return new KybEvaluationResponse("", KnowYourBusinessStatus.PASS, Collections.emptyList());
      }
    };
  }
}
