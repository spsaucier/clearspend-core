package com.tranwall.capital.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tranwall.capital.BaseCapitalTest;
import com.tranwall.capital.controller.type.ConvertBusinessProspectRequest;
import com.tranwall.capital.controller.type.ConvertBusinessProspectResponse;
import com.tranwall.capital.controller.type.CreateBusinessProspectRequest;
import com.tranwall.capital.controller.type.CreateBusinessProspectResponse;
import com.tranwall.capital.controller.type.SetBusinessProspectPasswordRequest;
import com.tranwall.capital.controller.type.SetBusinessProspectPhoneRequest;
import com.tranwall.capital.controller.type.SetBusinessProspectPhoneResponse;
import com.tranwall.capital.controller.type.ValidateBusinessProspectIdentifierRequest;
import com.tranwall.capital.controller.type.ValidateBusinessProspectIdentifierRequest.IdentifierType;
import com.tranwall.capital.data.model.BusinessProspect;
import com.tranwall.capital.data.model.enums.BusinessType;
import com.tranwall.capital.data.repository.BusinessProspectRepository;
import com.tranwall.capital.service.BusinessProspectService.CreateBusinessProspectRecord;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

@RequiredArgsConstructor(onConstructor = @__({@Autowired}))
class BusinessOnboardingControllerTest extends BaseCapitalTest {

  private final MockMvc mvc;
  private final ControllerHelper controllerHelper;

  private final BusinessProspectRepository businessProspectRepository;

