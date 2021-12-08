package com.tranwall.capital;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.javafaker.Faker;
import com.tranwall.capital.common.data.model.Address;
import com.tranwall.capital.common.data.model.Amount;
import com.tranwall.capital.common.typedid.data.AllocationId;
import com.tranwall.capital.common.typedid.data.BinId;
import com.tranwall.capital.common.typedid.data.BusinessBankAccountId;
import com.tranwall.capital.common.typedid.data.BusinessId;
import com.tranwall.capital.common.typedid.data.BusinessOwnerId;
import com.tranwall.capital.common.typedid.data.BusinessProspectId;
import com.tranwall.capital.common.typedid.data.ProgramId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.common.typedid.data.UserId;
import com.tranwall.capital.configuration.SecurityConfig;
import com.tranwall.capital.controller.BusinessBankAccountController.LinkTokenResponse;
import com.tranwall.capital.controller.type.business.prospect.BusinessProspectStatus;
import com.tranwall.capital.controller.type.business.prospect.ConvertBusinessProspectRequest;
import com.tranwall.capital.controller.type.business.prospect.ConvertBusinessProspectResponse;
import com.tranwall.capital.controller.type.business.prospect.CreateBusinessProspectRequest;
import com.tranwall.capital.controller.type.business.prospect.CreateBusinessProspectResponse;
import com.tranwall.capital.controller.type.business.prospect.SetBusinessProspectPhoneRequest;
import com.tranwall.capital.controller.type.business.prospect.ValidateBusinessProspectIdentifierRequest;
import com.tranwall.capital.controller.type.business.prospect.ValidateBusinessProspectIdentifierRequest.IdentifierType;
import com.tranwall.capital.controller.type.user.LoginRequest;
import com.tranwall.capital.crypto.PasswordUtil;
import com.tranwall.capital.crypto.data.model.embedded.EncryptedString;
import com.tranwall.capital.data.model.Account;
import com.tranwall.capital.data.model.Allocation;
import com.tranwall.capital.data.model.Bin;
import com.tranwall.capital.data.model.Business;
import com.tranwall.capital.data.model.BusinessBankAccount;
import com.tranwall.capital.data.model.BusinessOwner;
import com.tranwall.capital.data.model.BusinessProspect;
import com.tranwall.capital.data.model.Card;
import com.tranwall.capital.data.model.Program;
import com.tranwall.capital.data.model.User;
import com.tranwall.capital.data.model.enums.BankAccountTransactType;
import com.tranwall.capital.data.model.enums.BusinessType;
import com.tranwall.capital.data.model.enums.CardType;
import com.tranwall.capital.data.model.enums.Country;
import com.tranwall.capital.data.model.enums.Currency;
import com.tranwall.capital.data.model.enums.FundingType;
import com.tranwall.capital.data.model.enums.UserType;
import com.tranwall.capital.data.repository.AccountRepository;
import com.tranwall.capital.data.repository.AllocationRepository;
import com.tranwall.capital.data.repository.BusinessBankAccountRepository;
import com.tranwall.capital.data.repository.BusinessLimitRepository;
import com.tranwall.capital.data.repository.BusinessOwnerRepository;
import com.tranwall.capital.data.repository.BusinessProspectRepository;
import com.tranwall.capital.data.repository.BusinessRepository;
import com.tranwall.capital.data.repository.TransactionLimitRepository;
import com.tranwall.capital.data.repository.UserRepository;
import com.tranwall.capital.service.AccountService;
import com.tranwall.capital.service.AccountService.AdjustmentAndHoldRecord;
import com.tranwall.capital.service.AllocationService;
import com.tranwall.capital.service.AllocationService.AllocationRecord;
import com.tranwall.capital.service.BinService;
import com.tranwall.capital.service.BusinessBankAccountService;
import com.tranwall.capital.service.BusinessOwnerService;
import com.tranwall.capital.service.BusinessOwnerService.BusinessOwnerAndUserRecord;
import com.tranwall.capital.service.BusinessProspectService;
import com.tranwall.capital.service.BusinessProspectService.BusinessProspectRecord;
import com.tranwall.capital.service.BusinessService;
import com.tranwall.capital.service.CardService;
import com.tranwall.capital.service.FusionAuthService;
import com.tranwall.capital.service.ProgramService;
import com.tranwall.capital.service.UserService;
import com.tranwall.capital.service.UserService.CreateUpdateUserRecord;
import com.tranwall.capital.util.PhoneUtil;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
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

  public static final TypedId<BinId> binId = new TypedId<>("2691dad4-82f7-47ec-9cae-0686a22572fc");
  public static final TypedId<ProgramId> pooledProgramId =
      new TypedId<>("6faf3838-b2d7-422c-8d6f-c2294ebc73b4");
  public static final TypedId<ProgramId> individualProgramId =
      new TypedId<>("033955d1-f18e-497e-9905-88ba71e90208");
  public static final TypedId<BusinessId> businessId =
      new TypedId<>("82a79d15-9e47-421b-ab8f-78532f4f8bc7");

  private final AccountRepository accountRepository;
  private final AllocationRepository allocationRepository;
  private final BusinessBankAccountRepository businessBankAccountRepository;
  private final BusinessLimitRepository businessLimitRepository;
  private final BusinessOwnerRepository businessOwnerRepository;
  private final BusinessProspectRepository businessProspectRepository;
  private final BusinessRepository businessRepository;
  private final TransactionLimitRepository transactionLimitRepository;
  private final UserRepository userRepository;

  private final AccountService accountService;
  private final AllocationService allocationService;
  private final BinService binService;
  private final BusinessBankAccountService businessBankAccountService;
  private final BusinessOwnerService businessOwnerService;
  private final BusinessProspectService businessProspectService;
  private final BusinessService businessService;
  private final CardService cardService;
  private final FusionAuthService fusionAuthService;
  private final ProgramService programService;
  private final UserService userService;

  private final Faker faker = new Faker(new SecureRandom());

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
    if (businessRepository.findById(businessId).isEmpty()) {
      createBusiness(businessId);
      createBusinessBankAccount();
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
    return randomUUID() + "@tranwall.com";
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

  public Bin retrieveBin() {
    return binService.retrieveBin(binId);
  }

  public Bin createBin() {
    return binService.createBin(faker.random().nextInt(500000, 699999).toString(), "Unit test BIN");
  }

  public Program retrievePooledProgram() {
    return programService.retrieveProgram(pooledProgramId);
  }

  public Program retrieveIndividualProgram() {
    return programService.retrieveProgram(individualProgramId);
  }

  public Program createProgram(Bin bin) {
    return programService.createProgram(
        UUID.randomUUID().toString(),
        bin.getBin(),
        FundingType.POOLED,
        CardType.VIRTUAL,
        faker.number().digits(8));
  }

  public Business retrieveBusiness() {
    return businessService.retrieveBusiness(businessId);
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

  public TypedId<BusinessBankAccountId> createBusinessBankAccount() {
    return createBusinessBankAccount(businessId);
  }

  public TypedId<BusinessBankAccountId> createBusinessBankAccount(TypedId<BusinessId> businessId) {
    return businessBankAccountService
        .createBusinessBankAccount(
            generateRoutingNumber(),
            generateAccountNumber(),
            generateAccountName(),
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString(),
            businessId)
        .getId();
  }

  public BusinessBankAccount retrieveBusinessBankAccount() {
    List<BusinessBankAccount> businessBankAccounts =
        businessBankAccountRepository.findBusinessBankAccountsByBusinessId(businessId);
    assertThat(businessBankAccounts).isNotEmpty();

    return businessBankAccounts.get(0);
  }

  public AdjustmentAndHoldRecord transactBankAccount(
      BankAccountTransactType bankAccountTransactType, BigDecimal amount, boolean placeHold) {
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
        businessId, parentAllocationId, name, user, Amount.of(Currency.USD));
  }

  public record CreateBusinessRecord(
      Program program,
      Business business,
      BusinessOwner businessOwner,
      User user,
      String email,
      AllocationRecord allocationRecord,
      Cookie authCookie) {}

  public CreateBusinessRecord createBusiness() {
    return createBusiness(null);
  }

  @SneakyThrows
  public CreateBusinessRecord createBusiness(TypedId<BusinessId> businessId) {
    String email = generateEmail();
    String password = generatePassword();
    Program program = retrievePooledProgram();
    Business business =
        businessService.createBusiness(
            businessId,
            faker.company().name(),
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

    return new CreateBusinessRecord(
        program,
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
      Business business, Allocation allocation, User user, Program program, Currency currency) {
    return cardService
        .issueCard(
            program,
            business.getId(),
            allocation.getId(),
            user.getId(),
            currency,
            true,
            business.getLegalName())
        .card();
  }

  public Address generateEntityAddress() {
    return new com.tranwall.capital.common.data.model.Address(
        new EncryptedString(faker.address().streetAddress()),
        new EncryptedString(faker.address().secondaryAddress()),
        faker.address().city(),
        faker.address().state(),
        new EncryptedString(faker.address().zipCode()),
        Country.USA);
  }

  public com.tranwall.capital.controller.type.Address generateApiAddress() {
    return new com.tranwall.capital.controller.type.Address(
        faker.address().streetAddress(),
        faker.address().secondaryAddress(),
        faker.address().city(),
        faker.address().state(),
        faker.address().zipCode(),
        Country.USA);
  }
}
