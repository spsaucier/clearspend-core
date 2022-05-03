package com.clearspend.capital;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.clearspend.capital.client.plaid.PlaidClient;
import com.clearspend.capital.client.stripe.StripeClient;
import com.clearspend.capital.common.data.model.Address;
import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.common.typedid.data.business.BusinessOwnerId;
import com.clearspend.capital.common.typedid.data.business.BusinessProspectId;
import com.clearspend.capital.configuration.SecurityConfig;
import com.clearspend.capital.controller.business.BusinessBankAccountController.LinkTokenResponse;
import com.clearspend.capital.controller.nonprod.TestDataController;
import com.clearspend.capital.controller.nonprod.TestDataController.NetworkCommonAuthorization;
import com.clearspend.capital.controller.type.allocation.CreateAllocationRequest;
import com.clearspend.capital.controller.type.allocation.CreateAllocationResponse;
import com.clearspend.capital.controller.type.business.prospect.BusinessProspectStatus;
import com.clearspend.capital.controller.type.business.prospect.ConvertBusinessProspectRequest;
import com.clearspend.capital.controller.type.business.prospect.ConvertBusinessProspectResponse;
import com.clearspend.capital.controller.type.business.prospect.CreateBusinessProspectResponse;
import com.clearspend.capital.controller.type.business.prospect.CreateOrUpdateBusinessProspectRequest;
import com.clearspend.capital.controller.type.business.prospect.SetBusinessProspectPhoneRequest;
import com.clearspend.capital.controller.type.business.prospect.ValidateBusinessProspectIdentifierRequest;
import com.clearspend.capital.controller.type.business.prospect.ValidateBusinessProspectIdentifierRequest.IdentifierType;
import com.clearspend.capital.controller.type.card.limits.CurrencyLimit;
import com.clearspend.capital.controller.type.user.LoginRequest;
import com.clearspend.capital.crypto.PasswordUtil;
import com.clearspend.capital.crypto.data.model.embedded.EncryptedString;
import com.clearspend.capital.crypto.utils.CurrentUserSwitcher;
import com.clearspend.capital.crypto.utils.CurrentUserSwitcher.SwitchesCurrentUser;
import com.clearspend.capital.data.model.Account;
import com.clearspend.capital.data.model.Allocation;
import com.clearspend.capital.data.model.Card;
import com.clearspend.capital.data.model.ChartOfAccountsMapping;
import com.clearspend.capital.data.model.ExpenseCategory;
import com.clearspend.capital.data.model.User;
import com.clearspend.capital.data.model.business.Business;
import com.clearspend.capital.data.model.business.BusinessBankAccount;
import com.clearspend.capital.data.model.business.BusinessOwner;
import com.clearspend.capital.data.model.business.BusinessProspect;
import com.clearspend.capital.data.model.business.TosAcceptance;
import com.clearspend.capital.data.model.enums.AccountingSetupStep;
import com.clearspend.capital.data.model.enums.AuthorizationMethod;
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
import com.clearspend.capital.data.model.enums.card.CardStatusReason;
import com.clearspend.capital.data.model.enums.card.CardType;
import com.clearspend.capital.data.model.security.DefaultRoles;
import com.clearspend.capital.data.repository.AccountRepository;
import com.clearspend.capital.data.repository.AllocationRepository;
import com.clearspend.capital.data.repository.ChartOfAccountsMappingRepository;
import com.clearspend.capital.data.repository.ExpenseCategoryRepository;
import com.clearspend.capital.data.repository.TransactionLimitRepository;
import com.clearspend.capital.data.repository.UserRepository;
import com.clearspend.capital.data.repository.business.BusinessBankAccountBalanceRepository;
import com.clearspend.capital.data.repository.business.BusinessBankAccountRepository;
import com.clearspend.capital.data.repository.business.BusinessLimitRepository;
import com.clearspend.capital.data.repository.business.BusinessOwnerRepository;
import com.clearspend.capital.data.repository.business.BusinessProspectRepository;
import com.clearspend.capital.data.repository.business.BusinessRepository;
import com.clearspend.capital.service.AccountService;
import com.clearspend.capital.service.AccountService.AdjustmentAndHoldRecord;
import com.clearspend.capital.service.AllocationService;
import com.clearspend.capital.service.AllocationService.AllocationRecord;
import com.clearspend.capital.service.AllocationService.CreatesRootAllocation;
import com.clearspend.capital.service.BusinessBankAccountService;
import com.clearspend.capital.service.BusinessOwnerService;
import com.clearspend.capital.service.BusinessOwnerService.BusinessOwnerAndUserRecord;
import com.clearspend.capital.service.BusinessProspectService;
import com.clearspend.capital.service.BusinessProspectService.BusinessProspectRecord;
import com.clearspend.capital.service.BusinessService;
import com.clearspend.capital.service.CardService;
import com.clearspend.capital.service.FusionAuthService;
import com.clearspend.capital.service.FusionAuthService.FusionAuthUserAccessor;
import com.clearspend.capital.service.FusionAuthService.FusionAuthUserCreator;
import com.clearspend.capital.service.FusionAuthService.RoleChange;
import com.clearspend.capital.service.NetworkMessageService;
import com.clearspend.capital.service.RolesAndPermissionsService;
import com.clearspend.capital.service.ServiceHelper;
import com.clearspend.capital.service.UserService;
import com.clearspend.capital.service.UserService.CreateUpdateUserRecord;
import com.clearspend.capital.service.type.BusinessOwnerData;
import com.clearspend.capital.service.type.ConvertBusinessProspect;
import com.clearspend.capital.service.type.CurrentUser;
import com.clearspend.capital.service.type.NetworkCommon;
import com.clearspend.capital.util.PhoneUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.javafaker.Faker;
import com.stripe.model.issuing.Authorization;
import com.stripe.model.issuing.Authorization.AmountDetails;
import com.stripe.model.issuing.Authorization.MerchantData;
import com.stripe.model.issuing.Authorization.PendingRequest;
import com.stripe.model.issuing.Authorization.VerificationData;
import com.stripe.model.issuing.Card.Shipping;
import com.stripe.model.issuing.Card.SpendingControls;
import com.stripe.model.issuing.Card.SpendingControls.SpendingLimit;
import com.stripe.model.issuing.Card.Wallets;
import com.stripe.model.issuing.Card.Wallets.ApplePay;
import com.stripe.model.issuing.Card.Wallets.GooglePay;
import com.stripe.model.issuing.Cardholder;
import com.stripe.model.issuing.Cardholder.Billing;
import com.stripe.model.issuing.Cardholder.Requirements;
import java.io.IOException;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.persistence.EntityManager;
import javax.servlet.http.Cookie;
import javax.validation.constraints.NotNull;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.function.ThrowingRunnable;
import org.junit.jupiter.api.function.ThrowingSupplier;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SuppressWarnings("JavaTimeDefaultTimeZone")
@Transactional
@Component
@RequiredArgsConstructor
@Slf4j
public class TestHelper {