  private final ObjectMapper objectMapper =
      new ObjectMapper()
          .registerModule(new JavaTimeModule())
          .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
          .disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)
          .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

  CreateBusinessProspectRecord createBusinessProspect() throws Exception {
    CreateBusinessProspectRequest request =
        new CreateBusinessProspectRequest(
            controllerHelper.generateEmail(),
            controllerHelper.generateFirstName(),
            controllerHelper.generateLastName());
    String body = objectMapper.writeValueAsString(request);

    MockHttpServletResponse response =
        mvc.perform(post("/business-prospect").contentType("application/json").content(body))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

    CreateBusinessProspectResponse createBusinessProspectResponse =
        objectMapper.readValue(response.getContentAsString(), new TypeReference<>() {});

    BusinessProspect businessProspect =
        businessProspectRepository
            .findById(createBusinessProspectResponse.getBusinessProspectId())
            .orElseThrow();
    assertThat(businessProspect.getEmail().getEncrypted()).isEqualTo(request.getEmail());
    assertThat(businessProspect.getFirstName().getEncrypted()).isEqualTo(request.getFirstName());
    assertThat(businessProspect.getLastName().getEncrypted()).isEqualTo(request.getLastName());

    return new CreateBusinessProspectRecord(
        businessProspect, createBusinessProspectResponse.getOtp());
  }

  @Test
  void createBusinessProspect_success() throws Exception {
    createBusinessProspect();
  }

  void validateBusinessProspectIdentifier(
      IdentifierType identifierType, UUID businessProspectId, String otp) throws Exception {
    ValidateBusinessProspectIdentifierRequest request =
        new ValidateBusinessProspectIdentifierRequest(identifierType, otp);
    String body = objectMapper.writeValueAsString(request);

    MockHttpServletResponse response =
        mvc.perform(
                post(String.format("/business-prospect/%s/validate-identifier", businessProspectId))
                    .contentType("application/json")
                    .content(body))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
  }

  @Test
  void validateBusinessProspectEmail_success() throws Exception {
    CreateBusinessProspectRecord createBusinessProspectRecord = createBusinessProspect();
    validateBusinessProspectIdentifier(
        IdentifierType.EMAIL,
        createBusinessProspectRecord.businessProspect().getId(),
        createBusinessProspectRecord.otp());
  }

  String setBusinessProspectPhone(UUID businessProspectId) throws Exception {
    SetBusinessProspectPhoneRequest request =
        new SetBusinessProspectPhoneRequest(controllerHelper.generatePhone());
    String body = objectMapper.writeValueAsString(request);

    MockHttpServletResponse response =
        mvc.perform(
                post(String.format("/business-prospect/%s/phone", businessProspectId))
                    .contentType("application/json")
                    .content(body))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

    return objectMapper
        .readValue(
            response.getContentAsString(), new TypeReference<SetBusinessProspectPhoneResponse>() {})
        .getOtp();
  }

  @Test
  void setBusinessProspectPhone_success() throws Exception {
    CreateBusinessProspectRecord createBusinessProspectRecord = createBusinessProspect();
    validateBusinessProspectIdentifier(
        IdentifierType.EMAIL,
        createBusinessProspectRecord.businessProspect().getId(),
        createBusinessProspectRecord.otp());
    setBusinessProspectPhone(createBusinessProspectRecord.businessProspect().getId());
  }

  @Test
  void validateBusinessProspectPhone_success() throws Exception {
    CreateBusinessProspectRecord createBusinessProspectRecord = createBusinessProspect();
    validateBusinessProspectIdentifier(
        IdentifierType.EMAIL,
        createBusinessProspectRecord.businessProspect().getId(),
        createBusinessProspectRecord.otp());
    String otp = setBusinessProspectPhone(createBusinessProspectRecord.businessProspect().getId());
    validateBusinessProspectIdentifier(
        IdentifierType.PHONE, createBusinessProspectRecord.businessProspect().getId(), otp);
  }

  void setBusinessProspectPassword(UUID businessProspectId) throws Exception {
    SetBusinessProspectPasswordRequest request =
        new SetBusinessProspectPasswordRequest(controllerHelper.generatePassword());
    String body = objectMapper.writeValueAsString(request);

    MockHttpServletResponse response =
        mvc.perform(
                post(String.format("/business-prospect/%s/password", businessProspectId))
                    .contentType("application/json")
                    .content(body))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
  }

  @Test
  void setBusinessProspectPassword_success() throws Exception {
    CreateBusinessProspectRecord createBusinessProspectRecord = createBusinessProspect();
    validateBusinessProspectIdentifier(
        IdentifierType.EMAIL,
        createBusinessProspectRecord.businessProspect().getId(),
        createBusinessProspectRecord.otp());
    String otp = setBusinessProspectPhone(createBusinessProspectRecord.businessProspect().getId());
    validateBusinessProspectIdentifier(
        IdentifierType.PHONE, createBusinessProspectRecord.businessProspect().getId(), otp);
    setBusinessProspectPassword(createBusinessProspectRecord.businessProspect().getId());
  }

  UUID convertBusinessProspect(UUID businessProspectId) throws Exception {
    ConvertBusinessProspectRequest request =
        new ConvertBusinessProspectRequest(
            controllerHelper.generateBusinessName(),
            BusinessType.C_CORP,
            LocalDate.of(2021, 9, 30),
            controllerHelper.generateAddress());
    String body = objectMapper.writeValueAsString(request);

    MockHttpServletResponse response =
        mvc.perform(
                post(String.format("/business-prospect/%s/convert", businessProspectId))
                    .contentType("application/json")
                    .content(body))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

    return objectMapper
        .readValue(
            response.getContentAsString(), new TypeReference<ConvertBusinessProspectResponse>() {})
        .getBusinessId();
  }

  @Test
  void convertBusinessProspect_success() throws Exception {
    CreateBusinessProspectRecord createBusinessProspectRecord = createBusinessProspect();
    validateBusinessProspectIdentifier(
        IdentifierType.EMAIL,
        createBusinessProspectRecord.businessProspect().getId(),
        createBusinessProspectRecord.otp());
    String otp = setBusinessProspectPhone(createBusinessProspectRecord.businessProspect().getId());
    validateBusinessProspectIdentifier(
        IdentifierType.PHONE, createBusinessProspectRecord.businessProspect().getId(), otp);
    setBusinessProspectPassword(createBusinessProspectRecord.businessProspect().getId());
    convertBusinessProspect(createBusinessProspectRecord.businessProspect().getId());
  }
}
