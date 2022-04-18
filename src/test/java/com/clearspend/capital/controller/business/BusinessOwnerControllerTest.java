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
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.controller.type.business.owner.BusinessOwnerInfo;
import com.clearspend.capital.controller.type.business.owner.CreateBusinessOwnerResponse;
import com.clearspend.capital.controller.type.business.owner.CreateOrUpdateBusinessOwnerRequest;
import com.clearspend.capital.controller.type.business.owner.OwnersProvidedRequest;
import com.clearspend.capital.controller.type.business.owner.OwnersProvidedResponse;
import com.clearspend.capital.crypto.data.model.embedded.EncryptedString;
import com.clearspend.capital.data.model.User;
import com.clearspend.capital.data.model.business.Business;
import com.clearspend.capital.data.model.business.BusinessOwner;
import com.clearspend.capital.data.model.business.BusinessProspect;
import com.clearspend.capital.data.model.enums.BusinessOnboardingStep;
import com.clearspend.capital.data.model.enums.BusinessStatus;
import com.clearspend.capital.data.model.enums.Country;
import com.clearspend.capital.data.repository.UserRepository;
import com.clearspend.capital.data.repository.business.BusinessOwnerRepository;
import com.clearspend.capital.service.BusinessOwnerService;
import com.clearspend.capital.service.ServiceHelper;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import com.stripe.model.Account;
import com.stripe.model.Event;
import io.jsonwebtoken.lang.Collections;
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

  private final BusinessOwnerService businessOwnerService;
  private final StripeConnectHandler stripeConnectHandler;
  private final ServiceHelper serviceHelper;
  private final BusinessOwnerRepository businessOwnerRepository;
  private final UserRepository userRepository;

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
      BusinessOwnerService businessOwnerService,
      StripeConnectHandler stripeConnectHandler,
      ServiceHelper serviceHelper,
      BusinessOwnerRepository businessOwnerRepository,
      UserRepository userRepository,
      @Value("classpath:stripeResponses/createAccount.json") Resource createAccount,
      @Value("classpath:stripeResponses/requiredDocumentsForPersonAndSSNLast4.json") @NonNull
          Resource requiredDocumentsForPersonAndSSNLast4) {
    this.mvc = mvc;
    this.testHelper = testHelper;
    this.businessOwnerService = businessOwnerService;
    this.stripeConnectHandler = stripeConnectHandler;
    this.businessOwnerRepository = businessOwnerRepository;
    this.createAccount = createAccount;
    this.requiredDocumentsForPersonAndSSNLast4 = requiredDocumentsForPersonAndSSNLast4;
    this.serviceHelper = serviceHelper;
    this.userRepository = userRepository;
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

    CreateOrUpdateBusinessOwnerRequest request =
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
            true);

    String body = objectMapper.writeValueAsString(request);
    MockHttpServletResponse response =
        mvc.perform(
                post("/business-owners/create")
                    .contentType("application/json")
                    .content(body)
                    .cookie(onboardBusinessRecord.cookie()))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

    CreateBusinessOwnerResponse createBusinessOwnerResponse =
        objectMapper.readValue(response.getContentAsString(), CreateBusinessOwnerResponse.class);

    Assertions.assertNull(createBusinessOwnerResponse.getErrorMessages());
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
        serviceHelper
            .businessOwnerService()
            .retrieveBusinessOwner(onboardBusinessRecord.businessOwner().getId());
    Assertions.assertNotNull(businessOwner.getStripePersonReference());
  }

  @SneakyThrows
  @Test
  void updateBusinessOwner_ToHardFail() {
    Business business = onboardBusinessRecord.business();
    BusinessOwner businessOwner = onboardBusinessRecord.businessOwner();

    CreateOrUpdateBusinessOwnerRequest request =
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
            true);

    String body = objectMapper.writeValueAsString(request);
    mvc.perform(
            patch("/business-owners/update")
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

    final User user =
        userRepository
            .findById(new TypedId<>(onboardBusinessRecord.businessOwner().getId().toUuid()))
            .orElseThrow(() -> new RuntimeException("Can't find user"));
    testHelper.setCurrentUserAsWebhook(user);
    stripeConnectHandler.accountUpdated(
        event, (Account) event.getDataObjectDeserializer().deserializeUnsafe());

    serviceHelper.businessOwnerService().retrieveBusinessOwner(businessOwner.getId());
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

    CreateOrUpdateBusinessOwnerRequest request =
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
            true);

    String body = objectMapper.writeValueAsString(request);
    mvc.perform(
            patch("/business-owners/update")
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

    final User user =
        userRepository
            .findById(new TypedId<>(onboardBusinessRecord.businessOwner().getId().toUuid()))
            .orElseThrow(() -> new RuntimeException("Can't find user"));
    testHelper.runWithWebhookUser(
        user,
        () -> {
          stripeConnectHandler.accountUpdated(
              event, (Account) event.getDataObjectDeserializer().deserializeUnsafe());
        });

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

  @SneakyThrows
  @Test
  void trigggerAllOwnerProvided_success() {
    OwnersProvidedRequest request = new OwnersProvidedRequest(true, true);

    String body = objectMapper.writeValueAsString(request);
    MockHttpServletResponse response =
        mvc.perform(
                post("/business-owners/trigger-all-owners-provided")
                    .contentType("application/json")
                    .content(body)
                    .cookie(onboardBusinessRecord.cookie()))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

    OwnersProvidedResponse ownersProvidedResponse =
        objectMapper.readValue(response.getContentAsString(), OwnersProvidedResponse.class);

    Assertions.assertTrue(Collections.isEmpty(ownersProvidedResponse.getErrorMessages()));
  }

  @SneakyThrows
  @Test
  void triggerAllOwnerProvided_provideRepresentativeRequired() {
    BusinessOwner businessOwner = onboardBusinessRecord.businessOwner();

    businessOwner.setRelationshipRepresentative(false);
    businessOwnerRepository.save(businessOwner);

    OwnersProvidedRequest request = new OwnersProvidedRequest(true, true);

    String body = objectMapper.writeValueAsString(request);
    MockHttpServletResponse response =
        mvc.perform(
                post("/business-owners/trigger-all-owners-provided")
                    .contentType("application/json")
                    .content(body)
                    .cookie(onboardBusinessRecord.cookie()))
            .andExpect(status().is5xxServerError())
            .andReturn()
            .getResponse();

    Assertions.assertTrue(
        response.getContentAsString().contains("Please provide at least one representative"));
  }

  @SneakyThrows
  @Test
  void triggerAllOwnerProvided_provideExecutiveRequired() {
    BusinessOwner businessOwner = onboardBusinessRecord.businessOwner();

    businessOwner.setRelationshipRepresentative(true);
    businessOwner.setRelationshipOwner(true);
    businessOwner.setRelationshipExecutive(false);
    businessOwnerRepository.save(businessOwner);

    OwnersProvidedRequest request = new OwnersProvidedRequest(null, null);

    String body = objectMapper.writeValueAsString(request);
    MockHttpServletResponse response =
        mvc.perform(
                post("/business-owners/trigger-all-owners-provided")
                    .contentType("application/json")
                    .content(body)
                    .cookie(onboardBusinessRecord.cookie()))
            .andExpect(status().is5xxServerError())
            .andReturn()
            .getResponse();

    Assertions.assertTrue(
        response.getContentAsString().contains("Please provide the executive for business"));
  }

  @SneakyThrows
  @Test
  void triggerAllOwnerProvided_provideOwnerRequired() {
    BusinessOwner businessOwner = onboardBusinessRecord.businessOwner();

    businessOwner.setRelationshipRepresentative(true);
    businessOwner.setRelationshipExecutive(true);
    businessOwner.setRelationshipOwner(false);
    businessOwnerRepository.save(businessOwner);

    OwnersProvidedRequest request = new OwnersProvidedRequest(null, null);

    String body = objectMapper.writeValueAsString(request);
    MockHttpServletResponse response =
        mvc.perform(
                post("/business-owners/trigger-all-owners-provided")
                    .contentType("application/json")
                    .content(body)
                    .cookie(onboardBusinessRecord.cookie()))
            .andExpect(status().is5xxServerError())
            .andReturn()
            .getResponse();

    Assertions.assertTrue(
        response.getContentAsString().contains("Please provide owner details for business"));
  }
}
