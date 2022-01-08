package com.clearspend.capital.controller.nonprod;

import com.clearspend.capital.common.data.model.Address;
import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.controller.nonprod.type.testdata.CreateTestDataResponse;
import com.clearspend.capital.controller.nonprod.type.testdata.CreateTestDataResponse.TestBusiness;
import com.clearspend.capital.controller.nonprod.type.testdata.GetBusinessesResponse;
import com.clearspend.capital.crypto.data.model.embedded.EncryptedString;
import com.clearspend.capital.data.model.Account;
import com.clearspend.capital.data.model.Allocation;
import com.clearspend.capital.data.model.Card;
import com.clearspend.capital.data.model.User;
import com.clearspend.capital.data.model.business.Business;
import com.clearspend.capital.data.model.business.BusinessBankAccount;
import com.clearspend.capital.data.model.business.BusinessOwner;
import com.clearspend.capital.data.model.enums.AllocationReallocationType;
import com.clearspend.capital.data.model.enums.BankAccountTransactType;
import com.clearspend.capital.data.model.enums.BusinessReallocationType;
import com.clearspend.capital.data.model.enums.BusinessType;
import com.clearspend.capital.data.model.enums.Country;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.data.model.enums.FundingType;
import com.clearspend.capital.data.model.enums.LimitPeriod;
import com.clearspend.capital.data.model.enums.LimitType;
import com.clearspend.capital.data.model.enums.UserType;
import com.clearspend.capital.data.model.enums.card.BinType;
import com.clearspend.capital.data.model.enums.card.CardType;
import com.clearspend.capital.data.model.enums.network.NetworkMessageType;
import com.clearspend.capital.data.model.network.StripeWebhookLog;
import com.clearspend.capital.data.repository.AccountActivityRepository;
import com.clearspend.capital.data.repository.AllocationRepository;
import com.clearspend.capital.data.repository.CardRepository;
import com.clearspend.capital.data.repository.UserRepository;
import com.clearspend.capital.data.repository.business.BusinessRepository;
import com.clearspend.capital.service.AllocationService;
import com.clearspend.capital.service.AllocationService.AllocationRecord;
import com.clearspend.capital.service.BusinessBankAccountService;
import com.clearspend.capital.service.BusinessOwnerService;
import com.clearspend.capital.service.BusinessOwnerService.BusinessOwnerAndUserRecord;
import com.clearspend.capital.service.BusinessService;
import com.clearspend.capital.service.CardService;
import com.clearspend.capital.service.CardService.CardRecord;
import com.clearspend.capital.service.NetworkMessageService;
import com.clearspend.capital.service.UserService;
import com.clearspend.capital.service.UserService.CreateUpdateUserRecord;
import com.clearspend.capital.service.type.NetworkCommon;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.javafaker.Faker;
import com.stripe.model.issuing.Authorization;
import com.stripe.model.issuing.Authorization.MerchantData;
import com.stripe.model.issuing.Authorization.PendingRequest;
import com.stripe.model.issuing.Cardholder;
import com.stripe.model.issuing.Cardholder.Billing;
import com.stripe.model.issuing.Cardholder.Individual;
import com.stripe.model.issuing.Transaction;
import io.swagger.v3.oas.annotations.Parameter;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * This controller contains end points that are not deployed top production but allow the caller to
 * do things that they normally wouldn't be able to do.
 */
@Profile("!prod")
@RestController
@RequestMapping("/non-production/test-data")
@RequiredArgsConstructor
@Slf4j
public class TestDataController {

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
  private final BusinessService businessService;
  private final BusinessOwnerService businessOwnerService;
  private final CardService cardService;
  private final NetworkMessageService networkMessageService;
  private final UserService userService;

  private final AllocationRepository allocationRepository;
  private final AccountActivityRepository accountActivityRepository;
  private final BusinessRepository businessRepository;
  private final CardRepository cardRepository;
  private final UserRepository userRepository;

  private final Faker faker = new Faker();

  public record BusinessRecord(
      Business business, BusinessOwner businessOwner, User user, Allocation allocation) {}

