package com.tranwall.capital.controller.nonprod;

import com.github.javafaker.Faker;
import com.tranwall.capital.client.i2c.push.controller.type.CardAcceptor;
import com.tranwall.capital.client.i2c.push.controller.type.CardStatus;
import com.tranwall.capital.client.i2c.push.controller.type.I2cCard;
import com.tranwall.capital.client.i2c.push.controller.type.I2cTransaction;
import com.tranwall.capital.common.data.model.Address;
import com.tranwall.capital.common.data.model.Amount;
import com.tranwall.capital.common.typedid.data.AllocationId;
import com.tranwall.capital.common.typedid.data.BusinessId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.controller.nonprod.type.testdata.CreateTestDataResponse;
import com.tranwall.capital.controller.nonprod.type.testdata.CreateTestDataResponse.TestBusiness;
import com.tranwall.capital.controller.nonprod.type.testdata.GetBusinessesResponse;
import com.tranwall.capital.crypto.data.model.embedded.EncryptedString;
import com.tranwall.capital.data.model.Account;
import com.tranwall.capital.data.model.Allocation;
import com.tranwall.capital.data.model.Bin;
import com.tranwall.capital.data.model.Business;
import com.tranwall.capital.data.model.BusinessBankAccount;
import com.tranwall.capital.data.model.BusinessOwner;
import com.tranwall.capital.data.model.Card;
import com.tranwall.capital.data.model.Program;
import com.tranwall.capital.data.model.User;
import com.tranwall.capital.data.model.enums.AllocationReallocationType;
import com.tranwall.capital.data.model.enums.BankAccountTransactType;
import com.tranwall.capital.data.model.enums.BusinessReallocationType;
import com.tranwall.capital.data.model.enums.BusinessType;
import com.tranwall.capital.data.model.enums.CardType;
import com.tranwall.capital.data.model.enums.Country;
import com.tranwall.capital.data.model.enums.Currency;
import com.tranwall.capital.data.model.enums.FundingType;
import com.tranwall.capital.data.model.enums.NetworkMessageDeviceType;
import com.tranwall.capital.data.model.enums.NetworkMessageTransactionType;
import com.tranwall.capital.data.model.enums.NetworkMessageType;
import com.tranwall.capital.data.model.enums.UserType;
import com.tranwall.capital.data.repository.AllocationRepository;
import com.tranwall.capital.data.repository.BusinessRepository;
import com.tranwall.capital.data.repository.CardRepository;
import com.tranwall.capital.data.repository.UserRepository;
import com.tranwall.capital.service.AllocationService;
import com.tranwall.capital.service.AllocationService.AllocationRecord;
import com.tranwall.capital.service.BinService;
import com.tranwall.capital.service.BusinessBankAccountService;
import com.tranwall.capital.service.BusinessOwnerService;
import com.tranwall.capital.service.BusinessOwnerService.BusinessOwnerAndUserRecord;
import com.tranwall.capital.service.BusinessService;
import com.tranwall.capital.service.CardService;
import com.tranwall.capital.service.CardService.CardRecord;
import com.tranwall.capital.service.NetworkMessageService;
import com.tranwall.capital.service.ProgramService;
import com.tranwall.capital.service.UserService;
import com.tranwall.capital.service.UserService.CreateUpdateUserRecord;
import com.tranwall.capital.service.type.NetworkCommon;
import io.swagger.v3.oas.annotations.Parameter;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
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

  private final AllocationService allocationService;
  private final BinService binService;
  private final BusinessBankAccountService businessBankAccountService;
  private final BusinessService businessService;
  private final BusinessOwnerService businessOwnerService;
  private final CardService cardService;
  private final NetworkMessageService networkMessageService;
  private final ProgramService programService;
  private final UserService userService;

  private final AllocationRepository allocationRepository;
  private final BusinessRepository businessRepository;
  private final CardRepository cardRepository;
  private final UserRepository userRepository;

  private final Faker faker = new Faker();

  private Program individualVirtualProgram;
  private Program pooledVirtualProgram;
  private Program individualPlasticProgram;
  private Program pooledPlasticProgram;
  private List<Bin> allBins;
  private List<Program> allPrograms;

  public record BusinessRecord(
      Business business, BusinessOwner businessOwner, User user, Allocation allocation) {}

  @PostConstruct
  void init() {
    allBins = binService.findAllBins();
    Bin bin =
        allBins.size() > 0
            ? allBins.get(0)
            : binService.createBin(faker.random().nextInt(500000, 599999) + "", "Test Data BIN");

    allPrograms = programService.findAllPrograms();
    individualVirtualProgram =
        allPrograms.stream()
            .filter(
                p ->
                    p.getFundingType() == FundingType.INDIVIDUAL
                        && p.getCardType() == CardType.VIRTUAL)
            .findFirst()
            .orElseGet(() -> createProgram(bin, FundingType.INDIVIDUAL, CardType.VIRTUAL));

    pooledVirtualProgram =
        allPrograms.stream()
            .filter(
                p ->
                    p.getFundingType() == FundingType.POOLED && p.getCardType() == CardType.VIRTUAL)
            .findFirst()
            .orElseGet(() -> createProgram(bin, FundingType.POOLED, CardType.VIRTUAL));

    individualPlasticProgram =
        allPrograms.stream()
            .filter(
                p ->
                    p.getFundingType() == FundingType.INDIVIDUAL
                        && p.getCardType() == CardType.PLASTIC)
            .findFirst()
            .orElseGet(() -> createProgram(bin, FundingType.INDIVIDUAL, CardType.PLASTIC));

    pooledPlasticProgram =
        allPrograms.stream()
            .filter(
                p ->
                    p.getFundingType() == FundingType.POOLED && p.getCardType() == CardType.PLASTIC)
            .findFirst()
            .orElseGet(() -> createProgram(bin, FundingType.POOLED, CardType.PLASTIC));
  }

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
            .map(com.tranwall.capital.controller.type.business.Business::new)
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
            Amount.of(Currency.USD));
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
            Amount.of(Currency.USD));
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
            individualVirtualProgram,
            business.getId(),
            parentAllocation.getId(),
            user.user().getId(),
            business.getCurrency(),
            true,
            business.getLegalName());
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
            NetworkMessageType.PRE_AUTH_TRANSACTION,
            user.user(),
            cardRecord.card(),
            cardRecord.account(),
            individualVirtualProgram,
            amount));

    cardRecord =
        cardService.issueCard(
            individualPlasticProgram,
            business.getId(),
            parentAllocation.getId(),
            user.user().getId(),
            business.getCurrency(),
            true,
            business.getLegalName());
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
            NetworkMessageType.FINANCIAL_TRANSACTION,
            user.user(),
            cardRecord.card(),
            cardRecord.account(),
            individualPlasticProgram,
            amount));

    CreateUpdateUserRecord user2 = createUser(business);
    users.add(user2);
    cards.add(
        cardService
            .issueCard(
                pooledVirtualProgram,
                business.getId(),
                childAllocation.allocation().getId(),
                user.user().getId(),
                business.getCurrency(),
                true,
                business.getLegalName())
            .card());
    cards.add(
        cardService
            .issueCard(
                pooledPlasticProgram,
                business.getId(),
                childAllocation.allocation().getId(),
                user.user().getId(),
                business.getCurrency(),
                true,
                business.getLegalName())
            .card());

    CreateUpdateUserRecord user3 = createUser(business);
    users.add(user3);
    cards.add(
        cardService
            .issueCard(
                individualVirtualProgram,
                business.getId(),
                grandchildAllocation.allocation().getId(),
                user.user().getId(),
                business.getCurrency(),
                true,
                business.getLegalName())
            .card());

    return new CreateTestDataResponse(
        allBins,
        allPrograms,
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

  private Program createProgram(Bin bin, FundingType fundingType, CardType cardType) {
    return programService.createProgram(
        UUID.randomUUID().toString(),
        bin.getBin(),
        fundingType,
        cardType,
        faker.number().digits(8));
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
    return new com.tranwall.capital.common.data.model.Address(
        new EncryptedString(faker.address().streetAddress()),
        new EncryptedString(faker.address().secondaryAddress()),
        faker.address().city(),
        faker.address().state(),
        new EncryptedString(faker.address().zipCode()),
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
        businessId, parentAllocationId, name, user, Amount.of(Currency.USD));
  }

  public CreateUpdateUserRecord createUser(com.tranwall.capital.data.model.Business business)
      throws IOException {
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
        binService.findAllBins(),
        programService.findAllPrograms(),
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
      NetworkMessageType networkMessageType,
      User user,
      Card card,
      Account account,
      Program program,
      Amount amount) {
    Faker faker = Faker.instance();

    // individual fields below so it's easier to understand what's being set in Transaction and Card
    String notificationEventRef = faker.number().digits(40);
    String transactionRef = faker.number().digits(11);
    String messageType = networkMessageType.getMti();
    LocalDate date = LocalDate.now();
    OffsetTime time = OffsetTime.now();
    String transactionType = NetworkMessageTransactionType.PURCHASE_TRANS.getI2cTransactionType();
    String service = RandomStringUtils.randomAlphanumeric(120);
    String requestedAmount = amount.getAmount().toString();
    String requestedAmountCurrency = amount.getCurrency().name();
    String transactionAmount = amount.getAmount().toString();
    String transactionCurrency = amount.getCurrency().name();
    String transactionResponseCode = "Type: AN 2";
    String interchangeFee = amount.getAmount().abs().multiply(BigDecimal.valueOf(0.02)).toString();
    String panEntryMode = "021"; // 02 (Magnetic stripe) + 1 (Terminal can accept PINs)
    String authorizationCode = RandomStringUtils.randomAlphanumeric(16);
    String acquirerReferenceNumber = RandomStringUtils.randomAlphanumeric(20);
    String retrievalReferenceNumber = RandomStringUtils.randomAlphanumeric(80);
    String systemTraceAuditNumber = RandomStringUtils.randomAlphanumeric(24);
    String networkRef = "Type: AN 25";
    String originalTransactionRef = ""; // ""Type: N 11";
    String transferRef = ""; // ""Type: N 30";
    String bankAccountNumber = RandomStringUtils.randomAlphanumeric(31);
    String transactionDescription = RandomStringUtils.randomAlphanumeric(255);
    String externalTransReference = RandomStringUtils.randomAlphanumeric(40);
    String externalUserReference = RandomStringUtils.randomAlphanumeric(40);
    String externalLinkedCardRefRef = "Type: AN 11";
    String externalLinkedCardProfileSet1 = RandomStringUtils.randomAlphanumeric(255);
    String externalLinkedCardProfileSet2 = RandomStringUtils.randomAlphanumeric(255);
    String panSequenceNumber = "001"; // ""Type: AN 3";
    String fraudParameter = RandomStringUtils.randomAlphanumeric(20);

    String acquirerRef = RandomStringUtils.randomAlphanumeric(44);
    String merchantCode = RandomStringUtils.randomAlphanumeric(15);
    String merchantNameAndLocation = "Merchant Name            Tucson85641  AZUSA";
    String merchantLocality = "Tucson";
    String merchantRegion = "AZ";
    String merchantPostalCode = "85641";
    Integer mcc = 6060;
    String deviceRef = RandomStringUtils.randomAlphanumeric(8);
    String deviceType = NetworkMessageDeviceType.POS.getI2cDeviceType();
    OffsetDateTime localDateTime = OffsetDateTime.now();
    CardAcceptor cardAcceptor =
        new CardAcceptor(
            acquirerRef,
            merchantCode,
            merchantNameAndLocation,
            merchantLocality,
            merchantRegion,
            merchantPostalCode,
            mcc,
            deviceRef,
            deviceType,
            localDateTime);

    I2cTransaction i2cTransaction =
        new I2cTransaction(
            notificationEventRef,
            transactionRef,
            messageType,
            date,
            time,
            cardAcceptor,
            transactionType,
            service,
            requestedAmount,
            requestedAmountCurrency,
            transactionAmount,
            transactionCurrency,
            transactionResponseCode,
            interchangeFee,
            panEntryMode,
            authorizationCode,
            acquirerReferenceNumber,
            retrievalReferenceNumber,
            systemTraceAuditNumber,
            networkRef,
            originalTransactionRef,
            transferRef,
            bankAccountNumber,
            transactionDescription,
            externalTransReference,
            externalUserReference,
            externalLinkedCardRefRef,
            externalLinkedCardProfileSet1,
            externalLinkedCardProfileSet2,
            panSequenceNumber,
            fraudParameter);

    String cardNumber = card.getCardNumber().getEncrypted();
    String cardProgramRef = program.getI2cCardProgramRef();
    String cardReferenceRef = card.getI2cCardRef();
    String primaryCardNumber = card.getCardNumber().getEncrypted();
    String primaryCardReferenceRef = card.getI2cCardRef();
    // TODO(kuchlein): @Slava, do we have this somewhere? Ditto for memberRef
    String customerRef = RandomStringUtils.randomAlphanumeric(20);
    String memberRef = RandomStringUtils.randomAlphanumeric(25);
    String availableBalance =
        account.getAvailableBalance() != null
            ? account.getAvailableBalance().getAmount().toString()
            : "";
    String ledgerBalance = account.getLedgerBalance().getAmount().toString();
    String cardStatus = CardStatus.OPEN.getI2cCardStatus();
    String firstName = user.getFirstName().getEncrypted();
    String lastName = user.getLastName().getEncrypted();
    String addressLine1 = "";
    String addressLine2 = "";
    String locality = "";
    String region = "";
    String postalCode = "";
    String country = "";
    final Address address = user.getAddress();
    if (address != null) {
      addressLine1 =
          address.getStreetLine1() != null ? address.getStreetLine1().getEncrypted() : "";
      addressLine2 =
          address.getStreetLine2() != null ? address.getStreetLine2().getEncrypted() : "";
      locality = address.getLocality() != null ? address.getLocality() : "";
      region = address.getRegion() != null ? address.getRegion() : "";
      postalCode = address.getPostalCode() != null ? address.getPostalCode().getEncrypted() : "";
      country = address.getCountry() != null ? address.getCountry().name() : "";
    }
    String phone = user.getPhone().getEncrypted();
    String email = user.getEmail().getEncrypted();
    String sourceCardReferenceNumber = ""; // ""Type: AN 40";
    String sourceCardNumber = ""; // ""Type: N 19";

    I2cCard i2cCard =
        new I2cCard(
            cardNumber,
            null,
            cardProgramRef,
            cardReferenceRef,
            primaryCardNumber,
            primaryCardReferenceRef,
            customerRef,
            memberRef,
            availableBalance,
            ledgerBalance,
            cardStatus,
            firstName,
            lastName,
            addressLine1,
            addressLine2,
            locality,
            region,
            postalCode,
            country,
            phone,
            email,
            sourceCardReferenceNumber,
            sourceCardNumber);

    return new NetworkCommon(i2cTransaction, i2cCard);
  }
}
