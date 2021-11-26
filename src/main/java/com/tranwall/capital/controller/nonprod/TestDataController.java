package com.tranwall.capital.controller.nonprod;

import com.github.javafaker.Faker;
import com.tranwall.capital.common.data.model.Address;
import com.tranwall.capital.common.data.model.Amount;
import com.tranwall.capital.common.data.model.ClearAddress;
import com.tranwall.capital.common.typedid.data.AllocationId;
import com.tranwall.capital.common.typedid.data.BusinessId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.controller.nonprod.type.testdata.CreateTestDataResponse;
import com.tranwall.capital.controller.nonprod.type.testdata.CreateTestDataResponse.TestBusiness;
import com.tranwall.capital.crypto.data.model.embedded.EncryptedString;
import com.tranwall.capital.data.model.Allocation;
import com.tranwall.capital.data.model.Bin;
import com.tranwall.capital.data.model.Business;
import com.tranwall.capital.data.model.BusinessBankAccount;
import com.tranwall.capital.data.model.Card;
import com.tranwall.capital.data.model.Program;
import com.tranwall.capital.data.model.User;
import com.tranwall.capital.data.model.enums.AllocationReallocationType;
import com.tranwall.capital.data.model.enums.BankAccountTransactType;
import com.tranwall.capital.data.model.enums.BusinessReallocationType;
import com.tranwall.capital.data.model.enums.BusinessType;
import com.tranwall.capital.data.model.enums.CardType;
import com.tranwall.capital.data.model.enums.Country;
import com.tranwall.capital.data.model.enums.CreditOrDebit;
import com.tranwall.capital.data.model.enums.Currency;
import com.tranwall.capital.data.model.enums.FundingType;
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
import com.tranwall.capital.service.BusinessService;
import com.tranwall.capital.service.BusinessService.BusinessAndAllocationsRecord;
import com.tranwall.capital.service.CardService;
import com.tranwall.capital.service.NetworkMessageService;
import com.tranwall.capital.service.ProgramService;
import com.tranwall.capital.service.UserService;
import com.tranwall.capital.service.UserService.CreateUpdateUserRecord;
import com.tranwall.capital.service.type.NetworkCommon;
import io.swagger.v3.oas.annotations.Parameter;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
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

  private final AllocationService allocationService;
  private final BinService binService;
  private final BusinessService businessService;
  private final BusinessBankAccountService businessBankAccountService;
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

  @PostConstruct
  void init() {
    allBins = binService.findAllBins();
    Bin bin = allBins.size() > 0 ? allBins.get(0) : createBin();

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

  @GetMapping(value = "/create-all-demo", produces = MediaType.APPLICATION_JSON_VALUE)
  private CreateTestDataResponse createTestData() throws IOException {

    // create a new business
    BusinessAndAllocationsRecord businessAndAllocationsRecord = createBusiness(null);
    Business business = businessAndAllocationsRecord.business();

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

    AllocationRecord parentAllocation = businessAndAllocationsRecord.allocationRecord();

    // create child allocation and load $1326.86
    AllocationRecord childAllocation =
        allocationService.createAllocation(
            business.getId(),
            parentAllocation.allocation().getId(),
            faker.company().name(),
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
    Card card =
        issueCard(business, parentAllocation.allocation(), user.user(), individualVirtualProgram);
    cards.add(card);
    allocationService.reallocateAllocationFunds(
        business,
        parentAllocation.allocation().getId(),
        parentAllocation.allocation().getAccountId(),
        card.getId(),
        AllocationReallocationType.ALLOCATION_TO_CARD,
        Amount.of(business.getCurrency(), BigDecimal.valueOf(173.45)));

    Amount amount = Amount.of(Currency.USD, BigDecimal.TEN);
    networkMessageService.processNetworkMessage(
        new NetworkCommon(
            card.getCardNumber().getEncrypted(),
            card.getExpirationDate(),
            NetworkMessageType.PRE_AUTH_TRANSACTION,
            CreditOrDebit.fromAmount(amount),
            amount.abs(),
            "M1234",
            "Merchant Name",
            new ClearAddress("123 Main Street", "", "Tucson", "AZ", "23416", Country.USA),
            6060));

    card =
        issueCard(business, parentAllocation.allocation(), user.user(), individualPlasticProgram);
    cards.add(card);
    allocationService.reallocateAllocationFunds(
        business,
        parentAllocation.allocation().getId(),
        parentAllocation.allocation().getAccountId(),
        card.getId(),
        AllocationReallocationType.ALLOCATION_TO_CARD,
        Amount.of(business.getCurrency(), BigDecimal.valueOf(91.17)));

    amount = Amount.of(Currency.USD, BigDecimal.valueOf(26.27));
    networkMessageService.processNetworkMessage(
        new NetworkCommon(
            card.getCardNumber().getEncrypted(),
            card.getExpirationDate(),
            NetworkMessageType.FINANCIAL_TRANSACTION,
            CreditOrDebit.fromAmount(amount),
            amount.abs(),
            "M1234",
            "Merchant Name",
            new ClearAddress("123 Main Street", "", "Tucson", "AZ", "23416", Country.USA),
            6060));

    CreateUpdateUserRecord user2 = createUser(business);
    users.add(user2);
    cards.add(
        issueCard(business, childAllocation.allocation(), user2.user(), pooledVirtualProgram));
    cards.add(
        issueCard(business, childAllocation.allocation(), user2.user(), pooledPlasticProgram));

    CreateUpdateUserRecord user3 = createUser(business);
    users.add(user3);
    cards.add(
        issueCard(
            business, grandchildAllocation.allocation(), user3.user(), individualVirtualProgram));

    return new CreateTestDataResponse(
        allBins,
        allPrograms,
        List.of(
            new TestBusiness(
                business,
                Collections.emptyList(),
                List.of(
                    businessAndAllocationsRecord.allocationRecord().allocation(),
                    childAllocation.allocation(),
                    grandchildAllocation.allocation()),
                cards,
                users)));
  }

  private Bin createBin() {
    return binService.createBin(faker.random().nextInt(500000, 599999) + "", "Test Data BIN");
  }

  private Program createProgram(Bin bin, FundingType fundingType, CardType cardType) {
    return programService.createProgram(
        UUID.randomUUID().toString(),
        bin.getBin(),
        fundingType,
        cardType,
        faker.number().digits(8));
  }

  private BusinessAndAllocationsRecord createBusiness(TypedId<BusinessId> businessId) {
    return businessService.createBusiness(
        businessId,
        faker.company().name(),
        BusinessType.LLC,
        generateEntityAddress(),
        generateEmployerIdentificationNumber(),
        faker.internet().emailAddress(),
        faker.phoneNumber().phoneNumber(),
        Currency.USD);
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
      TypedId<BusinessId> businessId, String name, TypedId<AllocationId> parentAllocationId) {
    return allocationService.createAllocation(
        businessId, parentAllocationId, name, Amount.of(Currency.USD));
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
        faker.phoneNumber().phoneNumber(),
        true,
        null);
  }

  public Card issueCard(
      com.tranwall.capital.data.model.Business business,
      Allocation allocation,
      User user,
      Program program) {
    return cardService.issueCard(
        program,
        business.getId(),
        allocation.getId(),
        user.getId(),
        Currency.USD,
        true,
        business.getLegalName());
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
}
