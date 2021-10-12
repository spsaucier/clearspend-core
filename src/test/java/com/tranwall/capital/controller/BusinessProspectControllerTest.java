package com.tranwall.capital.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tranwall.capital.BaseCapitalTest;
import com.tranwall.capital.controller.type.business.prospect.SetBusinessProspectPasswordRequest;
import com.tranwall.capital.controller.type.business.prospect.ValidateBusinessProspectIdentifierRequest.IdentifierType;
import com.tranwall.capital.data.repository.BusinessProspectRepository;
import com.tranwall.capital.service.BusinessProspectService.CreateBusinessProspectRecord;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

@RequiredArgsConstructor(onConstructor = @__({@Autowired}))
class BusinessProspectControllerTest extends BaseCapitalTest {

  private final MockMvc mvc;
  private final ControllerHelper controllerHelper;

  private final BusinessProspectRepository businessProspectRepository;

  @Test
  void createBusinessProspect_success() throws Exception {
    controllerHelper.createBusinessProspect();
  }

  @Test
  void validateBusinessProspectEmail_success() throws Exception {
    CreateBusinessProspectRecord createBusinessProspectRecord =
        controllerHelper.createBusinessProspect();
    controllerHelper.validateBusinessProspectIdentifier(
        IdentifierType.EMAIL,
        createBusinessProspectRecord.businessProspect().getId(),
        createBusinessProspectRecord.otp());
  }

  @Test
  void setBusinessProspectPhone_success() throws Exception {
    CreateBusinessProspectRecord createBusinessProspectRecord =
        controllerHelper.createBusinessProspect();
    controllerHelper.validateBusinessProspectIdentifier(
        IdentifierType.EMAIL,
        createBusinessProspectRecord.businessProspect().getId(),
        createBusinessProspectRecord.otp());
    controllerHelper.setBusinessProspectPhone(
        createBusinessProspectRecord.businessProspect().getId());
  }

  @Test
  void validateBusinessProspectPhone_success() throws Exception {
    CreateBusinessProspectRecord createBusinessProspectRecord =
        controllerHelper.createBusinessProspect();
    controllerHelper.validateBusinessProspectIdentifier(
        IdentifierType.EMAIL,
        createBusinessProspectRecord.businessProspect().getId(),
        createBusinessProspectRecord.otp());
    String otp =
        controllerHelper.setBusinessProspectPhone(
            createBusinessProspectRecord.businessProspect().getId());
    controllerHelper.validateBusinessProspectIdentifier(
        IdentifierType.PHONE, createBusinessProspectRecord.businessProspect().getId(), otp);
  }

  void setBusinessProspectPassword(UUID businessProspectId) throws Exception {
    SetBusinessProspectPasswordRequest request =
        new SetBusinessProspectPasswordRequest(controllerHelper.generatePassword());
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
    CreateBusinessProspectRecord createBusinessProspectRecord =
        controllerHelper.createBusinessProspect();
    controllerHelper.validateBusinessProspectIdentifier(
        IdentifierType.EMAIL,
        createBusinessProspectRecord.businessProspect().getId(),
        createBusinessProspectRecord.otp());
    String otp =
        controllerHelper.setBusinessProspectPhone(
            createBusinessProspectRecord.businessProspect().getId());
    controllerHelper.validateBusinessProspectIdentifier(
        IdentifierType.PHONE, createBusinessProspectRecord.businessProspect().getId(), otp);
    setBusinessProspectPassword(createBusinessProspectRecord.businessProspect().getId());
  }

  @Test
  void convertBusinessProspect_success() throws Exception {
    CreateBusinessProspectRecord createBusinessProspectRecord =
        controllerHelper.createBusinessProspect();
    controllerHelper.validateBusinessProspectIdentifier(
        IdentifierType.EMAIL,
        createBusinessProspectRecord.businessProspect().getId(),
        createBusinessProspectRecord.otp());
    String otp =
        controllerHelper.setBusinessProspectPhone(
            createBusinessProspectRecord.businessProspect().getId());
    controllerHelper.validateBusinessProspectIdentifier(
        IdentifierType.PHONE, createBusinessProspectRecord.businessProspect().getId(), otp);
    setBusinessProspectPassword(createBusinessProspectRecord.businessProspect().getId());
    controllerHelper.convertBusinessProspect(
        createBusinessProspectRecord.businessProspect().getId());
  }
}
