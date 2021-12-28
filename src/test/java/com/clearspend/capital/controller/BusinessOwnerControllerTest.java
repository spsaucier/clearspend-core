package com.clearspend.capital.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.TestHelper.OnboardBusinessRecord;
import com.clearspend.capital.controller.type.business.owner.CreateBusinessOwnerResponse;
import com.clearspend.capital.controller.type.business.owner.CreateOrUpdateBusinessOwnerRequest;
import com.clearspend.capital.data.model.BusinessProspect;
import javax.servlet.http.Cookie;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

@RequiredArgsConstructor(onConstructor = @__({@Autowired}))
@Transactional
class BusinessOwnerControllerTest extends BaseCapitalTest {

  private final MockMvc mvc;
  private final TestHelper testHelper;

  private Cookie authCookie;

  OnboardBusinessRecord onboardBusinessRecord;

  @BeforeEach
  void init() throws Exception {
    testHelper.init();
    this.authCookie = testHelper.login("business-owner-tester@clearspend.com", "Password1!");
    if (onboardBusinessRecord == null) {
      mockServerHelper.expectOtpViaEmail();
      mockServerHelper.expectOtpViaSms();
      mockServerHelper.expectEmailVerification("123456");
      mockServerHelper.expectPhoneVerification("123456");

      onboardBusinessRecord = testHelper.onboardBusiness();
    }
  }

  @SneakyThrows
  @Test
  void createBusinessOwner_success() {
    BusinessProspect businessProspect = onboardBusinessRecord.businessProspect();
    CreateOrUpdateBusinessOwnerRequest request =
        new CreateOrUpdateBusinessOwnerRequest(
            testHelper.generateFirstName(),
            testHelper.generateLastName(),
            testHelper.generateDateOfBirth(),
            testHelper.generateTaxIdentificationNumber(),
            testHelper.generateEmail(),
            testHelper.generatePhone(),
            testHelper.generateApiAddress());

    String body = objectMapper.writeValueAsString(request);

    MockHttpServletResponse response =
        mvc.perform(
                post("/business-owners")
                    .contentType("application/json")
                    .content(body)
                    .cookie(authCookie))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

    CreateBusinessOwnerResponse createBusinessOwnerResponse =
        objectMapper.readValue(response.getContentAsString(), CreateBusinessOwnerResponse.class);

    Assertions.assertNull(createBusinessOwnerResponse.getErrorMessage());
  }

  @SneakyThrows
  @Test
  void updateBusinessOwner_success() {
    BusinessProspect businessProspect = onboardBusinessRecord.businessProspect();
    CreateOrUpdateBusinessOwnerRequest request =
        new CreateOrUpdateBusinessOwnerRequest(
            businessProspect.getFirstName().getEncrypted(),
            businessProspect.getLastName().getEncrypted(),
            testHelper.generateDateOfBirth(),
            testHelper.generateTaxIdentificationNumber(),
            businessProspect.getEmail().getEncrypted(),
            businessProspect.getPhone().getEncrypted(),
            testHelper.generateApiAddress());

    String body = objectMapper.writeValueAsString(request);

    MockHttpServletResponse response =
        mvc.perform(
                patch(
                        String.format(
                            "/business-owners/%s", onboardBusinessRecord.businessOwner().getId()))
                    .contentType("application/json")
                    .content(body)
                    .cookie(authCookie))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
  }
}
