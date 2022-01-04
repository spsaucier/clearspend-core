package com.clearspend.capital.client.alloy;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.crypto.data.model.embedded.NullableEncryptedString;
import com.clearspend.capital.data.model.Business;
import com.clearspend.capital.data.model.BusinessOwner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@RequiredArgsConstructor(onConstructor = @__({@Autowired}))
@Slf4j
public class AlloyClientTest extends BaseCapitalTest {

  private final AlloyClient alloyClient;

  @Test
  void testOnboardIndividual() {
    BusinessOwner owner = new BusinessOwner();
    owner.setLastName(new NullableEncryptedString("test"));
    alloyClient.onboardIndividual(owner, "test");
  }

  @Test
  void testOnboardBusiness() {
    Business business = new Business();
    business.setLegalName("LegalName");
    alloyClient.onboardBusiness(business);
  }

  @Test
  void testRunGroupEvaluation() {
    alloyClient.runGroupEvaluation("");
  }

  @Test
  void testGetEntityInformationForBusinessEntity() {
    alloyClient.getEntityInformationForBusinessEntity("");
  }

  @Test
  void testGetEntityInformationForIndividualEntity() {
    alloyClient.getEntityInformationForIndividualEntity("");
  }

  @Test
  void testGetEvaluationForBusinessEntity() {
    alloyClient.getEvaluationForBusinessEntity("", "");
  }

  @Test
  void testGetEvaluationForIndividualEntity() {
    alloyClient.getEvaluationForIndividualEntity("", "");
  }
}