  @GetMapping("/db-content")
  private CreateTestDataResponse getDbContent() {
    return getAllData(null);
  }

  @GetMapping("/business/{businessId}")
  private CreateTestDataResponse getBusiness(
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
  private GetBusinessesResponse getBusinesses() {
    return new GetBusinessesResponse(
        businessRepository.findAll().stream()
            .map(com.clearspend.capital.controller.type.business.Business::new)
            .collect(Collectors.toList()));
  }

  @GetMapping(value = "/create-all-demo", produces = MediaType.APPLICATION_JSON_VALUE)
  private CreateTestDataResponse createTestData() throws IOException {

    // create a new business
    BusinessRecord businessRecord = createBusiness(new TypedId<>());
    Business business = businessRecord.business();
    createUser(business);

    // create bankAccount, deposit $10,000 and withdraw $267.34
    BusinessBankAccount businessBankAccount =
        businessBankAccountService.createBusinessBankAccount(
            faker.number().digits(9),
            faker.number().digits(11),
            faker.funnyName().name(),
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString(),
            business.getId());
    businessBankAccountService.transactBankAccount(
        business.getId(),
        businessBankAccount.getId(),
        BankAccountTransactType.DEPOSIT,
        Amount.of(business.getCurrency(), BigDecimal.valueOf(10000)),
        false);
    businessBankAccountService.transactBankAccount(
        business.getId(),
        businessBankAccount.getId(),
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
            Collections.emptyList(),
            Collections.emptySet());
    businessService.reallocateBusinessFunds(
        business.getId(),
        childAllocation.allocation().getId(),
        childAllocation.allocation().getAccountId(),
        BusinessReallocationType.BUSINESS_TO_ALLOCATION,
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
            Collections.emptyList(),
            Collections.emptySet());
    businessService.reallocateBusinessFunds(
        business.getId(),
        grandchildAllocation.allocation().getId(),
        grandchildAllocation.allocation().getAccountId(),
        BusinessReallocationType.BUSINESS_TO_ALLOCATION,
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
            Collections.emptyList(),
            Collections.emptySet(),
            business.getClearAddress().toAddress());
    cards.add(cardRecord.card());
    allocationService.reallocateAllocationFunds(
        business,
        parentAllocation.getId(),
        parentAllocation.getAccountId(),
        cardRecord.card().getId(),
        AllocationReallocationType.ALLOCATION_TO_CARD,
        Amount.of(business.getCurrency(), BigDecimal.valueOf(173.45)));

    Amount amount = Amount.of(Currency.USD, BigDecimal.TEN);
    networkMessageService.processNetworkMessage(
        generateNetworkCommon(
            NetworkMessageType.AUTH_REQUEST,
            user.user(),
            cardRecord.card(),
            cardRecord.account(),
            amount));

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
            Collections.emptyList(),
            Collections.emptySet(),
            business.getClearAddress().toAddress());
    cards.add(cardRecord.card());
    allocationService.reallocateAllocationFunds(
        business,
        parentAllocation.getId(),
        parentAllocation.getAccountId(),
        cardRecord.card().getId(),
        AllocationReallocationType.ALLOCATION_TO_CARD,
        Amount.of(business.getCurrency(), BigDecimal.valueOf(91.17)));

    amount = Amount.of(Currency.USD, BigDecimal.valueOf(26.27));
    networkMessageService.processNetworkMessage(
        generateNetworkCommon(
            NetworkMessageType.AUTH_REQUEST,
            user.user(),
            cardRecord.card(),
            cardRecord.account(),
            amount));

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
                Collections.emptyList(),
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
                Collections.emptyList(),
                Collections.emptySet(),
                business.getClearAddress().toAddress())
            .card());

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
                Collections.emptyList(),
                Collections.emptySet(),
                business.getClearAddress().toAddress())
            .card());

    return new CreateTestDataResponse(
        List.of(
            new TestBusiness(
                business,
                Collections.emptyList(),
                List.of(
                    businessRecord.allocation(),
                    childAllocation.allocation(),
                    grandchildAllocation.allocation()),
                cards,
                users)));
  }

  private BusinessRecord createBusiness(TypedId<BusinessId> businessId) {
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

    BusinessOwnerAndUserRecord businessOwnerRecord =
        businessOwnerService.createBusinessOwner(
            new TypedId<>(),
            businessId,
            faker.name().firstName(),
            faker.name().lastName(),
            generateEntityAddress(),
            faker.internet().emailAddress(),
            faker.phoneNumber().phoneNumber(),
            null,
            false,
            null);

    AllocationRecord allocationRecord =
        allocationService.createRootAllocation(
            businessId, businessOwnerRecord.user(), business.getLegalName() + " - root");

    return new BusinessRecord(
        business,
        businessOwnerRecord.businessOwner(),
        businessOwnerRecord.user(),
        allocationRecord.allocation());
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

  public CreateUpdateUserRecord createUser(Business business) throws IOException {
    return userService.createUser(
        business.getId(),
        UserType.EMPLOYEE,
        faker.name().firstName(),
        faker.name().lastName(),
        generateEntityAddress(),
        faker.internet().emailAddress(),
        faker.phoneNumber().phoneNumber());
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
                        userService.retrieveUsersForBusiness(business.getId()),
                        allocations.stream()
                            .filter(
                                allocation -> allocation.getBusinessId().equals(business.getId()))
                            .collect(Collectors.toList()),
                        cards.stream()
                            .filter(card -> card.getBusinessId().equals(business.getId()))
                            .collect(Collectors.toList()),
                        users.stream()
                            .filter(user -> user.getBusinessId().equals(business.getId()))
                            .map(u -> new CreateUpdateUserRecord(u, null))
                            .collect(Collectors.toList())))
            .collect(Collectors.toList()));
  }

  public static NetworkCommon generateNetworkCommon(
      NetworkMessageType networkMessageType, User user, Card card, Account account, Amount amount) {
    Faker faker = Faker.instance();

    Cardholder cardholder = new Cardholder();
    if (card.getShippingAddress() != null) {
      Billing billing = new Billing();
      billing.setAddress(card.getShippingAddress().toStripeAddress());
      cardholder.setBilling(billing);
    }
    cardholder.setCreated(user.getCreated().toEpochSecond());
    cardholder.setEmail(user.getEmail().getEncrypted());
    cardholder.setId("stripe_" + user.getId().toString());
    Individual individual = new Individual();
    individual.setFirstName(user.getFirstName().getEncrypted());
    individual.setLastName(user.getLastName().getEncrypted());
    cardholder.setIndividual(individual);
    cardholder.setName(card.getCardLine3());
    if (user.getPhone() != null) {
      cardholder.setPhoneNumber(user.getPhone().getEncrypted());
    }
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

    return switch (networkMessageType) {
      case AUTH_REQUEST -> {
        PendingRequest pendingRequest = new PendingRequest();
        pendingRequest.setAmount(amount.toStripeAmount());
        pendingRequest.setCurrency(amount.getCurrency().name());
        pendingRequest.setIsAmountControllable(false);
        pendingRequest.setMerchantAmount(amount.toStripeAmount());
        pendingRequest.setMerchantCurrency(amount.getCurrency().name());

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

        yield new NetworkCommon(
            NetworkMessageType.AUTH_REQUEST, stripeAuthorization, new StripeWebhookLog());
      }
      case TRANSACTION_CREATED -> {
        Transaction stripeTransaction = new Transaction();
        stripeTransaction.setAmount(amount.toStripeAmount());
        stripeTransaction.setCard(stripeCard.getId());
        stripeTransaction.setCardholder(cardholder.getId());
        stripeTransaction.setCreated(System.currentTimeMillis());
        stripeTransaction.setCurrency(amount.getCurrency().name());
        stripeTransaction.setId("stripe_" + UUID.randomUUID());
        stripeTransaction.setMerchantAmount(amount.toStripeAmount());
        stripeTransaction.setMerchantData(merchantData);

        yield new NetworkCommon(stripeTransaction, new StripeWebhookLog());
      }
      default -> throw new IllegalStateException("Unexpected value: " + networkMessageType);
    };
  }
}
