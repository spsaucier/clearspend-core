package com.tranwall.capital.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tranwall.capital.BaseCapitalTest;
import com.tranwall.capital.TestHelper;
import com.tranwall.capital.TestHelper.OnboardBusinessRecord;
import com.tranwall.capital.controller.type.business.owner.UpdateBusinessOwnerRequest;
import com.tranwall.capital.data.model.BusinessProspect;
import javax.servlet.http.Cookie;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

@RequiredArgsConstructor(onConstructor = @__({@Autowired}))
class BusinessOwnerControllerTest extends BaseCapitalTest {

  private final MockMvc mvc;
  private final TestHelper testHelper;

  private Cookie authCookie;

  OnboardBusinessRecord onboardBusinessRecord;

  @BeforeEach
  void init() throws Exception {
    if (onboardBusinessRecord == null) {
      mockServerHelper.expectOtpViaEmail();
      mockServerHelper.expectOtpViaSms();
      mockServerHelper.expectEmailVerification("123456");
      mockServerHelper.expectPhoneVerification("123456");

      onboardBusinessRecord = testHelper.onboardBusiness();
    }
    this.authCookie = testHelper.login("tester@tranwall.com", "Password1!");
  }

  @SneakyThrows
  @Test
  void updateBusinessOwner_success() {
    BusinessProspect businessProspect = onboardBusinessRecord.businessProspect();
    UpdateBusinessOwnerRequest request =
        new UpdateBusinessOwnerRequest(
            businessProspect.getFirstName().getEncrypted(),
            businessProspect.getLastName().getEncrypted(),
            testHelper.generateDateOfBirth(),
            testHelper.generateTaxIdentificationNumber(),
            businessProspect.getEmail().getEncrypted(),
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
