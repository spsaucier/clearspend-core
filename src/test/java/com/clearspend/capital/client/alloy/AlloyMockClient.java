package com.clearspend.capital.client.alloy;

import com.clearspend.capital.client.alloy.response.EntityInformation;
import com.clearspend.capital.client.alloy.response.OnboardResponse;
import com.clearspend.capital.data.model.Business;
import com.clearspend.capital.data.model.BusinessOwner;
import com.clearspend.capital.data.model.enums.KnowYourBusinessStatus;
import com.clearspend.capital.data.model.enums.KnowYourCustomerStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;

@Profile("test")
@Configuration
public class AlloyMockClient extends AlloyClient {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Value("classpath:alloyResource/success/runGroupEvaluationSuccess.json")
  Resource groupEvaluationSuccess;

  @Value("classpath:alloyResource/success/kybSuccess.json")
  Resource kybSuccess;

  @Value("classpath:alloyResource/softfail/kybSoftFail.json")
  Resource kybSoftFail;

  @Value("classpath:alloyResource/success/kycSuccess.json")
  Resource kycSuccess;

  @Value("classpath:alloyResource/softfail/kycEntityInfo.json")
  Resource kycEntityInfoSoftFail;

  @Value("classpath:alloyResource/success/kybEntityInfo.json")
  Resource kybEntityInfoSuccess;

  public AlloyMockClient() {
    super(null, null, null, null);
  }

  @Override
  public KycEvaluationResponse onboardIndividual(BusinessOwner owner, String alloyGroup) {
    return switch (owner.getLastName().getEncrypted()) {
      case "Denied" -> new KycEvaluationResponse(
          "", KnowYourCustomerStatus.FAIL, Collections.emptyList());
      case "Review" -> new KycEvaluationResponse(
          "", KnowYourCustomerStatus.REVIEW, Collections.emptyList());
      default -> new KycEvaluationResponse(
          "", KnowYourCustomerStatus.PASS, Collections.emptyList());
    };
  }

  @Override
  public KybEvaluationResponse onboardBusiness(Business business) {
    return switch (business.getLegalName()) {
      case "BusinessDenied" -> new KybEvaluationResponse(
          "", KnowYourBusinessStatus.FAIL, Collections.emptyList());
      case "BusinessReview" -> new KybEvaluationResponse(
          "", KnowYourBusinessStatus.REVIEW, Collections.emptyList());
      default -> new KybEvaluationResponse(
          "", KnowYourBusinessStatus.PASS, Collections.emptyList());
    };
  }

  @Override
  @SneakyThrows
  public OnboardResponse runGroupEvaluation(String groupExternalToken) {
    return objectMapper.readValue(groupEvaluationSuccess.getFile(), OnboardResponse.class);
  }

  @SneakyThrows
  public EntityInformation getEntityInformationForBusinessEntity(String entityToken) {
    return objectMapper.readValue(kybEntityInfoSuccess.getFile(), EntityInformation.class);
  }

  @SneakyThrows
  public EntityInformation getEntityInformationForIndividualEntity(String entityToken) {
    return objectMapper.readValue(kycEntityInfoSoftFail.getFile(), EntityInformation.class);
  }

  @SneakyThrows
  public OnboardResponse getEvaluationForBusinessEntity(
      String entityToken, String evaluationToken) {
    return objectMapper.readValue(kybSuccess.getFile(), OnboardResponse.class);
  }

  @SneakyThrows
  public OnboardResponse getEvaluationForIndividualEntity(
      String entityToken, String evaluationToken) {
    return objectMapper.readValue(kycSuccess.getFile(), OnboardResponse.class);
  }
}
