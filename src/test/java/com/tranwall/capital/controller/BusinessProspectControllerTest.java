package com.tranwall.capital.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tranwall.capital.BaseCapitalTest;
import com.tranwall.capital.TestHelper;
import com.tranwall.capital.common.typedid.data.BusinessProspectId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.controller.type.business.prospect.SetBusinessProspectPasswordRequest;
import com.tranwall.capital.controller.type.business.prospect.ValidateBusinessProspectIdentifierRequest.IdentifierType;
import com.tranwall.capital.crypto.PasswordUtil;
import com.tranwall.capital.data.repository.BusinessProspectRepository;
import com.tranwall.capital.service.BusinessProspectService.CreateBusinessProspectRecord;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

@RequiredArgsConstructor(onConstructor = @__({@Autowired}))
class BusinessProspectControllerTest extends BaseCapitalTest {

  private final MockMvc mvc;
  private final TestHelper testHelper;

  private final BusinessProspectRepository businessProspectRepository;

  @Test
  void createBusinessProspect_success() throws Exception {
    testHelper.createBusinessProspect();
  }

  @Test
  void validateBusinessProspectEmail_success() throws Exception {
    CreateBusinessProspectRecord createBusinessProspectRecord = testHelper.createBusinessProspect();
    testHelper.validateBusinessProspectIdentifier(
        IdentifierType.EMAIL,
        createBusinessProspectRecord.businessProspect().getId(),
        createBusinessProspectRecord.otp());
  }

  @Test
  void setBusinessProspectPhone_success() throws Exception {
    CreateBusinessProspectRecord createBusinessProspectRecord = testHelper.createBusinessProspect();
    testHelper.validateBusinessProspectIdentifier(
        IdentifierType.EMAIL,
        createBusinessProspectRecord.businessProspect().getId(),
        createBusinessProspectRecord.otp());
    testHelper.setBusinessProspectPhone(createBusinessProspectRecord.businessProspect().getId());
  }

  @Test
  void validateBusinessProspectPhone_success() throws Exception {
    CreateBusinessProspectRecord createBusinessProspectRecord = testHelper.createBusinessProspect();
    testHelper.validateBusinessProspectIdentifier(
        IdentifierType.EMAIL,
        createBusinessProspectRecord.businessProspect().getId(),
        createBusinessProspectRecord.otp());
    String otp =
        testHelper.setBusinessProspectPhone(
            createBusinessProspectRecord.businessProspect().getId());
    testHelper.validateBusinessProspectIdentifier(
        IdentifierType.PHONE, createBusinessProspectRecord.businessProspect().getId(), otp);
  }

  void setBusinessProspectPassword(TypedId<BusinessProspectId> businessProspectId)
      throws Exception {
    SetBusinessProspectPasswordRequest request =
        new SetBusinessProspectPasswordRequest(PasswordUtil.generatePassword());
    String body = objectMapper.writeValueAsString(request);

    MockHttpServletResponse response =
        mvc.perform(
                post(String.format("/business-prospects/%s/password", businessProspectId))
                    .contentType("application/json")
                    .content(body))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
  }

  @Test
  void setBusinessProspectPassword_success() throws Exception {
    CreateBusinessProspectRecord createBusinessProspectRecord = testHelper.createBusinessProspect();
    testHelper.validateBusinessProspectIdentifier(
        IdentifierType.EMAIL,
        createBusinessProspectRecord.businessProspect().getId(),
        createBusinessProspectRecord.otp());
    String otp =
        testHelper.setBusinessProspectPhone(
            createBusinessProspectRecord.businessProspect().getId());
    testHelper.validateBusinessProspectIdentifier(
        IdentifierType.PHONE, createBusinessProspectRecord.businessProspect().getId(), otp);
    setBusinessProspectPassword(createBusinessProspectRecord.businessProspect().getId());
  }

  @Test
  void convertBusinessProspect_success() throws Exception {
    CreateBusinessProspectRecord createBusinessProspectRecord = testHelper.createBusinessProspect();
    testHelper.validateBusinessProspectIdentifier(
        IdentifierType.EMAIL,
        createBusinessProspectRecord.businessProspect().getId(),
        createBusinessProspectRecord.otp());
    String otp =
        testHelper.setBusinessProspectPhone(
            createBusinessProspectRecord.businessProspect().getId());
    testHelper.validateBusinessProspectIdentifier(
        IdentifierType.PHONE, createBusinessProspectRecord.businessProspect().getId(), otp);
    setBusinessProspectPassword(createBusinessProspectRecord.businessProspect().getId());
    testHelper.convertBusinessProspect(createBusinessProspectRecord.businessProspect().getId());
  }
}
