package com.tranwall.capital.controller;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.javafaker.Faker;
import com.tranwall.capital.controller.type.Address;
import com.tranwall.capital.controller.type.business.prospect.ConvertBusinessProspectRequest;
import com.tranwall.capital.controller.type.business.prospect.ConvertBusinessProspectResponse;
import com.tranwall.capital.controller.type.business.prospect.CreateBusinessProspectRequest;
import com.tranwall.capital.controller.type.business.prospect.CreateBusinessProspectResponse;
import com.tranwall.capital.controller.type.business.prospect.SetBusinessProspectPhoneRequest;
import com.tranwall.capital.controller.type.business.prospect.SetBusinessProspectPhoneResponse;
import com.tranwall.capital.controller.type.business.prospect.ValidateBusinessProspectIdentifierRequest;
import com.tranwall.capital.controller.type.business.prospect.ValidateBusinessProspectIdentifierRequest.IdentifierType;
import com.tranwall.capital.data.model.Business;
import com.tranwall.capital.data.model.BusinessOwner;
import com.tranwall.capital.data.model.BusinessProspect;
import com.tranwall.capital.data.model.enums.BusinessType;
import com.tranwall.capital.data.model.enums.Country;
import com.tranwall.capital.data.repository.BusinessProspectRepository;
import com.tranwall.capital.service.BusinessOwnerService;
import com.tranwall.capital.service.BusinessProspectService.CreateBusinessProspectRecord;
import com.tranwall.capital.service.BusinessService;
import com.tranwall.capital.util.PhoneUtil;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.MockMvc;

@Component
@RequiredArgsConstructor
@Slf4j
public class ControllerHelper {

  public final ObjectMapper objectMapper =
      new ObjectMapper()
          .registerModule(new JavaTimeModule())
          .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
          .disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)
          .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

  private final MockMvc mvc;

  private final BusinessProspectRepository businessProspectRepository;

  private final BusinessService businessService;
  private final BusinessOwnerService businessOwnerService;

  private final Faker faker = new Faker();

  record OnboardBusinessRecord(
      Business business, BusinessOwner businessOwner, BusinessProspect businessProspect) {}

  public String generateFirstName() {
    return faker.name().firstName();
  }

  public String generateLastName() {
    return faker.name().lastName();
  }

  public String generateEmail() {
    return randomUUID() + "@tranwall.com";
  }

  public String generatePhone() {
    return PhoneUtil.randomPhoneNumber();
  }

  public String generateTaxIdentificationNumber() {
    return faker.number().digits(9);
  }

  public LocalDate generateDateOfBirth() {
    return new java.sql.Date(faker.date().birthday(18, 100).getTime()).toLocalDate();
  }

  public String generatePassword() {
    return faker.internet().password(10, 32, true, true, true);
  }

  public String generateBusinessName() {
    return faker.company().name();
  }

  public Address generateAddress() {
    return new Address(
        faker.address().streetAddress(),
        faker.address().secondaryAddress(),
        faker.address().city(),
        faker.address().state(),
        faker.address().zipCode(),
        Country.USA);
  }

  public OnboardBusinessRecord onboardBusiness() throws Exception {
    // create business prospect including setting email (returns email OTP)
    CreateBusinessProspectRecord createBusinessProspectRecord = createBusinessProspect();

    // validate email OTP
    validateBusinessProspectIdentifier(
        IdentifierType.EMAIL,
        createBusinessProspectRecord.businessProspect().getId(),
        createBusinessProspectRecord.otp());

    // set business phone (returns phone OTP)
    String otp = setBusinessProspectPhone(createBusinessProspectRecord.businessProspect().getId());

    // validate phone OTP
    validateBusinessProspectIdentifier(
        IdentifierType.PHONE, createBusinessProspectRecord.businessProspect().getId(), otp);

    // convert the prospect to a business
    ConvertBusinessProspectResponse convertBusinessProspectResponse =
        convertBusinessProspect(createBusinessProspectRecord.businessProspect().getId());

    return new OnboardBusinessRecord(
        businessService.retrieveBusiness(
            createBusinessProspectRecord.businessProspect().getBusinessId()),
        businessOwnerService.retrieveBusinessOwner(
            convertBusinessProspectResponse.getBusinessOwnerId()),
        createBusinessProspectRecord.businessProspect());
  }

  public CreateBusinessProspectRecord createBusinessProspect() throws Exception {
    CreateBusinessProspectRequest request =
        new CreateBusinessProspectRequest(generateEmail(), generateFirstName(), generateLastName());
    String body = objectMapper.writeValueAsString(request);

    MockHttpServletResponse response =
        mvc.perform(post("/business-prospects").contentType("application/json").content(body))
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

  void validateBusinessProspectIdentifier(
      IdentifierType identifierType, UUID businessProspectId, String otp) throws Exception {
    ValidateBusinessProspectIdentifierRequest request =
        new ValidateBusinessProspectIdentifierRequest(identifierType, otp);
    String body = objectMapper.writeValueAsString(request);

    MockHttpServletResponse response =
        mvc.perform(
                post(String.format(
                        "/business-prospects/%s/validate-identifier", businessProspectId))
                    .contentType("application/json")
                    .content(body))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
  }

  String setBusinessProspectPhone(UUID businessProspectId) throws Exception {
    SetBusinessProspectPhoneRequest request = new SetBusinessProspectPhoneRequest(generatePhone());
    String body = objectMapper.writeValueAsString(request);

    MockHttpServletResponse response =
        mvc.perform(
                post(String.format("/business-prospects/%s/phone", businessProspectId))
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

  ConvertBusinessProspectResponse convertBusinessProspect(UUID businessProspectId)
      throws Exception {
    ConvertBusinessProspectRequest request =
        new ConvertBusinessProspectRequest(
            generateBusinessName(),
            BusinessType.C_CORP,
            generateTaxIdentificationNumber(),
            generatePhone(),
            generateAddress());
    String body = objectMapper.writeValueAsString(request);

    MockHttpServletResponse response =
        mvc.perform(
                post(String.format("/business-prospects/%s/convert", businessProspectId))
                    .contentType("application/json")
                    .content(body))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

    return objectMapper.readValue(response.getContentAsString(), new TypeReference<>() {});
  }
}