  private static final Map<Currency, Map<LimitType, Map<LimitPeriod, BigDecimal>>>
      DEFAULT_TRANSACTION_LIMITS = Map.of(Currency.USD, new HashMap<>());

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
  private final ChartOfAccountsMappingRepository mappingRepository;
  private final ServiceHelper serviceHelper;
  private final BusinessBankAccountBalanceRepository businessBankAccountBalanceRepo;
  private final ExpenseCategoryRepository expenseCategoryRepository;

  private final AccountService accountService;
  private final AllocationService allocationService;
  private final BusinessBankAccountService businessBankAccountService;
  private final BusinessOwnerService businessOwnerService;
  private final BusinessProspectService businessProspectService;
  private final BusinessService businessService;
  private final CardService cardService;
  private final FusionAuthService fusionAuthService;
  private final NetworkMessageService networkMessageService;
  private final RolesAndPermissionsService rolesAndPermissionsService;
  private final UserService userService;

  private final EntityManager entityManager;
  private final PlaidClient plaidClient;
  private final StripeClient stripeClient;

  private final Faker faker = new Faker(new SecureRandom(new byte[] {0}));
  private final Map<TypedId<?>, String> passwords = new HashMap<>();

  public final ObjectMapper objectMapper =
      new ObjectMapper()
          .registerModule(new JavaTimeModule())
          .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
          .disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)
          .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

  private final MockMvc mvc;

  private volatile Cookie defaultAuthCookie;

  public void setIssuedPhysicalCardsLimit(TypedId<BusinessId> businessId, int newLimit) {
    com.clearspend.capital.data.model.business.BusinessLimit limit =
        businessLimitRepository.findByBusinessId(businessId).orElseThrow();
    limit.setIssuedPhysicalCardsLimit(newLimit);
    businessLimitRepository.save(limit);
  }

  public record OnboardBusinessRecord(
      Business business,
      BusinessOwner businessOwner,
      BusinessProspect businessProspect,
      Cookie cookie) {}

  private final Random random = new Random(0);

  /**
   * Creates the default business if it doesn't already exist, creates a businessBankAccount if it
   * doesn't exist.
   *
   * @return a CreateBusinessRecord representing the business
   */
  public CreateBusinessRecord init() {
    TypedId<BusinessId> businessId = businessIds.get(0);
    CreateBusinessRecord createBusinessRecord = null;
    if (businessRepository.findById(businessId).isEmpty()) {
      createBusinessRecord = createBusiness(businessId);
      setCurrentUser(createBusinessRecord.user());
    } else {
      log.debug("Default businessID {} already exists, not creating.", businessId);
    }
    if (businessBankAccountRepository.findByBusinessId(businessId).isEmpty()) {
      createBusinessBankAccount(businessId);
    } else {
      log.debug("Business bank account already exists for default business. Not creating");
    }

    if (createBusinessRecord == null) {
      Business business = businessRepository.getById(businessId);
      BusinessOwner businessOwner = businessOwnerRepository.findByBusinessId(businessId).get(0);
      User user =
          userService.retrieveUsersForBusiness(businessId).stream()
              .filter(
                  u ->
                      u.getType().equals(UserType.BUSINESS_OWNER)
                          && passwords.containsKey(u.getId()))
              .findFirst()
              .orElse(
                  createBusinessOwner(business.getId(), generateEmail(), generatePassword(), false)
                      .user());
      entityManager.flush();
      setCurrentUser(user);
      createBusinessRecord =
          new CreateBusinessRecord(
              business,
              businessOwner,
              user,
              user.getEmail().getEncrypted(),
              serviceHelper.allocationService().getRootAllocation(businessId),
              login(user));
    }

    return createBusinessRecord;
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
    return generatePassword(32);
  }

  public String generatePassword(int length) {
    return RandomStringUtils.randomAscii(length);
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
    return faker.date().birthday(18, 100).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
  }

  public String generateBusinessName() {
    return faker.company().name() + faker.number().digits(6);
  }

  public String generateAllocationName() {
    return "Alloc-" + faker.number().digits(8);
  }

  public Faker getFaker() {
    return faker;
  }

  public OnboardBusinessRecord onboardBusiness() throws Exception {

    BusinessProspect businessProspect = createProspect().businessProspect;

    // convert the prospect to a business
    ConvertBusinessProspectResponse convertBusinessProspectResponse =
        convertBusinessProspect(businessProspect.getId());

    return new OnboardBusinessRecord(
        serviceHelper.businessService().getBusiness(businessProspect.getBusinessId()).business(),
        serviceHelper
            .businessOwnerService()
            .retrieveBusinessOwner(convertBusinessProspectResponse.getBusinessOwnerId()),
        businessProspect,
        defaultAuthCookie);
  }

  public OnboardBusinessRecord createProspect() throws Exception {
    // create business prospect including setting email (returns email OTP)
    BusinessProspect businessProspect = createBusinessProspect();

    // validate email OTP
    validateBusinessProspectIdentifier(IdentifierType.EMAIL, businessProspect.getId(), "123456");

    // set business phone (returns phone OTP)
    setBusinessProspectPhone(businessProspect.getId());

    // validate phone OTP
    validateBusinessProspectIdentifier(IdentifierType.PHONE, businessProspect.getId(), "123456");

    // set business owner password
    String password = PasswordUtil.generatePassword();
    businessProspectService.setBusinessProspectPassword(businessProspect.getId(), password, true);
    passwords.put(businessProspect.getId(), password);

    login(businessProspect.getEmail().getEncrypted(), password);

    return new OnboardBusinessRecord(null, null, businessProspect, defaultAuthCookie);
  }

  public void testBusinessProspectState(String email, BusinessProspectStatus status) {
    BusinessProspectRecord record =
        businessProspectService.createOrUpdateBusinessProspect(
            generateFirstName(),
            generateLastName(),
            BusinessType.MULTI_MEMBER_LLC,
            true,
            false,
            false,
            false,
            email,
            faker.internet().ipV4Address(),
            faker.internet().userAgentAny(),
            true);
    assertThat(record.businessProspectStatus()).isEqualTo(status);
  }

  /**
   * Sets a user in the SecurityContext with every possible global role. Useful for extremely
   * elevated permission actions to support tests.
   */
  @FusionAuthUserAccessor(
      reviewer = "Craig Miller",
      explanation = "to create master user for testing purposes")
  public void setUserAsMaster(@NonNull User user) {
    CurrentUserSwitcher.setCurrentUser(user, DefaultRoles.ALL_GLOBAL);
  }

  @FusionAuthUserAccessor(
      reviewer = "Craig Miller",
      explanation = "to create master user for testing purposes")
  public void setCurrentUserAsWebhook(@NonNull final User user) {
    CurrentUserSwitcher.setCurrentUser(user, Set.of(DefaultRoles.GLOBAL_APPLICATION_WEBHOOK));
  }

  public CreateUpdateUserRecord createUserWithGlobalRole(
      final Business business, final String role) {
    final CreateUpdateUserRecord user = createUser(business);
    // Bypassing RolesAndPermissionsService because this can't pass the permission check
    fusionAuthService.changeUserRole(RoleChange.GRANT, user.user().getSubjectRef(), role);
    return user;
  }

  /**
   * Set the current user for direct service testing, including global roles, if any.
   *
   * @param user the user who will be taking subsequent
   */
  @SwitchesCurrentUser(reviewer = "jscarbor", explanation = "For testing")
  @FusionAuthUserAccessor(
      reviewer = "jscarbor",
      explanation = "to look up roles that would normally come on the JWT")
  public void setCurrentUser(@NonNull User user) {
    CurrentUserSwitcher.setCurrentUser(
        user, fusionAuthService.getUserRoles(UUID.fromString(user.getSubjectRef())));
  }

  @SwitchesCurrentUser(reviewer = "Craig Miller", explanation = "For testing")
  public void setCurrentUser(final CurrentUser currentUser) {
    Optional.ofNullable(currentUser)
        .ifPresentOrElse(
            theCurrentUser -> CurrentUserSwitcher.setCurrentUser(theCurrentUser),
            () -> CurrentUserSwitcher.clearCurrentUser());
  }

  /**
   * If there is test logic that should be run with a particular user, and then that user should be
   * removed prior to any other code being executed, this helper is intended to support this.
   *
   * @param user the User to set before running the logic.
   * @param action a wrapper around the logic to be run with the User set.
   */
  @SneakyThrows
  public void runWithCurrentUser(@NonNull final User user, final ThrowingRunnable action) {
    final CurrentUser oldCurrentUser = CurrentUser.get();
    setCurrentUser(user);
    try {
      action.run();
    } finally {
      setCurrentUser(oldCurrentUser);
    }
  }

  @SneakyThrows
  public <T> T runWithCurrentUser(@NonNull final User user, final ThrowingSupplier<T> action) {
    final CurrentUser oldCurrentUser = CurrentUser.get();
    setCurrentUser(user);
    try {
      return action.get();
    } finally {
      setCurrentUser(oldCurrentUser);
    }
  }

  @SneakyThrows
  public void runWithWebhookUser(@NonNull final User user, final ThrowingRunnable action) {
    final CurrentUser oldCurrentUser = CurrentUser.get();
    setCurrentUserAsWebhook(user);
    try {
      action.run();
    } finally {
      setCurrentUser(oldCurrentUser);
    }
  }

  @SneakyThrows
  public <T> T runWithWebhookUser(@NonNull final User user, final ThrowingSupplier<T> action) {
    final CurrentUser oldCurrentUser = CurrentUser.get();
    setCurrentUserAsWebhook(user);
    try {
      return action.get();
    } finally {
      setCurrentUser(oldCurrentUser);
    }
  }

  @SwitchesCurrentUser(reviewer = "Craig Miller", explanation = "For testing")
  public void clearCurrentUser() {
    CurrentUserSwitcher.clearCurrentUser();
  }

  /**
   * Get a fresh cookie for the given user to perform subsequent operations through {@link #mvc}.
   * Uses the cached password from when the test user was created with this class's facilities.
   *
   * <p>The cookie remains available at {@link #getDefaultAuthCookie()}.
   *
   * <p>For tests accessing services directly, see {@link CurrentUserSwitcher}
   *
   * @param userId whose cookie to retrieve
   * @return cookie for authorizing this user
   */
  public Cookie login(@NonNull TypedId<UserId> userId) {
    return login(userService.retrieveUser(userId));
  }

  /**
   * Get a fresh cookie for the given user to perform subsequent operations through {@link #mvc}.
   * Uses the cached password from when the test user was created with this class's facilities.
   *
   * <p>The cookie remains available at {@link #getDefaultAuthCookie()}.
   *
   * <p>For tests accessing services directly, see {@link CurrentUserSwitcher}.
   *
   * @param user whose cookie to retrieve
   * @return cookie for authorizing this user
   */
  public Cookie login(@NonNull User user) {
    return login(user.getEmail().getEncrypted(), getPassword(user));
  }

  public String getPassword(User user) {
    return passwords.get(user.getId());
  }

  /**
   * Get a fresh cookie for the given user to perform subsequent operations through {@link #mvc}.
   * Uses the cached password from when the test user was created with this class's facilities.
   *
   * <p>The cookie remains available at {@link #getDefaultAuthCookie()}.
   *
   * <p>For tests accessing services directly, see {@link CurrentUserSwitcher}.
   *
   * @param email of the user whose cookie to retrieve
   * @param password the user's password
   * @return cookie for authorizing this user
   */
  @SneakyThrows
  public @NonNull Cookie login(@NonNull String email, @NonNull String password) {
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
    CreateOrUpdateBusinessProspectRequest request =
        new CreateOrUpdateBusinessProspectRequest(
            generateEmail(),
            generateFirstName(),
            generateLastName(),
            BusinessType.MULTI_MEMBER_LLC,
            true,
            false,
            true);
    String body = objectMapper.writeValueAsString(request);

    MockHttpServletResponse response =
        mvc.perform(
                post("/business-prospects")
                    .header(HttpHeaders.USER_AGENT, faker.internet().userAgentAny())
                    .contentType("application/json")
                    .content(body))
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
      String businessLegalName, TypedId<BusinessProspectId> businessProspectId) throws Exception {
    ConvertBusinessProspectRequest request =
        new ConvertBusinessProspectRequest(
            businessLegalName,
            generateEmployerIdentificationNumber(),
            generatePhone(),
            generateApiAddress(),
            "7311",
            "Business default description.");
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

  public ConvertBusinessProspectResponse convertBusinessProspect(
      TypedId<BusinessProspectId> businessProspectId) throws Exception {
    return convertBusinessProspect(generateBusinessName(), businessProspectId);
  }

  public Business retrieveBusiness() {
    return serviceHelper.businessService().getBusiness(businessIds.get(0)).business();
  }

  public void deleteBusinessBankAccount(final TypedId<BusinessId> businessId) {
    businessBankAccountBalanceRepo.deleteAllByBusinessId(businessId);
    businessBankAccountRepository.deleteAllByBusinessId(businessId);
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

  @FusionAuthUserCreator(
      reviewer = "jscarbor",
      explanation = "for mimicking the onboarding process")
  public BusinessOwnerAndUserRecord createBusinessOwner(
      TypedId<BusinessId> businessId, String email, String password, Boolean representative) {
    TypedId<BusinessOwnerId> businessOwnerId = new TypedId<>();
    UUID fusionAuthUserId =
        fusionAuthService.createBusinessOwner(businessId, businessOwnerId, email, password);
    passwords.put(businessOwnerId, password);
    return serviceHelper
        .businessProspectService()
        .createMainBusinessOwnerAndRepresentative(
            new BusinessOwnerData(
                businessOwnerId,
                businessId,
                generateFirstName(),
                generateLastName(),
                null,
                null,
                true,
                representative,
                false,
                false,
                null,
                null,
                generateEntityAddress(),
                email,
                generatePhone(),
                fusionAuthUserId.toString(),
                null),
            new TosAcceptance(
                OffsetDateTime.now(ZoneOffset.UTC),
                faker.internet().ipV4Address(),
                faker.internet().userAgentAny()));
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

  public BusinessBankAccount createBusinessBankAccount(TypedId<BusinessId> businessId) {
    try {
      String linkToken = plaidClient.createLinkToken(businessId);
      List<BusinessBankAccount> accounts =
          businessBankAccountService.linkBusinessBankAccounts(linkToken, businessId);
      return accounts.get(0);
    } catch (IOException e) {
      log.info("Exception initializing with plaid", e);
      throw new RuntimeException(e);
    }
  }

  public BusinessBankAccount retrieveBusinessBankAccount() {
    List<BusinessBankAccount> businessBankAccounts =
        businessBankAccountRepository.findByBusinessId(businessIds.get(0));
    assertThat(businessBankAccounts).isNotEmpty();

    return businessBankAccounts.get(0);
  }

  public AdjustmentAndHoldRecord transactBankAccount(
      BusinessBankAccount businessBankAccount,
      BankAccountTransactType bankAccountTransactType,
      User user,
      BigDecimal amount,
      boolean standardHold) {
    Account businessAccount =
        serviceHelper
            .allocationService()
            .getRootAllocation(businessBankAccount.getBusinessId())
            .account();
    return businessBankAccountService.transactBankAccount(
        businessBankAccount.getBusinessId(),
        businessBankAccount.getId(),
        user.getId(),
        bankAccountTransactType,
        new Amount(businessAccount.getLedgerBalance().getCurrency(), amount),
        standardHold);
  }

  public AllocationRecord createAllocation(
      TypedId<BusinessId> businessId, String name, TypedId<AllocationId> parentAllocationId) {
    entityManager.flush();
    final AllocationRecord allocationRecord =
        allocationService.createAllocation(
            businessId,
            parentAllocationId,
            name,
            Amount.of(Currency.USD),
            DEFAULT_TRANSACTION_LIMITS,
            Collections.emptySet(),
            Collections.emptySet(),
            false);
    entityManager.flush();
    return allocationRecord;
  }

  @SneakyThrows
  public @NonNull @NotNull(message = "allocationId required") TypedId<AllocationId>
      createAllocationMvc(
          TypedId<UserId> actor, String allocationName, TypedId<AllocationId> parentAllocationId) {
    CreateAllocationRequest request =
        new CreateAllocationRequest(
            allocationName,
            parentAllocationId,
            new com.clearspend.capital.controller.type.Amount(Currency.USD, BigDecimal.ZERO),
            Collections.singletonList(new CurrencyLimit(Currency.USD, new HashMap<>())),
            Collections.emptySet(),
            Collections.emptySet(),
            false);

    String body = objectMapper.writeValueAsString(request);

    Cookie authCookie = login(actor);

    MockHttpServletResponse response =
        mvc.perform(
                post("/allocations")
                    .contentType("application/json")
                    .content(body)
                    .cookie(authCookie))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

    CreateAllocationResponse createAllocationResponse =
        objectMapper.readValue(response.getContentAsString(), new TypeReference<>() {});
    return createAllocationResponse.getAllocationId();
  }

  public record CreateBusinessRecord(
      Business business,
      BusinessOwner businessOwner,
      User user,
      String email,
      AllocationRecord allocationRecord,
      Cookie authCookie) {}

  public CreateBusinessRecord createBusiness() {
    return createBusiness(getNextBusinessId(), 0L);
  }

  public CreateBusinessRecord createBusiness(Long openingBalance) {
    return createBusiness(getNextBusinessId(), openingBalance);
  }

  public CreateBusinessRecord createBusiness(TypedId<BusinessId> businessId) {
    return createBusiness(businessId, 0L);
  }

  @CreatesRootAllocation(
      reviewer = "jscarbor",
      explanation =
          "For testing, mimics the onboarding process - but could this call convertBusinessProspect?")
  @SneakyThrows
  public CreateBusinessRecord createBusiness(TypedId<BusinessId> businessId, Long openingBalance) {
    String email = generateEmail();
    String password = generatePassword();
    String legalName = faker.company().name() + " " + UUID.randomUUID(); // more unique names
    Business business =
        serviceHelper
            .businessService()
            .createBusiness(
                businessId,
                BusinessType.SINGLE_MEMBER_LLC,
                email,
                new ConvertBusinessProspect(
                    null,
                    legalName.length() > 100 ? legalName.substring(0, 100) : legalName,
                    legalName.length() > 100 ? legalName.substring(0, 100) : legalName,
                    generateEmployerIdentificationNumber(),
                    faker.phoneNumber().phoneNumber(),
                    generateEntityAddress(),
                    MerchantType.AGRICULTURAL_COOPERATIVE,
                    MerchantType.AGRICULTURAL_COOPERATIVE.getDescription(),
                    faker.internet().url()),
                new TosAcceptance(
                    OffsetDateTime.now().truncatedTo(ChronoUnit.MICROS),
                    faker.internet().ipV4Address(),
                    faker.internet().userAgentAny()))
            .business();
    BusinessOwnerAndUserRecord businessOwner =
        createBusinessOwner(business.getId(), email, password, true);
    passwords.put(businessOwner.user().getId(), password);
    setCurrentUser(businessOwner.user());
    AllocationRecord rootAllocation =
        allocationService.createRootAllocation(
            business.getId(), businessOwner.user(), business.getLegalName());

    log.debug("Created business {} with owner and root allocation.", businessId);

    setCurrentUser(businessOwner.user());

    if (openingBalance != null && openingBalance != 0L) {
      serviceHelper
          .accountService()
          .depositFunds(
              business.getId(),
              rootAllocation.account(),
              Amount.of(Currency.USD, new BigDecimal(openingBalance)),
              false);
    }

    business
        .getStripeData()
        .setFinancialAccountRef(
            stripeClient
                .createFinancialAccount(business.getId(), business.getStripeData().getAccountRef())
                .getId());

    business.setCodatCompanyRef("codat_company_ref");
    business.setCodatConnectionId("codat-connection-id");
    business.setCodatCreditCardId("codat-card-id");
    business.setAccountingSetupStep(AccountingSetupStep.AWAITING_SYNC);

    business = businessRepository.save(business);

    final CreateBusinessRecord createBusinessRecord =
        new CreateBusinessRecord(
            business,
            businessOwner.businessOwner(),
            businessOwner.user(),
            email,
            rootAllocation,
            login(email, password));
    entityManager.flush();
    return createBusinessRecord;
  }

  public Cookie getDefaultAuthCookie() {
    return defaultAuthCookie;
  }

  @Transactional
  public void deleteAllocation(TypedId<BusinessId> businessId) {
    allocationRepository.deleteByBusinessId(businessId);
  }

  public CreateUpdateUserRecord createUser(Business business) {
    CreateUpdateUserRecord record =
        userService.createUserAndFusionAuthRecord(
            business.getId(),
            UserType.EMPLOYEE,
            faker.name().firstName(),
            faker.name().lastName(),
            generateEntityAddress(),
            faker.internet().emailAddress(),
            faker.phoneNumber().phoneNumber());
    passwords.put(record.user().getId(), record.password());
    return record;
  }

  public CreateUpdateUserRecord createUserWithRole(Allocation allocation, String role) {
    CreateUpdateUserRecord result =
        createUser(
            serviceHelper.businessService().getBusiness(allocation.getBusinessId()).business());
    rolesAndPermissionsService.createOrUpdateUserAllocationRole(
        result.user().getId(), allocation.getId(), role);
    return result;
  }

  public Card issueCard(
      Business business,
      Allocation allocation,
      User user,
      Currency currency,
      FundingType fundingType,
      CardType cardType,
      boolean activateCard) {
    Card card =
        cardService
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
                Collections.emptySet(),
                Collections.emptySet(),
                false,
                business.getClearAddress().toAddress())
            .card();
    if (activateCard) {
      setCurrentUser(user);
      card = cardService.activateMyCard(card, CardStatusReason.NONE);
    }
    entityManager.flush();

    entityManager.flush();
    return card;
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

  @NotNull
  public Authorization getAuthorization(
      Business business,
      User user,
      Card card,
      MerchantType merchantType,
      long authorizationAmount,
      AuthorizationMethod authorizationMethod,
      long pendingAmount,
      String stripeId) {
    Authorization authorization = new Authorization();
    authorization.setId(stripeId);
    authorization.setLivemode(false);
    authorization.setAmount(authorizationAmount);
    AmountDetails amountDetails = new AmountDetails();
    amountDetails.setAtmFee(null);
    authorization.setAmountDetails(amountDetails);
    authorization.setApproved(false);
    authorization.setAuthorizationMethod("online");
    authorization.setBalanceTransactions(new ArrayList<>());
    authorization.setCard(getStripeCard(business, user, card));
    authorization.setCardholder(user.getExternalRef());
    authorization.setCreated(OffsetDateTime.now().toEpochSecond());
    authorization.setCurrency(business.getCurrency().toStripeCurrency());
    authorization.setMerchantAmount(0L);
    authorization.setMerchantCurrency(business.getCurrency().toStripeCurrency());
    MerchantData merchantData = getMerchantData(merchantType);
    authorization.setMerchantData(merchantData);
    authorization.setAuthorizationMethod(authorizationMethod.name().toLowerCase());
    authorization.setMetadata(new HashMap<>());
    authorization.setObject("issuing.authorization");
    if (pendingAmount != 0) {
      PendingRequest pendingRequest = new PendingRequest();
      pendingRequest.setAmount(pendingAmount);
      AmountDetails pendingRequestAmountDetails = new AmountDetails();
      pendingRequestAmountDetails.setAtmFee(null);
      pendingRequest.setAmountDetails(pendingRequestAmountDetails);
      pendingRequest.setCurrency(business.getCurrency().toStripeCurrency());
      pendingRequest.setIsAmountControllable(
          merchantType == MerchantType.AUTOMATED_FUEL_DISPENSERS);
      pendingRequest.setMerchantAmount(pendingAmount);
      pendingRequest.setMerchantCurrency(business.getCurrency().toStripeCurrency());
      authorization.setPendingRequest(pendingRequest);
    }
    authorization.setRequestHistory(new ArrayList<>());
    authorization.setStatus("pending");
    authorization.setTransactions(new ArrayList<>());
    VerificationData verificationData = new VerificationData();
    verificationData.setAddressLine1Check("not_provided");
    verificationData.setAddressPostalCodeCheck("not_provided");
    verificationData.setCvcCheck("not_provided");
    verificationData.setExpiryCheck("match");
    authorization.setWallet(null);
    return authorization;
  }

  @org.jetbrains.annotations.NotNull
  public MerchantData getMerchantData(MerchantType merchantType) {
    MerchantData merchantData = new MerchantData();
    merchantData.setCategory(merchantType.getStripeMerchantType());
    merchantData.setCategoryCode(String.valueOf(merchantType.getMcc()));
    merchantData.setCity("San Francisco");
    merchantData.setCountry("US");
    merchantData.setName("Tim's Balance");
    merchantData.setNetworkId("1234567890");
    merchantData.setPostalCode("94103");
    merchantData.setState("CA");
    return merchantData;
  }

  public void createUserAllocationCardAndNetworkTransaction(
      CreateBusinessRecord createBusinessRecord,
      Business business,
      Account sourceAccount,
      int transactions) {
    setCurrentUser(createBusinessRecord.user());
    Allocation allocation = createBusinessRecord.allocationRecord.allocation();
    Account account = createBusinessRecord.allocationRecord.account();
    CreateUpdateUserRecord user = createUserWithRole(allocation, DefaultRoles.ALLOCATION_EMPLOYEE);
    int maxAmount = 99;
    if (sourceAccount != null) {
      AllocationRecord allocationRecord =
          createAllocation(
              business.getId(),
              generateAccountName(),
              createBusinessRecord.allocationRecord().allocation().getId());
      serviceHelper
          .accountService()
          .reallocateFunds(
              sourceAccount.getId(),
              allocationRecord.account().getId(),
              new Amount(Currency.USD, BigDecimal.valueOf((long) maxAmount * transactions)));
      allocation = allocationRecord.allocation();
      account = allocationRecord.account();
    }
    setCurrentUser(createBusinessRecord.user());
    Card card =
        issueCard(
            business,
            allocation,
            user.user(),
            Currency.USD,
            FundingType.POOLED,
            CardType.PHYSICAL,
            true);
    SecureRandom random = new SecureRandom();
    for (int i = 0; i < transactions; i++) {
      Amount amount =
          Amount.of(Currency.USD, new BigDecimal(random.nextInt(maxAmount)).add(BigDecimal.ONE));
      createNetworkTransaction(business, account, user.user(), card, amount);
    }
  }

  public void createNetworkTransaction(
      Business business, Account account, User user, Card card, Amount amount) {
    NetworkCommonAuthorization networkCommonAuthorization =
        TestDataController.generateAuthorizationNetworkCommon(user, card, account, amount);
    runWithWebhookUser(
        user,
        () -> {
          networkMessageService.processNetworkMessage(networkCommonAuthorization.networkCommon());
        });
    assertPost(networkCommonAuthorization.networkCommon(), false, false, true);

    NetworkCommon common =
        TestDataController.generateCaptureNetworkCommon(
            business, networkCommonAuthorization.authorization());
    runWithWebhookUser(
        user,
        () -> {
          networkMessageService.processNetworkMessage(common);
        });
    assertPost(common, true, false, false);
  }

  public void assertPost(
      NetworkCommon networkCommon, boolean postAdjustment, boolean postDecline, boolean postHold) {
    assertThat(networkCommon.isPostAdjustment()).isEqualTo(postAdjustment);
    assertThat(networkCommon.isPostDecline()).isEqualTo(postDecline);
    assertThat(networkCommon.isPostHold()).isEqualTo(postHold);
  }

  private com.stripe.model.issuing.Card getStripeCard(Business business, User user, Card card) {
    log.info("business: {}", business);
    log.info("user: {}", user);
    log.info("card: {}", card);
    com.stripe.model.issuing.Card out = new com.stripe.model.issuing.Card();
    out.setId(card.getExternalRef());
    out.setLivemode(false);
    out.setBrand("Visa");
    out.setCancellationReason(null);
    out.setCardholder(getStripeCardholder(business, user));
    out.setCreated(card.getCreated().toEpochSecond());
    out.setCurrency(business.getCurrency().toStripeCurrency());
    //    String cvc;
    out.setExpMonth((long) card.getExpirationDate().getMonthValue());
    out.setExpYear((long) card.getExpirationDate().getYear());
    out.setLast4(card.getLastFour());
    out.setMetadata(new HashMap<>());
    //    String number;
    out.setObject("issuing.card");
    out.setReplacedBy(null);
    out.setReplacementFor(null);
    out.setReplacementReason(null);
    if (card.getType() == CardType.PHYSICAL) {
      Shipping shipping = new Shipping();
      com.stripe.model.Address address = new com.stripe.model.Address();
      address.setLine1(card.getShippingAddress().getStreetLine1().getEncrypted());
      if (card.getShippingAddress().getStreetLine2() != null
          && StringUtils.isNotBlank(card.getShippingAddress().getStreetLine2().getEncrypted())) {
        address.setLine2(card.getShippingAddress().getStreetLine2().getEncrypted());
      }
      address.setCity(card.getShippingAddress().getLocality());
      address.setState(card.getShippingAddress().getRegion());
      address.setPostalCode(card.getShippingAddress().getPostalCode().getEncrypted());
      address.setCountry(card.getShippingAddress().getCountry().getTwoCharacterCode());
      shipping.setAddress(address);
      out.setShipping(shipping);
    }
    SpendingControls spendingControls = new SpendingControls();
    spendingControls.setAllowedCategories(null);
    spendingControls.setBlockedCategories(null);
    List<SpendingLimit> spendingLimits = new ArrayList<>();
    SpendingLimit spendingLimit = new SpendingLimit();
    spendingLimit.setAmount(50000L);
    spendingLimit.setCategories(new ArrayList<>());
    spendingLimit.setInterval("daily");
    spendingLimits.add(spendingLimit);
    spendingControls.setSpendingLimits(spendingLimits);
    spendingControls.setSpendingLimitsCurrency(business.getCurrency().toStripeCurrency());
    out.setSpendingControls(spendingControls);
    out.setStatus("active");
    out.setType(card.getType().toStripeType());
    Wallets wallets = new Wallets();
    ApplePay applePay = new ApplePay();
    applePay.setEligible(true);
    applePay.setIneligibleReason(null);
    wallets.setApplePay(applePay);
    GooglePay googlePay = new GooglePay();
    googlePay.setEligible(true);
    googlePay.setIneligibleReason(null);
    wallets.setGooglePay(googlePay);
    wallets.setPrimaryAccountIdentifier(null);
    out.setWallets(wallets);

    return out;
  }

  private Cardholder getStripeCardholder(Business business, User user) {
    Cardholder out = new Cardholder();

    out.setId(user.getExternalRef());
    out.setLivemode(false);
    Billing billing = new Billing();
    com.stripe.model.Address address = new com.stripe.model.Address();
    address.setLine1(business.getClearAddress().getStreetLine1());
    if (StringUtils.isNotBlank(business.getClearAddress().getStreetLine2())) {
      address.setLine2(business.getClearAddress().getStreetLine2());
    }
    address.setCity(business.getClearAddress().getLocality());
    address.setState(business.getClearAddress().getRegion());
    address.setPostalCode(business.getClearAddress().getPostalCode());
    address.setCountry(business.getClearAddress().getCountry().getTwoCharacterCode());
    billing.setAddress(address);
    out.setBilling(billing);
    out.setCompany(null);
    out.setCreated(user.getCreated().toEpochSecond());
    out.setEmail(user.getEmail().getEncrypted());
    out.setIndividual(null);
    out.setMetadata(new HashMap<>());
    out.setName(user.getFirstName().getEncrypted() + " " + user.getLastName().getEncrypted());
    out.setObject("issuing.cardholder");
    out.setPhoneNumber(user.getPhone().getEncrypted());
    Requirements requirements = new Requirements();
    requirements.setDisabledReason(null);
    requirements.setPastDue(new ArrayList<>());
    out.setRequirements(requirements);
    Cardholder.SpendingControls spendingControls = new Cardholder.SpendingControls();
    spendingControls.setAllowedCategories(Collections.emptyList());
    spendingControls.setBlockedCategories(Collections.emptyList());
    spendingControls.setSpendingLimits(Collections.emptyList());
    spendingControls.setSpendingLimitsCurrency(null);
    out.setSpendingControls(spendingControls);
    out.setStatus("active");
    out.setType("individual");

    return out;
  }

  public void createCodatExpenseCategoryMappings(Business business) {
    List<ExpenseCategory> expenseCategories =
        expenseCategoryRepository.findByBusinessId(business.getId());
    ChartOfAccountsMapping mappingOne =
        new ChartOfAccountsMapping(business.getId(), expenseCategories.get(0).getId(), 1, "auto");

    ChartOfAccountsMapping mappingTwo =
        new ChartOfAccountsMapping(business.getId(), expenseCategories.get(1).getId(), 2, "fuel");

    mappingRepository.save(mappingOne);
    mappingRepository.save(mappingTwo);
  }
}
