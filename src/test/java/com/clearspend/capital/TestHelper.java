package com.clearspend.capital;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.clearspend.capital.client.plaid.PlaidClient;
import com.clearspend.capital.common.data.model.Address;
import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.BinId;
import com.clearspend.capital.common.typedid.data.BusinessBankAccountId;
import com.clearspend.capital.common.typedid.data.BusinessId;
import com.clearspend.capital.common.typedid.data.BusinessOwnerId;
import com.clearspend.capital.common.typedid.data.BusinessProspectId;
import com.clearspend.capital.common.typedid.data.ProgramId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.configuration.SecurityConfig;
import com.clearspend.capital.controller.BusinessBankAccountController.LinkTokenResponse;
import com.clearspend.capital.controller.type.business.prospect.BusinessProspectStatus;
import com.clearspend.capital.controller.type.business.prospect.ConvertBusinessProspectRequest;
import com.clearspend.capital.controller.type.business.prospect.ConvertBusinessProspectResponse;
import com.clearspend.capital.controller.type.business.prospect.CreateBusinessProspectRequest;
import com.clearspend.capital.controller.type.business.prospect.CreateBusinessProspectResponse;
import com.clearspend.capital.controller.type.business.prospect.SetBusinessProspectPhoneRequest;
import com.clearspend.capital.controller.type.business.prospect.ValidateBusinessProspectIdentifierRequest;
import com.clearspend.capital.controller.type.business.prospect.ValidateBusinessProspectIdentifierRequest.IdentifierType;
import com.clearspend.capital.controller.type.user.LoginRequest;
import com.clearspend.capital.crypto.PasswordUtil;
import com.clearspend.capital.crypto.data.model.embedded.EncryptedString;
import com.clearspend.capital.data.model.Account;
import com.clearspend.capital.data.model.Allocation;
import com.clearspend.capital.data.model.Business;
import com.clearspend.capital.data.model.BusinessBankAccount;
import com.clearspend.capital.data.model.BusinessOwner;
import com.clearspend.capital.data.model.BusinessProspect;
import com.clearspend.capital.data.model.Card;
import com.clearspend.capital.data.model.User;
import com.clearspend.capital.data.model.enums.BankAccountTransactType;
import com.clearspend.capital.data.model.enums.BusinessType;
import com.clearspend.capital.data.model.enums.Country;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.data.model.enums.FundingType;
import com.clearspend.capital.data.model.enums.LimitPeriod;
import com.clearspend.capital.data.model.enums.LimitType;
import com.clearspend.capital.data.model.enums.UserType;
import com.clearspend.capital.data.model.enums.card.BinType;
import com.clearspend.capital.data.model.enums.card.CardType;
import com.clearspend.capital.data.repository.AccountRepository;
import com.clearspend.capital.data.repository.AllocationRepository;
import com.clearspend.capital.data.repository.BusinessBankAccountRepository;
import com.clearspend.capital.data.repository.BusinessLimitRepository;
import com.clearspend.capital.data.repository.BusinessOwnerRepository;
import com.clearspend.capital.data.repository.BusinessProspectRepository;
import com.clearspend.capital.data.repository.BusinessRepository;
import com.clearspend.capital.data.repository.TransactionLimitRepository;
import com.clearspend.capital.data.repository.UserRepository;
import com.clearspend.capital.service.AccountService.AdjustmentAndHoldRecord;
import com.clearspend.capital.service.AllocationService;
import com.clearspend.capital.service.AllocationService.AllocationRecord;
import com.clearspend.capital.service.BusinessBankAccountService;
import com.clearspend.capital.service.BusinessOwnerService;
import com.clearspend.capital.service.BusinessOwnerService.BusinessOwnerAndUserRecord;
import com.clearspend.capital.service.BusinessProspectService;
import com.clearspend.capital.service.BusinessProspectService.BusinessProspectRecord;
import com.clearspend.capital.service.BusinessService;
import com.clearspend.capital.service.CardService;
import com.clearspend.capital.service.FusionAuthService;
import com.clearspend.capital.service.UserService;
import com.clearspend.capital.service.UserService.CreateUpdateUserRecord;
import com.clearspend.capital.util.PhoneUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.javafaker.Faker;
import java.io.IOException;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.servlet.http.Cookie;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class TestHelper {

  private static final Map<Currency, Map<LimitType, Map<LimitPeriod, BigDecimal>>>
      DEFAULT_TRANSACTION_LIMITS = Map.of(Currency.USD, new HashMap<>());

  public static final TypedId<BinId> binId = new TypedId<>("2691dad4-82f7-47ec-9cae-0686a22572fc");
  public static final TypedId<ProgramId> pooledProgramId =
      new TypedId<>("6faf3838-b2d7-422c-8d6f-c2294ebc73b4");
  public static final TypedId<ProgramId> individualProgramId =
      new TypedId<>("033955d1-f18e-497e-9905-88ba71e90208");
  /**
   * List of BusinessIds for testing. It's easy to search for these. Use {@link
   * #getNextBusinessId()} to get the first unused one from the list or to generate another one if
   * the test requires many.
   */
  public static final List<TypedId<BusinessId>> businessIds =
      Stream.of(
              "82a79d15-9e47-421b-ab8f-78532f4f8bc7",
              "961c7ab8-0f04-4322-a208-3e0e8a044835",
              "7fab4c66-959d-4130-980a-c3e4c93a8996",
              "651a69ce-ac11-43b5-83c4-e36aae34566e",
              "be808a40-fd9c-40de-b321-0c369972940a",
              "3e969bdb-3922-469d-9177-0e13139e048d",
              "09b17a56-c465-4335-b93b-1949b2fb71a8",
              "0220e85a-7c88-4045-ade1-55dd396377e6",
              "683dfc70-de16-40fd-8dc9-4386f024580f",
              "032b1e14-ccbd-4234-9f70-2c735324910a")
          .map(TypedId<BusinessId>::new)
          .collect(Collectors.toList());

  private final AccountRepository accountRepository;
  private final AllocationRepository allocationRepository;
  private final BusinessBankAccountRepository businessBankAccountRepository;
  private final BusinessLimitRepository businessLimitRepository;
  private final BusinessOwnerRepository businessOwnerRepository;
  private final BusinessProspectRepository businessProspectRepository;
  private final BusinessRepository businessRepository;
  private final TransactionLimitRepository transactionLimitRepository;
  private final UserRepository userRepository;

  private final AllocationService allocationService;
  private final BusinessBankAccountService businessBankAccountService;
  private final BusinessOwnerService businessOwnerService;
  private final BusinessProspectService businessProspectService;
  private final BusinessService businessService;
  private final CardService cardService;
  private final FusionAuthService fusionAuthService;
  private final UserService userService;
  private final PlaidClient plaidClient;

  private final Faker faker = new Faker(new SecureRandom(new byte[] {0}));

  public final ObjectMapper objectMapper =
      new ObjectMapper()
          .registerModule(new JavaTimeModule())
          .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
          .disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)
          .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

  private final MockMvc mvc;

  private volatile Cookie defaultAuthCookie;

  public record OnboardBusinessRecord(
      Business business, BusinessOwner businessOwner, BusinessProspect businessProspect) {}

  public void init() {
    TypedId<BusinessId> businessId = businessIds.get(0);
    if (businessRepository.findById(businessId).isEmpty()) {
      createBusiness(businessId);
    } else {
      log.debug("Default businessID {} already exists, not creating.", businessId);
    }
    if (businessBankAccountRepository.findBusinessBankAccountsByBusinessId(businessId).isEmpty()) {
      createBusinessBankAccount(businessId);
    } else {
      log.debug("Business bank account already exists for default business. Not creating");
    }
  }

  /** @return the first unused BusinessId from {@link #businessIds} */
  private TypedId<BusinessId> getNextBusinessId() {
    Set<TypedId<BusinessId>> used =
        businessRepository.findAll().stream().map(Business::getId).collect(Collectors.toSet());
    Optional<TypedId<BusinessId>> optionalNewId =
        businessIds.stream().filter(a -> !used.contains(a)).findFirst();

    // If the default list of IDs is not enough for the test, make another.
    if (optionalNewId.isPresent()) {
      return optionalNewId.get();
    } else {
      TypedId<BusinessId> newId = new TypedId<>(UUID.randomUUID());
      businessIds.add(newId);
      return newId;
    }
  }

  public String generateFullName() {
    return faker.name().fullName();
  }

  public String generateFirstName() {
    return faker.name().firstName();
  }

  public String generateLastName() {
    return faker.name().lastName();
  }

  public String generateEmail() {
    return randomUUID() + "@clearspend.com";
  }

  public String generatePassword() {
    return randomUUID().toString();
  }

  public String generatePhone() {
    return PhoneUtil.randomPhoneNumber();
  }

  public String generateTaxIdentificationNumber() {
    return faker.number().digits(9);
  }

  public String generateEmployerIdentificationNumber() {
    return Integer.toString(faker.number().numberBetween(100_000_000, 999_999_999));
  }

  public String generateRoutingNumber() {
    return faker.number().digits(9);
  }

  public String generateAccountNumber() {
    return faker.number().digits(11);
  }

  public String generateAccountName() {
    return faker.funnyName().name();
  }

  public LocalDate generateDateOfBirth() {
    return new java.sql.Date(faker.date().birthday(18, 100).getTime()).toLocalDate();
  }

  public String generateBusinessName() {
    return faker.company().name();
  }

  public OnboardBusinessRecord onboardBusiness() throws Exception {
    // create business prospect including setting email (returns email OTP)
    BusinessProspect businessProspect = createBusinessProspect();

    // validate email OTP
    validateBusinessProspectIdentifier(IdentifierType.EMAIL, businessProspect.getId(), "123456");

    // set business phone (returns phone OTP)
    setBusinessProspectPhone(businessProspect.getId());

    // validate phone OTP
    validateBusinessProspectIdentifier(IdentifierType.PHONE, businessProspect.getId(), "123456");

    // set business owner password
    businessProspectService.setBusinessProspectPassword(
        businessProspect.getId(), PasswordUtil.generatePassword());

    // convert the prospect to a business
    ConvertBusinessProspectResponse convertBusinessProspectResponse =
        convertBusinessProspect(businessProspect.getId());

    return new OnboardBusinessRecord(
        businessService.retrieveBusiness(businessProspect.getBusinessId()),
        businessOwnerService.retrieveBusinessOwner(
            convertBusinessProspectResponse.getBusinessOwnerId()),
        businessProspect);
  }

  public void testBusinessProspectState(String email, BusinessProspectStatus status) {
    BusinessProspectRecord record =
        businessProspectService.createBusinessProspect(
            generateFirstName(), generateLastName(), email);
    assertThat(record.businessProspectStatus()).isEqualTo(status);
  }

  @SneakyThrows
  public Cookie login(String email, String password) {
    LoginRequest request = new LoginRequest(email, password);
    String body = objectMapper.writeValueAsString(request);

    MockHttpServletResponse response =
        mvc.perform(post("/authentication/login").contentType("application/json").content(body))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

    log.info("response: {}", response);
    defaultAuthCookie = response.getCookie(SecurityConfig.ACCESS_TOKEN_COOKIE_NAME);
    return defaultAuthCookie;
  }

  public BusinessProspect createBusinessProspect() throws Exception {
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

    return businessProspect;
  }

  public void validateBusinessProspectIdentifier(
      IdentifierType identifierType, TypedId<BusinessProspectId> businessProspectId, String otp)
      throws Exception {
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

  public void setBusinessProspectPhone(TypedId<BusinessProspectId> businessProspectId)
      throws Exception {
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
  }

  public ConvertBusinessProspectResponse convertBusinessProspect(
      TypedId<BusinessProspectId> businessProspectId) throws Exception {
    ConvertBusinessProspectRequest request =
        new ConvertBusinessProspectRequest(
            generateBusinessName(),
            BusinessType.C_CORP,
            generateEmployerIdentificationNumber(),
            generatePhone(),
            generateApiAddress());
    String body = objectMapper.writeValueAsString(request);

    MockHttpServletResponse response =
        mvc.perform(
                post(String.format("/business-prospects/%s/convert", businessProspectId))
                    .contentType("application/json")
                    .content(body)
                    .cookie(defaultAuthCookie))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

    return objectMapper.readValue(response.getContentAsString(), new TypeReference<>() {});
  }

  public Business retrieveBusiness() {
    return businessService.retrieveBusiness(businessIds.get(0));
  }

  public void deleteBusiness(TypedId<BusinessId> businessId) {
    businessRepository.deleteById(businessId);
  }

  public void deleteBusinessLimit(TypedId<BusinessId> businessId) {
    businessLimitRepository.deleteByBusinessId(businessId);
  }

  public void deleteSpendLimit(TypedId<BusinessId> businessId) {
    transactionLimitRepository.deleteByBusinessId(businessId);
  }

  public BusinessOwnerAndUserRecord createBusinessOwner(
      TypedId<BusinessId> businessId, String email, String password) {
    TypedId<BusinessOwnerId> businessOwnerId = new TypedId<>();
    UUID fusionAuthUserId =
        fusionAuthService.createBusinessOwner(businessId, businessOwnerId, email, password);
    return businessOwnerService.createBusinessOwner(
        businessOwnerId,
        businessId,
        generateFirstName(),
        generateLastName(),
        generateEntityAddress(),
        email,
        generatePhone(),
        fusionAuthUserId.toString());
  }

  public void deleteBusinessOwner(TypedId<BusinessOwnerId> businessOwnerId) {
    businessOwnerRepository.deleteById(businessOwnerId);
  }

  public void deleteUser(TypedId<UserId> userId) {
    userRepository.deleteById(userId);
  }

  @Transactional
  public void deleteAccount(TypedId<BusinessId> businessId) {
    accountRepository.deleteByBusinessId(businessId);
  }

  public String getLinkToken(TypedId<BusinessId> businessId) throws Exception {
    MockHttpServletResponse response =
        mvc.perform(
                get("/business-bank-accounts/link-token")
                    .cookie(defaultAuthCookie)
                    .contentType("application/json"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

    return objectMapper
        .readValue(response.getContentAsString(), new TypeReference<LinkTokenResponse>() {})
        .linkToken();
  }

  public TypedId<BusinessBankAccountId> createBusinessBankAccount(TypedId<BusinessId> businessId) {
    try {
      String linkToken = plaidClient.createLinkToken(businessId);
      List<BusinessBankAccount> accounts =
          businessBankAccountService.linkBusinessBankAccounts(linkToken, businessId);
      return accounts.get(0).getId();
    } catch (IOException e) {
      log.info("Exception initializing with plaid", e);
      throw new RuntimeException(e);
    }
  }

  public BusinessBankAccount retrieveBusinessBankAccount() {
    List<BusinessBankAccount> businessBankAccounts =
        businessBankAccountRepository.findBusinessBankAccountsByBusinessId(businessIds.get(0));
    assertThat(businessBankAccounts).isNotEmpty();

    return businessBankAccounts.get(0);
  }

  public AdjustmentAndHoldRecord transactBankAccount(
      BankAccountTransactType bankAccountTransactType, BigDecimal amount, boolean placeHold) {
    TypedId<BusinessId> businessId = businessIds.get(0);
    BusinessBankAccount businessBankAccount = retrieveBusinessBankAccount();
    Account businessAccount = allocationService.getRootAllocation(businessId).account();
    return businessBankAccountService.transactBankAccount(
        businessId,
        businessBankAccount.getId(),
        bankAccountTransactType,
        new Amount(businessAccount.getLedgerBalance().getCurrency(), amount),
        placeHold);
  }

  public AllocationRecord createAllocation(
      TypedId<BusinessId> businessId,
      String name,
      TypedId<AllocationId> parentAllocationId,
      User user) {
    return allocationService.createAllocation(
        businessId,
        parentAllocationId,
        name,
        user,
        Amount.of(Currency.USD),
        DEFAULT_TRANSACTION_LIMITS,
        Collections.emptyList(),
        Collections.emptySet());
  }

  public record CreateBusinessRecord(
      Business business,
      BusinessOwner businessOwner,
      User user,
      String email,
      AllocationRecord allocationRecord,
      Cookie authCookie) {}

  public CreateBusinessRecord createBusiness() {
    return createBusiness(getNextBusinessId());
  }

  @SneakyThrows
  public CreateBusinessRecord createBusiness(TypedId<BusinessId> businessId) {
    String email = generateEmail();
    String password = generatePassword();
    String legalName = faker.company().name() + " " + UUID.randomUUID(); // more unique names
    Business business =
        businessService.createBusiness(
            businessId,
            legalName.length() > 100 ? legalName.substring(0, 100) : legalName,
            BusinessType.LLC,
            generateEntityAddress(),
            generateEmployerIdentificationNumber(),
            faker.internet().emailAddress(),
            faker.phoneNumber().phoneNumber(),
            Currency.USD);
    BusinessOwnerAndUserRecord businessOwner =
        createBusinessOwner(business.getId(), email, password);

    AllocationRecord rootAllocation =
        allocationService.createRootAllocation(
            business.getId(), businessOwner.user(), business.getLegalName() + " - root");

    log.debug("Created business {} with owner and root allocation.", businessId);

    return new CreateBusinessRecord(
        business,
        businessOwner.businessOwner(),
        businessOwner.user(),
        email,
        rootAllocation,
        login(email, password));
  }

  @Transactional
  public void deleteAllocation(TypedId<BusinessId> businessId) {
    allocationRepository.deleteByBusinessId(businessId);
  }

  public CreateUpdateUserRecord createUser(Business business) {
    return userService.createUser(
        business.getId(),
        UserType.EMPLOYEE,
        faker.name().firstName(),
        faker.name().lastName(),
        generateEntityAddress(),
        faker.internet().emailAddress(),
        faker.phoneNumber().phoneNumber());
  }

  public Card issueCard(
      Business business,
      Allocation allocation,
      User user,
      Currency currency,
      FundingType fundingType,
      CardType cardType) {
    return cardService
        .issueCard(
            BinType.DEBIT,
            fundingType,
            cardType,
            business.getId(),
            allocation.getId(),
            user.getId(),
            currency,
            true,
            business.getLegalName(),
            Map.of(Currency.USD, new HashMap<>()),
            Collections.emptyList(),
            Collections.emptySet(),
            business.getClearAddress().toAddress())
        .card();
  }

  public Address generateEntityAddress() {
    return new Address(
        new EncryptedString(faker.address().streetAddress()),
        new EncryptedString(faker.address().secondaryAddress()),
        faker.address().city(),
        faker.address().state(),
        new EncryptedString(faker.address().zipCode()),
        Country.USA);
  }

  public com.clearspend.capital.controller.type.Address generateApiAddress() {
    return new com.clearspend.capital.controller.type.Address(
        faker.address().streetAddress(),
        faker.address().secondaryAddress(),
        faker.address().city(),
        faker.address().state(),
        faker.address().zipCode(),
        Country.USA);
  }
}
