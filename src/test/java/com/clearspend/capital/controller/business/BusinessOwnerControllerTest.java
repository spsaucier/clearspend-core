package com.clearspend.capital.controller.business;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.TestHelper.OnboardBusinessRecord;
import com.clearspend.capital.client.stripe.webhook.controller.StripeConnectHandler;
import com.clearspend.capital.common.data.model.Address;
import com.clearspend.capital.controller.type.business.owner.BusinessOwnerInfo;
import com.clearspend.capital.controller.type.business.owner.CreateBusinessOwnerResponse;
import com.clearspend.capital.controller.type.business.owner.CreateOrUpdateBusinessOwnerRequest;
import com.clearspend.capital.crypto.data.model.embedded.EncryptedString;
import com.clearspend.capital.data.model.business.Business;
import com.clearspend.capital.data.model.business.BusinessOwner;
import com.clearspend.capital.data.model.business.BusinessProspect;
import com.clearspend.capital.data.model.enums.BusinessOnboardingStep;
import com.clearspend.capital.data.model.enums.BusinessStatus;
import com.clearspend.capital.data.model.enums.Country;
import com.clearspend.capital.service.BusinessOwnerService;
import com.clearspend.capital.service.BusinessService;
import com.clearspend.capital.service.ServiceHelper;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import com.stripe.model.Account;
import com.stripe.model.Event;
import java.io.FileReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

@Slf4j
class BusinessOwnerControllerTest extends BaseCapitalTest {

  private final MockMvc mvc;
  private final TestHelper testHelper;

  private final BusinessService businessService;
  private final BusinessOwnerService businessOwnerService;
  private final StripeConnectHandler stripeConnectHandler;
  private final ServiceHelper serviceHelper;

  private final Resource createAccount;
  private final Resource requiredDocumentsForPersonAndSSNLast4;

  OnboardBusinessRecord onboardBusinessRecord;

  Address address =
      new Address(
          new EncryptedString("13810 Shavano Wind"),
          new EncryptedString("San Antonio, Texas(TX), 78230"),
          "San Antonio",
          "Texas",
          new EncryptedString("78230"),
          Country.USA);

  @Autowired
  public BusinessOwnerControllerTest(
      MockMvc mvc,
      TestHelper testHelper,
      BusinessService businessService,
      BusinessOwnerService businessOwnerService,
      StripeConnectHandler stripeConnectHandler,
      ServiceHelper serviceHelper,
      @Value("classpath:stripeResponses/createAccount.json") Resource createAccount,
      @Value("classpath:stripeResponses/requiredDocumentsForPersonAndSSNLast4.json") @NonNull
          Resource requiredDocumentsForPersonAndSSNLast4) {
    this.mvc = mvc;
    this.testHelper = testHelper;
    this.businessService = businessService;
    this.businessOwnerService = businessOwnerService;
    this.stripeConnectHandler = stripeConnectHandler;
    this.createAccount = createAccount;
    this.requiredDocumentsForPersonAndSSNLast4 = requiredDocumentsForPersonAndSSNLast4;
    this.serviceHelper = serviceHelper;
  }

  @BeforeEach
  void init() throws Exception {
    if (onboardBusinessRecord == null) {
      onboardBusinessRecord = testHelper.onboardBusiness();
    }
  }

  @SneakyThrows
  @Test
  void createBusinessOwner_success() {
    BusinessOwner businessOwner = onboardBusinessRecord.businessOwner();

    List<CreateOrUpdateBusinessOwnerRequest> request =
        List.of(
            new CreateOrUpdateBusinessOwnerRequest(
                null,
                testHelper.generateFirstName(),
                testHelper.generateLastName(),
                businessOwner.getRelationshipOwner(),
                businessOwner.getRelationshipExecutive(),
                BigDecimal.valueOf(40),
                "CEO",
                LocalDate.of(1900, 1, 1),
                testHelper.generateTaxIdentificationNumber(),
                testHelper.generateEmail(),
                testHelper.generatePhone(),
                new com.clearspend.capital.controller.type.Address(address),
                true));

    String body = objectMapper.writeValueAsString(request);
    MockHttpServletResponse response =
        mvc.perform(
                post("/business-owners")
                    .contentType("application/json")
                    .content(body)
                    .cookie(onboardBusinessRecord.cookie()))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

    List<CreateBusinessOwnerResponse> createBusinessOwnerResponse =
        objectMapper.readValue(
            response.getContentAsString(),
            objectMapper
                .getTypeFactory()
                .constructParametricType(List.class, CreateBusinessOwnerResponse.class));

    Assertions.assertNull(createBusinessOwnerResponse.get(0).getErrorMessages());
  }

  @SneakyThrows
  @Test
  void updateBusinessOwner_success() {
    BusinessProspect businessProspect = onboardBusinessRecord.businessProspect();
    CreateOrUpdateBusinessOwnerRequest request =
        new CreateOrUpdateBusinessOwnerRequest(
            onboardBusinessRecord.businessOwner().getId(),
            businessProspect.getFirstName().getEncrypted(),
            businessProspect.getLastName().getEncrypted(),
            null,
            null,
            null,
            null,
            testHelper.generateDateOfBirth(),
            testHelper.generateTaxIdentificationNumber(),
            businessProspect.getEmail().getEncrypted(),
            null,
            testHelper.generateApiAddress(),
            true);
    request.setOnboarding(true);
    String body = objectMapper.writeValueAsString(request);

    mvc.perform(
            patch("/business-owners/update")
                .contentType("application/json")
                .content(body)
                .cookie(onboardBusinessRecord.cookie()))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse();

    BusinessOwner businessOwner =
        businessOwnerService.retrieveBusinessOwner(onboardBusinessRecord.businessOwner().getId());
    Assertions.assertNotNull(businessOwner.getStripePersonReference());
  }

