package com.clearspend.capital.controller.nonprod;

import static com.clearspend.capital.crypto.utils.CurrentUserSwitcher.setCurrentUser;
import static java.util.stream.Collectors.joining;

import com.clearspend.capital.client.plaid.PlaidClient;
import com.clearspend.capital.client.stripe.StripeClient;
import com.clearspend.capital.common.data.model.Address;
import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.common.data.util.HttpReqRespUtils;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessBankAccountId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.controller.nonprod.type.testdata.CreateTestDataResponse;
import com.clearspend.capital.controller.nonprod.type.testdata.CreateTestDataResponse.TestBusiness;
import com.clearspend.capital.controller.nonprod.type.testdata.GetBusinessesResponse;
import com.clearspend.capital.controller.type.business.owner.OwnersProvidedRequest;
import com.clearspend.capital.controller.type.review.ApplicationReviewRequirements;
import com.clearspend.capital.controller.type.review.ApplicationReviewRequirements.KycDocuments;
import com.clearspend.capital.controller.type.user.UpdateUserRequest;
import com.clearspend.capital.crypto.data.model.embedded.EncryptedString;
import com.clearspend.capital.crypto.utils.CurrentUserSwitcher.SwitchesCurrentUser;
import com.clearspend.capital.data.model.Account;
import com.clearspend.capital.data.model.Allocation;
import com.clearspend.capital.data.model.Card;
import com.clearspend.capital.data.model.User;
import com.clearspend.capital.data.model.business.Business;
import com.clearspend.capital.data.model.business.BusinessBankAccount;
import com.clearspend.capital.data.model.business.BusinessOwner;
import com.clearspend.capital.data.model.enums.AllocationReallocationType;
import com.clearspend.capital.data.model.enums.BankAccountTransactType;
import com.clearspend.capital.data.model.enums.BusinessType;
import com.clearspend.capital.data.model.enums.Country;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.data.model.enums.FundingType;
import com.clearspend.capital.data.model.enums.LimitPeriod;
import com.clearspend.capital.data.model.enums.LimitType;
import com.clearspend.capital.data.model.enums.MerchantType;
import com.clearspend.capital.data.model.enums.UserType;
import com.clearspend.capital.data.model.enums.card.BinType;
import com.clearspend.capital.data.model.enums.card.CardType;
import com.clearspend.capital.data.model.enums.network.NetworkMessageType;
import com.clearspend.capital.data.model.network.StripeWebhookLog;
import com.clearspend.capital.data.model.security.DefaultRoles;
import com.clearspend.capital.data.repository.AllocationRepository;
import com.clearspend.capital.data.repository.CardRepository;
import com.clearspend.capital.data.repository.UserRepository;
import com.clearspend.capital.data.repository.business.BusinessBankAccountRepository;
import com.clearspend.capital.data.repository.business.BusinessRepository;
import com.clearspend.capital.service.AllocationService;
import com.clearspend.capital.service.AllocationService.AllocationRecord;
import com.clearspend.capital.service.ApplicationReviewService;
import com.clearspend.capital.service.BusinessBankAccountService;
import com.clearspend.capital.service.BusinessOwnerService;
import com.clearspend.capital.service.BusinessOwnerService.CreateBusinessOwner;
import com.clearspend.capital.service.BusinessOwnerService.TestDataBusinessOp;
import com.clearspend.capital.service.BusinessProspectService;
import com.clearspend.capital.service.BusinessProspectService.BusinessProspectRecord;
import com.clearspend.capital.service.BusinessProspectService.ConvertBusinessProspectRecord;
import com.clearspend.capital.service.BusinessService;
import com.clearspend.capital.service.CardService;
import com.clearspend.capital.service.CardService.CardRecord;
import com.clearspend.capital.service.NetworkMessageService;
import com.clearspend.capital.service.UserService;
import com.clearspend.capital.service.UserService.CreateUpdateUserRecord;
import com.clearspend.capital.service.UserService.TestDataUserOp;
import com.clearspend.capital.service.type.BusinessOwnerData;
import com.clearspend.capital.service.type.ConvertBusinessProspect;
import com.clearspend.capital.service.type.NetworkCommon;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.javafaker.Faker;
import com.github.javafaker.Name;
import com.stripe.exception.StripeException;
import com.stripe.model.Person;
import com.stripe.model.issuing.Authorization;
import com.stripe.model.issuing.Authorization.AmountDetails;
import com.stripe.model.issuing.Authorization.MerchantData;
import com.stripe.model.issuing.Authorization.PendingRequest;
import com.stripe.model.issuing.Cardholder;
import com.stripe.model.issuing.Cardholder.Billing;
import com.stripe.model.issuing.Cardholder.Individual;
import com.stripe.model.issuing.Transaction;
import com.stripe.param.FileCreateParams.Purpose;
import com.stripe.param.PersonUpdateParams;
import com.stripe.param.PersonUpdateParams.Verification;
import com.stripe.param.PersonUpdateParams.Verification.Document;
import io.swagger.v3.oas.annotations.Parameter;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;
import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * This controller contains end points that are not deployed top production but allow the caller to
 * do things that they normally wouldn't be able to do.
 */
@Profile("!prod")
@SuppressWarnings("JavaTimeDefaultTimeZone")
@RestController
@RequestMapping("/non-production/test-data")
@RequiredArgsConstructor
@Slf4j
public class TestDataController {

  private String testPersonSSNSuccess = "000000000";
  private String testPersonSSNUnsuccess = "111111111";
  private String testPersonSSNSuccessImmediate = "222222222";
  private String einSuccess = "000000000";
  private String einSuccessImmediate = "222222222";
  private String einSuccessAsNonProfit = "000000001";
  private String einUnsuccess = "111111111";
  private LocalDate dobSuccess = LocalDate.of(1901, 1, 1);
  private LocalDate dobSuccessImmediate = LocalDate.of(1902, 1, 1);
  private LocalDate dobOfacAllert = LocalDate.of(1900, 1, 1);

  public static final ObjectMapper objectMapper =
      new ObjectMapper()
          .registerModule(new JavaTimeModule())
          .registerModule(new Jdk8Module())
          .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
          .disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)
          .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

  private static final Map<Currency, Map<LimitType, Map<LimitPeriod, BigDecimal>>>
      DEFAULT_TRANSACTION_LIMITS = Map.of(Currency.USD, new HashMap<>());

  private final AllocationService allocationService;
  private final BusinessBankAccountService businessBankAccountService;
  private final BusinessOwnerService businessOwnerService;
  private final BusinessService businessService;
  private final CardService cardService;
  private final NetworkMessageService networkMessageService;
  private final UserService userService;
  private final BusinessProspectService businessProspectService;
  private final ApplicationReviewService applicationReviewService;

  private final AllocationRepository allocationRepository;
  private final BusinessRepository businessRepository;
  private final CardRepository cardRepository;
  private final UserRepository userRepository;
  private final Faker faker = new Faker();

  private final StripeClient stripeClient;
  private final PlaidClient plaidClient;
  private final BusinessBankAccountRepository businessBankAccountRepository;

  public record BusinessRecord(
      Business business, List<BusinessOwner> businessOwners, User user, Allocation allocation) {}

  @GetMapping("/db-content")
  CreateTestDataResponse getDbContent() {
    return getAllData(null);
  }

  @GetMapping("/business/{businessId}")
  CreateTestDataResponse getBusiness(
      @PathVariable(value = "businessId")
          @Parameter(
              required = true,
              name = "businessId",
              description = "ID of the business record.",
              example = "48104ecb-1343-4cc1-b6f2-e6cc88e9a80f")
          TypedId<BusinessId> businessId) {
    return getAllData(businessId);
  }

  @GetMapping("/business")
  GetBusinessesResponse getBusinesses() {
    return new GetBusinessesResponse(
        businessRepository.findAll().stream()
            .map(com.clearspend.capital.controller.type.business.Business::new)
            .toList());
  }

  @SwitchesCurrentUser(reviewer = "jscarbor", explanation = "This class is for testing only")
  @GetMapping(value = "/create-all-demo", produces = MediaType.APPLICATION_JSON_VALUE)
  CreateTestDataResponse createTestData(
      @RequestHeader(value = HttpHeaders.USER_AGENT) String userAgent,
      HttpServletRequest httpServletRequest) {

    // create a new business
    BusinessRecord businessRecord =
        onboardNewBusiness(BusinessType.MULTI_MEMBER_LLC, userAgent, httpServletRequest);
    Business business = businessRecord.business();

    // FIXME(kuchlein): change this to follow a proper onboarding rather than ramming it through
    business
        .getStripeData()
        .setFinancialAccountRef(
            stripeClient
                .createFinancialAccount(business.getId(), business.getStripeData().getAccountRef())
                .getId());
    business = businessRepository.save(business);

    createUser(business);
    setCurrentUser(businessRecord.user(), Set.of(DefaultRoles.GLOBAL_APPLICATION_WEBHOOK));

    // create bankAccount, deposit $10,000 and withdraw $267.34
    BusinessBankAccount businessBankAccount =
        businessBankAccountService.createBusinessBankAccount(
            faker.number().digits(9),
            faker.number().digits(11),
            faker.funnyName().name(),
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString(),
            faker.funnyName().name(),
            business.getId());
    businessBankAccountService.transactBankAccount(
        business.getId(),
        businessBankAccount.getId(),
        businessRecord.user().getId(),
        BankAccountTransactType.DEPOSIT,
        Amount.of(business.getCurrency(), BigDecimal.valueOf(10000)),
        false);
    businessBankAccountService.transactBankAccount(
        business.getId(),
        businessBankAccount.getId(),
        businessRecord.user().getId(),
        BankAccountTransactType.WITHDRAW,
        Amount.of(business.getCurrency(), BigDecimal.valueOf(267.34)),
        false);

    Allocation parentAllocation = businessRecord.allocation();

    // create child allocation and load $1326.86
    AllocationRecord childAllocation =
        allocationService.createAllocation(
            business.getId(),
            parentAllocation.getId(),
            faker.company().name(),
            businessRecord.user(),
            Amount.of(Currency.USD),
            DEFAULT_TRANSACTION_LIMITS,
            Collections.emptySet(),
            Collections.emptySet());
    businessService.reallocateBusinessFunds(
        business.getId(),
        businessRecord.user.getId(),
        parentAllocation.getId(),
        childAllocation.allocation().getId(),
        Amount.of(business.getCurrency(), BigDecimal.valueOf(1326.86)));

    // create grandchild allocation and load $1926.27
    AllocationRecord grandchildAllocation =
        allocationService.createAllocation(
            business.getId(),
            childAllocation.allocation().getId(),
            faker.company().name(),
            businessRecord.user(),
            Amount.of(Currency.USD),
            DEFAULT_TRANSACTION_LIMITS,
            Collections.emptySet(),
            Collections.emptySet());
    businessService.reallocateBusinessFunds(
        business.getId(),
        businessRecord.user.getId(),
        parentAllocation.getId(),
        grandchildAllocation.allocation().getId(),
        Amount.of(business.getCurrency(), BigDecimal.valueOf(1926.27)));

    List<CreateUpdateUserRecord> users = new ArrayList<>();
    List<Card> cards = new ArrayList<>();

    CreateUpdateUserRecord user = createUser(business);
    users.add(user);
    CardRecord cardRecord =
        cardService.issueCard(
            BinType.DEBIT,
            FundingType.INDIVIDUAL,
            CardType.VIRTUAL,
            business.getId(),
            parentAllocation.getId(),
            user.user().getId(),
            business.getCurrency(),
            true,
            business.getLegalName(),
            Map.of(Currency.USD, new HashMap<>()),
            Collections.emptySet(),
            Collections.emptySet(),
            business.getClearAddress().toAddress());
    cards.add(cardRecord.card());
    allocationService.reallocateAllocationFunds(
        business,
        businessRecord.user.getId(),
        parentAllocation.getId(),
        parentAllocation.getAccountId(),
        cardRecord.card().getId(),
        AllocationReallocationType.ALLOCATION_TO_CARD,
        Amount.of(business.getCurrency(), BigDecimal.valueOf(173.45)));

    Amount amount = Amount.of(Currency.USD, BigDecimal.TEN);
    networkMessageService.processNetworkMessage(
        generateAuthorizationNetworkCommon(
                user.user(), cardRecord.card(), cardRecord.account(), amount)
            .networkCommon);

    cardRecord =
        cardService.issueCard(
            BinType.DEBIT,
            FundingType.INDIVIDUAL,
            CardType.PHYSICAL,
            business.getId(),
            parentAllocation.getId(),
            user.user().getId(),
            business.getCurrency(),
            true,
            business.getLegalName(),
            Map.of(Currency.USD, new HashMap<>()),
            Collections.emptySet(),
            Collections.emptySet(),
            business.getClearAddress().toAddress());
    cards.add(cardRecord.card());
    allocationService.reallocateAllocationFunds(
        business,
        businessRecord.user.getId(),
        parentAllocation.getId(),
        parentAllocation.getAccountId(),
        cardRecord.card().getId(),
        AllocationReallocationType.ALLOCATION_TO_CARD,
        Amount.of(business.getCurrency(), BigDecimal.valueOf(91.17)));

    amount = Amount.of(Currency.USD, BigDecimal.valueOf(26.27));
    networkMessageService.processNetworkMessage(
        generateAuthorizationNetworkCommon(
                user.user(), cardRecord.card(), cardRecord.account(), amount)
            .networkCommon);

    CreateUpdateUserRecord user2 = createUser(business);
    users.add(user2);
    cards.add(
        cardService
            .issueCard(
                BinType.DEBIT,
                FundingType.POOLED,
                CardType.VIRTUAL,
                business.getId(),
                childAllocation.allocation().getId(),
                user.user().getId(),
                business.getCurrency(),
                true,
                business.getLegalName(),
                Map.of(Currency.USD, new HashMap<>()),
                Collections.emptySet(),
                Collections.emptySet(),
                business.getClearAddress().toAddress())
            .card());
    cards.add(
        cardService
            .issueCard(
                BinType.DEBIT,
                FundingType.POOLED,
                CardType.PHYSICAL,
                business.getId(),
                childAllocation.allocation().getId(),
                user.user().getId(),
                business.getCurrency(),
                true,
                business.getLegalName(),
                Map.of(Currency.USD, new HashMap<>()),
                Collections.emptySet(),
                Collections.emptySet(),
                business.getClearAddress().toAddress())
            .card());

    // If this doesn't bomb, then something is right with the Stripe integration
    Name user2NewName = faker.name();
    userService.updateUser(
        new UpdateUserRequest(
            user2.user().getId(),
            user2.user().getBusinessId(),
            user2NewName.firstName(),
            user2NewName.lastName(),
            null,
            null,
            null,
            false));

    CreateUpdateUserRecord user3 = createUser(business);
    users.add(user3);
    cards.add(
        cardService
            .issueCard(
                BinType.DEBIT,
                FundingType.INDIVIDUAL,
                CardType.VIRTUAL,
                business.getId(),
                grandchildAllocation.allocation().getId(),
                user.user().getId(),
                business.getCurrency(),
                true,
                business.getLegalName(),
                Map.of(Currency.USD, new HashMap<>()),
                Collections.emptySet(),
                Collections.emptySet(),
                business.getClearAddress().toAddress())
            .card());

    return new CreateTestDataResponse(
        List.of(
            new TestBusiness(
                business,
                List.of(
                    businessRecord.allocation(),
                    childAllocation.allocation(),
                    grandchildAllocation.allocation()),
                cards,
                users)));
  }

  // This test data method is used to simulate an onboarding flow as in UI ,
  // and upload test documents provided by Stripe for application review.
  @GetMapping("/business/{type}/onboard")
  @TestDataBusinessOp(
      reviewer = "Craig Miller",
      explanation = "This is just for generating test data.")
  @CreateBusinessOwner(
      reviewer = "Craig Miller",
      explanation = "This is just for generating test data.")
  BusinessRecord onboardNewBusiness(
      @PathVariable(value = "type") BusinessType businessType,
      @RequestHeader(value = HttpHeaders.USER_AGENT) String userAgent,
      HttpServletRequest httpServletRequest) {

    BusinessProspectRecord businessProspect =
        businessProspectService.createOrUpdateBusinessProspect(
            faker.name().firstName(),
            faker.name().lastName(),
            businessType,
            true,
            true,
            false,
            false,
            faker.internet().safeEmailAddress(),
            HttpReqRespUtils.getClientIpAddressIfServletRequestExist(httpServletRequest),
            userAgent,
            false);
    businessProspectService.setBusinessProspectPhone(
        businessProspect.businessProspect().getId(), getValidPhoneNumber(), false);
    businessProspectService.setBusinessProspectPassword(
        businessProspect.businessProspect().getId(), "1234567890", false);
    ConvertBusinessProspectRecord convertBusinessProspectRecord =
        businessProspectService.convertBusinessProspect(
            new ConvertBusinessProspect(
                businessProspect.businessProspect().getId(),
                faker.company().name(),
                faker.company().name(),
                generateEmployerIdentificationNumber(),
                getValidPhoneNumber(),
                new Address(
                    new EncryptedString("13810 Shavano Wind"),
                    new EncryptedString("San Antonio, Texas(TX), 78230"),
                    "San Antonio",
                    "Texas",
                    new EncryptedString("78230"),
                    Country.USA),
                MerchantType.COMPUTERS_PERIPHERALS_AND_SOFTWARE,
                "Description of business",
                faker.internet().url()));

    Business business = convertBusinessProspectRecord.business();

    List<BusinessOwner> businessOwners =
        List.of(
            businessOwnerService.restrictedUpdateBusinessOwner(
                new BusinessOwnerData(
                    convertBusinessProspectRecord.businessOwner().getId(),
                    business.getId(),
                    convertBusinessProspectRecord.businessOwner().getFirstName().getEncrypted(),
                    convertBusinessProspectRecord.businessOwner().getLastName().getEncrypted(),
                    LocalDate.of(1902, 1, 1),
                    faker.number().digits(9),
                    convertBusinessProspectRecord.businessOwner().getRelationshipOwner(),
                    convertBusinessProspectRecord.businessOwner().getRelationshipRepresentative(),
                    convertBusinessProspectRecord.businessOwner().getRelationshipExecutive(),
                    convertBusinessProspectRecord.businessOwner().getRelationshipDirector(),
                    BigDecimal.valueOf(40),
                    "Title",
                    new Address(
                        new EncryptedString("13810 Shavano Wind"),
                        new EncryptedString("San Antonio, Texas(TX), 78230"),
                        "San Antonio",
                        "Texas",
                        new EncryptedString("78230"),
                        Country.USA),
                    convertBusinessProspectRecord.businessOwner().getEmail().getEncrypted(),
                    convertBusinessProspectRecord.businessOwner().getPhone().getEncrypted(),
                    null,
                    true)),
            businessOwnerService.restrictedCreateBusinessOwner(
                new BusinessOwnerData(
                    null,
                    business.getId(),
                    faker.name().firstName(),
                    faker.name().lastName(),
                    LocalDate.of(1902, 1, 1),
                    faker.number().digits(9),
                    true,
                    false,
                    true,
                    false,
                    BigDecimal.valueOf(40),
                    "Title Representative",
                    new Address(
                        new EncryptedString("13810 Shavano Wind"),
                        new EncryptedString("San Antonio, Texas(TX), 78230"),
                        "San Antonio",
                        "Texas",
                        new EncryptedString("78230"),
                        Country.USA),
                    faker.internet().safeEmailAddress(),
                    getValidPhoneNumber(),
                    null,
                    true)));

    AllocationRecord allocationRecord = convertBusinessProspectRecord.rootAllocationRecord();

    ApplicationReviewRequirements documentsForManualReview =
        applicationReviewService.getStripeApplicationRequirements(business.getId());

    documentsForManualReview
        .getKycRequiredDocuments()
        .forEach(kycOwnerDocuments -> uploadIdentityDocument(business, kycOwnerDocuments));

    // This is done again because on Stripe it can take some time to validate data
    // and first time will not return all the requirements

    documentsForManualReview =
        applicationReviewService.getStripeApplicationRequirements(business.getId());

    documentsForManualReview
        .getKycRequiredDocuments()
        .forEach(kycOwnerDocuments -> uploadIdentityDocument(business, kycOwnerDocuments));

    return new BusinessRecord(
        business,
        businessOwners,
        convertBusinessProspectRecord.user(),
        allocationRecord.allocation());
  }

  // This test data method is used to simulate an onboarding flow as in UI ,
  // and upload test documents provided by Stripe for application review.
  @GetMapping("/business/onboard/test-data")
  List<BusinessRecord> onboardNewBusiness_testData(
      @RequestHeader(value = HttpHeaders.USER_AGENT) String userAgent,
      HttpServletRequest httpServletRequest,
      @RequestParam(name = "unsuccessEIN", required = false, defaultValue = "false")
          Boolean unsuccessEIN) {

    List<BusinessRecord> businessRecords = new ArrayList<>();

    businessRecords.add(
        generateTestBusinessAndBusinessOwner(
            userAgent,
            httpServletRequest,
            BusinessType.PRIVATE_CORPORATION,
            false,
            true,
            true,
            unsuccessEIN ? einUnsuccess : generateEmployerIdentificationNumber(),
            faker.number().digits(9),
            LocalDate.of(1970, 1, 1)));

    return businessRecords;
  }

  @TestDataBusinessOp(
      explanation = "This is part of generating test data",
      reviewer = "Craig Miller")
  private BusinessRecord generateTestBusinessAndBusinessOwner(
      String userAgent,
      HttpServletRequest httpServletRequest,
      BusinessType businessType,
      Boolean prospectOwner,
      Boolean prospectRepresentative,
      Boolean prospectExecutive,
      String ein,
      String ssn,
      LocalDate dob) {

    List<BusinessOwner> businessOwners = new ArrayList<>();

    // prepare database for generating new test cases with EIN business issue
    if (einUnsuccess.equals(ein)) {
      prepareDatabaseToAcceptTestEIN(ein);
    }

    BusinessProspectRecord businessProspect =
        businessProspectService.createOrUpdateBusinessProspect(
            faker.name().firstName(),
            faker.name().lastName(),
            businessType,
            prospectOwner,
            prospectRepresentative,
            prospectExecutive,
            false,
            faker.internet().safeEmailAddress(),
            HttpReqRespUtils.getClientIpAddressIfServletRequestExist(httpServletRequest),
            userAgent,
            false);
    businessProspectService.setBusinessProspectPhone(
        businessProspect.businessProspect().getId(), getValidPhoneNumber(), false);
    businessProspectService.setBusinessProspectPassword(
        businessProspect.businessProspect().getId(), "1234567890", false);
    ConvertBusinessProspectRecord convertBusinessProspectRecord =
        businessProspectService.convertBusinessProspect(
            new ConvertBusinessProspect(
                businessProspect.businessProspect().getId(),
                faker.company().name(),
                faker.company().name(),
                ein,
                getValidPhoneNumber(),
                new Address(
                    new EncryptedString("13810 Shavano Wind"),
                    new EncryptedString("San Antonio, Texas(TX), 78230"),
                    "San Antonio",
                    "Texas",
                    new EncryptedString("78230"),
                    Country.USA),
                MerchantType.COMPUTERS_PERIPHERALS_AND_SOFTWARE,
                "Description of business",
                faker.internet().url()));

    Business business = convertBusinessProspectRecord.business();

    businessOwners.add(
        businessOwnerService
            .restrictedUpdateBusinessOwnerAndStripePerson(
                business.getId(),
                new BusinessOwnerData(
                    convertBusinessProspectRecord.businessOwner().getId(),
                    business.getId(),
                    convertBusinessProspectRecord.businessOwner().getFirstName().getEncrypted(),
                    convertBusinessProspectRecord.businessOwner().getLastName().getEncrypted(),
                    dob,
                    ssn,
                    convertBusinessProspectRecord.businessOwner().getRelationshipOwner(),
                    convertBusinessProspectRecord.businessOwner().getRelationshipRepresentative(),
                    convertBusinessProspectRecord.businessOwner().getRelationshipExecutive(),
                    convertBusinessProspectRecord.businessOwner().getRelationshipDirector(),
                    null,
                    "Title",
                    new Address(
                        new EncryptedString("13810 Shavano Wind"),
                        new EncryptedString("San Antonio, Texas(TX), 78230"),
                        "San Antonio",
                        "Texas",
                        new EncryptedString("78230"),
                        Country.USA),
                    convertBusinessProspectRecord.businessOwner().getEmail().getEncrypted(),
                    convertBusinessProspectRecord.businessOwner().getPhone().getEncrypted(),
                    null,
                    true))
            .businessOwner());

    businessOwnerService.restrictedAllOwnersProvided(
        business.getId(), new OwnersProvidedRequest(true, true));

    return new BusinessRecord(business, businessOwners, null, null);
  }

  private void prepareDatabaseToAcceptTestEIN(String ein) {
    Optional<Business> byEmployerIdentificationNumber =
        businessRepository.findByEmployerIdentificationNumber(ein);
    byEmployerIdentificationNumber.ifPresent(
        business -> {
          business.setEmployerIdentificationNumber(generateEmployerIdentificationNumber());
          businessRepository.save(business);
        });
  }

  private void uploadIdentityDocument(Business business, KycDocuments kycOwnerDocuments) {
    kycOwnerDocuments
        .documents()
        .forEach(
            requiredDocument -> {
              Person person =
                  stripeClient.retrievePerson(
                      requiredDocument.entityTokenId(), business.getStripeData().getAccountRef());

              try {
                com.stripe.model.File file =
                    stripeClient.uploadFile(
                        new FileInputStream(
                            new ClassPathResource("files/stripeTestFile/success.png").getFile()),
                        Purpose.IDENTITY_DOCUMENT,
                        business.getStripeData().getAccountRef());

                person.update(
                    PersonUpdateParams.builder()
                        .setVerification(
                            Verification.builder()
                                .setDocument(Document.builder().setFront(file.getId()).build())
                                .build())
                        .build());

              } catch (StripeException | IOException e) {
                log.error("Exception uploading identity document", e);
              }
            });
  }

  public Address generateEntityAddress() {
    com.github.javafaker.Address fakedAddress = faker.address();
    return new Address(
        new EncryptedString(fakedAddress.streetAddress()),
        new EncryptedString(fakedAddress.secondaryAddress()),
        fakedAddress.cityName(),
        fakedAddress.state(),
        new EncryptedString(fakedAddress.zipCode()),
        Country.USA);
  }

  public String generateEmployerIdentificationNumber() {
    return Integer.toString(faker.number().numberBetween(100_000_000, 999_999_999));
  }

  @TestDataUserOp(
      reviewer = "Craig Miller",
      explanation = "The method being called is to create users for the test data")
  public CreateUpdateUserRecord createUser(Business business) {
    return userService.createUserAndFusionAuthRecord(
        business.getId(),
        UserType.EMPLOYEE,
        faker.name().firstName(),
        faker.name().lastName(),
        generateEntityAddress(),
        faker.internet().emailAddress(),
        getValidPhoneNumber());
  }

  private CreateTestDataResponse getAllData(TypedId<BusinessId> businessId) {
    List<Business> businesses =
        businessId != null
            ? businessRepository.findById(businessId).stream().toList()
            : businessRepository.findAll();
    List<Allocation> allocations = allocationRepository.findAll();
    List<Card> cards = cardRepository.findAll();
    List<User> users = userRepository.findAll();

    return new CreateTestDataResponse(
        businesses.stream()
            .map(
                business ->
                    new CreateTestDataResponse.TestBusiness(
                        business,
                        allocations.stream()
                            .filter(
                                allocation -> allocation.getBusinessId().equals(business.getId()))
                            .toList(),
                        cards.stream()
                            .filter(card -> card.getBusinessId().equals(business.getId()))
                            .toList(),
                        users.stream()
                            .filter(user -> user.getBusinessId().equals(business.getId()))
                            .map(u -> new CreateUpdateUserRecord(u, null))
                            .toList()))
            .toList());
  }

  private static String generateStripeId(String prefix) {
    return prefix + RandomStringUtils.randomAlphanumeric(24);
  }

  public record NetworkCommonAuthorization(
      NetworkCommon networkCommon, Authorization authorization) {}

  public static NetworkCommonAuthorization generateAuthorizationNetworkCommon(
      User user, Card card, Account account, Amount amount) {
    Faker faker = Faker.instance();

    Cardholder cardholder = new Cardholder();

    Billing billing = new Billing();
    billing.setAddress(card.getShippingAddress().toStripeAddress());
    cardholder.setBilling(billing);
    cardholder.setCreated(user.getCreated().toEpochSecond());
    cardholder.setEmail(user.getEmail().getEncrypted());
    cardholder.setId("stripe_" + user.getId().toString());
    Individual individual = new Individual();
    individual.setFirstName(user.getFirstName().getEncrypted());
    individual.setLastName(user.getLastName().getEncrypted());
    cardholder.setIndividual(individual);
    cardholder.setName(card.getCardLine3());
    cardholder.setPhoneNumber(user.getPhone().getEncrypted());
    cardholder.setStatus("active");
    cardholder.setType("individual");

    com.stripe.model.issuing.Card stripeCard = new com.stripe.model.issuing.Card();
    stripeCard.setBrand("Visa");
    stripeCard.setCardholder(cardholder);
    stripeCard.setCreated(card.getCreated().toEpochSecond());
    stripeCard.setCurrency(account.getLedgerBalance().getCurrency().name());
    stripeCard.setExpMonth((long) card.getExpirationDate().getMonthValue());
    stripeCard.setExpYear((long) card.getExpirationDate().getYear());
    stripeCard.setId(card.getExternalRef());
    stripeCard.setLast4(card.getLastFour());
    stripeCard.setStatus("active");
    stripeCard.setType(card.getType().toStripeType());

    MerchantData merchantData = new MerchantData();
    merchantData.setCategory("bakeries");
    merchantData.setCategoryCode("5462");
    merchantData.setCity("Tucson");
    merchantData.setPostalCode("85641");
    merchantData.setState("AZ");
    merchantData.setCountry("US");
    merchantData.setName("Tuscon Bakery");
    merchantData.setNetworkId(faker.number().digits(10));

    PendingRequest pendingRequest = new PendingRequest();
    pendingRequest.setAmount(amount.toStripeAmount());
    pendingRequest.setCurrency(amount.getCurrency().name());
    pendingRequest.setIsAmountControllable(false);
    pendingRequest.setMerchantAmount(amount.toStripeAmount());
    pendingRequest.setMerchantCurrency(amount.getCurrency().name());
    AmountDetails amountDetails = new AmountDetails();
    amountDetails.setAtmFee(0L);
    pendingRequest.setAmountDetails(amountDetails);

    Authorization stripeAuthorization = new Authorization();
    stripeAuthorization.setAmount(0L);
    stripeAuthorization.setApproved(false);
    stripeAuthorization.setAuthorizationMethod("online");
    stripeAuthorization.setCard(stripeCard);
    stripeAuthorization.setCardholder(cardholder.getId());
    stripeAuthorization.setCreated(System.currentTimeMillis());
    stripeAuthorization.setCurrency(amount.getCurrency().name());
    stripeAuthorization.setId("stripe_" + UUID.randomUUID());
    stripeAuthorization.setMerchantAmount(amount.toStripeAmount());
    stripeAuthorization.setMerchantData(merchantData);
    stripeAuthorization.setPendingRequest(pendingRequest);
    stripeAuthorization.setStatus("pending");

    return new NetworkCommonAuthorization(
        new NetworkCommon(
            NetworkMessageType.AUTH_REQUEST, stripeAuthorization, new StripeWebhookLog()),
        stripeAuthorization);
  }

  public static NetworkCommon generateCaptureNetworkCommon(
      Business business, Authorization authorization) {

    Transaction transaction = new Transaction();
    transaction.setId(generateStripeId("ipi_"));
    transaction.setLivemode(false);
    transaction.setAmount(-authorization.getPendingRequest().getAmount());
    Transaction.AmountDetails amountDetails = new Transaction.AmountDetails();
    amountDetails.setAtmFee(authorization.getPendingRequest().getAmountDetails().getAtmFee());
    transaction.setAmountDetails(amountDetails);
    transaction.setAuthorization(authorization.getId());
    transaction.setBalanceTransaction(generateStripeId("txn_"));
    transaction.setCard(authorization.getCard().getId());
    transaction.setCardholder(authorization.getCard().getCardholder().getId());
    transaction.setCreated(OffsetDateTime.now().toEpochSecond());
    transaction.setCurrency(business.getCurrency().toStripeCurrency());
    transaction.setDispute(null);
    transaction.setMerchantAmount(-authorization.getPendingRequest().getAmount());
    transaction.setMerchantCurrency(business.getCurrency().toStripeCurrency());
    transaction.setMerchantData(authorization.getMerchantData());
    transaction.setMetadata(new HashMap<>());
    transaction.setObject("issuing.transaction");
    //    PurchaseDetails purchaseDetails;
    transaction.setType("capture");
    transaction.setWallet(null);

    StripeWebhookLog stripeWebhookLog = new StripeWebhookLog();
    stripeWebhookLog.setRequest("{}");

    return new NetworkCommon(transaction, stripeWebhookLog);
  }

  private String getValidPhoneNumber() {
    return PhoneUtil.randomPhoneNumber();
  }

  private static final class PhoneUtil {

    private PhoneUtil() {}

    // https://en.wikipedia.org/wiki/List_of_North_American_Numbering_Plan_area_codes#United_States
    private static final int[] areaCodes = {
      212, 315, 332, 347, 516, 518, 585, 607, 631, 646, 680, 716, 718, 838, 845, 914, 917, 929, 934,
      210, 214, 254, 281, 325, 346, 361, 409, 430, 432, 469, 512, 682, 713, 726, 737, 806, 817, 830,
      832, 903, 915, 936, 940, 956, 972, 979
    };

    public static String randomPhoneNumber() {
      ThreadLocalRandom random = ThreadLocalRandom.current();
      return String.format(
          "+1%d%d%s",
          areaCodes[random.nextInt(areaCodes.length)],
          random.nextInt(2, 10),
          IntStream.generate(() -> random.nextInt(10))
              .limit(6)
              .mapToObj(Integer::toString)
              .collect(joining()));
    }
  }

  @GetMapping("/plaid/un-link/{businessBankAccountId}")
  void unLink(
      @PathVariable(value = "businessBankAccountId")
          @Parameter(
              required = true,
              name = "businessBankAccountId",
              description = "ID of the businessBankAccount record.",
              example = "48104ecb-1343-4cc1-b6f2-e6cc88e9a80f")
          TypedId<BusinessBankAccountId> businessBankAccountId)
      throws IOException {
    BusinessBankAccount businessBankAccount =
        businessBankAccountRepository.findById(businessBankAccountId).orElseThrow();
    plaidClient.sandboxItemResetLogin(
        businessBankAccount.getBusinessId(), businessBankAccount.getAccessToken().getEncrypted());
  }
}