  @SneakyThrows
  @Test
  void updateBusinessOwner_ToHardFail() {
    Business business = onboardBusinessRecord.business();
    BusinessOwner businessOwner = onboardBusinessRecord.businessOwner();

    List<CreateOrUpdateBusinessOwnerRequest> request =
        List.of(
            new CreateOrUpdateBusinessOwnerRequest(
                businessOwner.getId(),
                businessOwner.getFirstName().getEncrypted(),
                businessOwner.getLastName().getEncrypted(),
                businessOwner.getRelationshipOwner(),
                businessOwner.getRelationshipExecutive(),
                BigDecimal.valueOf(50),
                "Fraud",
                LocalDate.of(1900, 1, 1),
                testHelper.generateTaxIdentificationNumber(),
                businessOwner.getEmail().getEncrypted(),
                businessOwner.getPhone().getEncrypted(),
                new com.clearspend.capital.controller.type.Address(address),
                true));

    String body = objectMapper.writeValueAsString(request);
    mvc.perform(
            post("/business-owners")
                .contentType("application/json")
                .content(body)
                .cookie(onboardBusinessRecord.cookie()))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse();

    Event event =
        new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create()
            .fromJson(new FileReader(createAccount.getFile()), Event.class);

    event
        .getData()
        .setObject(
            JsonParser.parseString(
                    event
                        .getDataObjectDeserializer()
                        .getRawJson()
                        .replace(event.getAccount(), business.getStripeData().getAccountRef()))
                .getAsJsonObject());

    event
        .getData()
        .setObject(
            JsonParser.parseString(
                    event
                        .getDataObjectDeserializer()
                        .getRawJson()
                        .replace(
                            "\"disabled_reason\":\"requirements.past_due\"",
                            "\"disabled_reason\":\"rejected.fraud\""))
                .getAsJsonObject());

    stripeConnectHandler.accountUpdated(
        event, (Account) event.getDataObjectDeserializer().deserializeUnsafe());

    businessOwnerService.retrieveBusinessOwner(businessOwner.getId());
    Business businessResponse =
        serviceHelper.businessService().getBusiness(business.getId()).business();
    assertThat(businessResponse.getOnboardingStep())
        .isEqualTo(BusinessOnboardingStep.BUSINESS_OWNERS);
    assertThat(businessResponse.getStatus()).isEqualTo(BusinessStatus.CLOSED);
  }

  @SneakyThrows
  @Test
  void updateBusinessOwner_ToSoftFail() {
    Business business = onboardBusinessRecord.business();
    BusinessOwner businessOwner = onboardBusinessRecord.businessOwner();

    List<CreateOrUpdateBusinessOwnerRequest> request =
        List.of(
            new CreateOrUpdateBusinessOwnerRequest(
                businessOwner.getId(),
                businessOwner.getFirstName().getEncrypted(),
                businessOwner.getLastName().getEncrypted(),
                businessOwner.getRelationshipOwner(),
                businessOwner.getRelationshipExecutive(),
                BigDecimal.valueOf(40),
                "Review",
                LocalDate.of(1900, 1, 1),
                testHelper.generateTaxIdentificationNumber(),
                businessOwner.getEmail().getEncrypted(),
                businessOwner.getPhone().getEncrypted(),
                new com.clearspend.capital.controller.type.Address(address),
                true));

    String body = objectMapper.writeValueAsString(request);
    mvc.perform(
            post("/business-owners")
                .contentType("application/json")
                .content(body)
                .cookie(onboardBusinessRecord.cookie()))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse();

    Event event =
        new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create()
            .fromJson(new FileReader(requiredDocumentsForPersonAndSSNLast4.getFile()), Event.class);

    event
        .getData()
        .setObject(
            JsonParser.parseString(
                    event
                        .getDataObjectDeserializer()
                        .getRawJson()
                        .replace(event.getAccount(), business.getStripeData().getAccountRef()))
                .getAsJsonObject());

    stripeConnectHandler.accountUpdated(
        event, (Account) event.getDataObjectDeserializer().deserializeUnsafe());

    Business businessResponse =
        serviceHelper.businessService().getBusiness(business.getId()).business();
    assertThat(businessResponse.getOnboardingStep()).isEqualTo(BusinessOnboardingStep.SOFT_FAIL);
    assertThat(businessResponse.getStatus()).isEqualTo(BusinessStatus.ONBOARDING);
  }

  @SneakyThrows
  @Test
  void listBusinessOwners() {
    MockHttpServletResponse response =
        mvc.perform(get("/business-owners/list").cookie(onboardBusinessRecord.cookie()))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

    List<BusinessOwnerInfo> businessOwnerList =
        objectMapper.readValue(
            response.getContentAsString(),
            objectMapper
                .getTypeFactory()
                .constructParametricType(List.class, BusinessOwnerInfo.class));

    Assertions.assertEquals(1, businessOwnerList.size());
  }

  @SneakyThrows
  @Test
  void listBusinessOwners_forABusinessWithNoOwners() {

    mvc.perform(
            delete(
                    String.format(
                        "/business-owners/%s", onboardBusinessRecord.businessOwner().getId()))
                .cookie(onboardBusinessRecord.cookie()))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse();
    MockHttpServletResponse response =
        mvc.perform(get("/business-owners/list").cookie(onboardBusinessRecord.cookie()))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

    List<BusinessOwnerInfo> businessOwnerList =
        objectMapper.readValue(
            response.getContentAsString(),
            objectMapper
                .getTypeFactory()
                .constructParametricType(List.class, BusinessOwnerInfo.class));

    Assertions.assertEquals(0, businessOwnerList.size());
  }
}
