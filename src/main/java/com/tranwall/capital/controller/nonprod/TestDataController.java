package com.tranwall.capital.controller.nonprod;

import com.github.javafaker.Faker;
import com.tranwall.capital.common.data.model.Address;
import com.tranwall.capital.common.data.model.Amount;
import com.tranwall.capital.common.typedid.data.AllocationId;
import com.tranwall.capital.common.typedid.data.BusinessId;
import com.tranwall.capital.common.typedid.data.ProgramId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.controller.nonprod.type.testdata.CreateTestDataResponse;
import com.tranwall.capital.crypto.data.model.embedded.EncryptedString;
import com.tranwall.capital.data.model.Allocation;
import com.tranwall.capital.data.model.Bin;
import com.tranwall.capital.data.model.Card;
import com.tranwall.capital.data.model.Program;
import com.tranwall.capital.data.model.User;
import com.tranwall.capital.data.model.enums.BusinessType;
import com.tranwall.capital.data.model.enums.CardType;
import com.tranwall.capital.data.model.enums.Country;
import com.tranwall.capital.data.model.enums.Currency;
import com.tranwall.capital.data.model.enums.FundingType;
import com.tranwall.capital.data.model.enums.UserType;
import com.tranwall.capital.data.repository.AllocationRepository;
import com.tranwall.capital.data.repository.BusinessRepository;
import com.tranwall.capital.data.repository.CardRepository;
import com.tranwall.capital.service.AllocationService;
import com.tranwall.capital.service.AllocationService.AllocationRecord;
import com.tranwall.capital.service.BinService;
import com.tranwall.capital.service.BusinessBankAccountService;
import com.tranwall.capital.service.BusinessService;
import com.tranwall.capital.service.BusinessService.BusinessAndAllocationsRecord;
import com.tranwall.capital.service.CardService;
import com.tranwall.capital.service.ProgramService;
import com.tranwall.capital.service.UserService;
import com.tranwall.capital.service.UserService.CreateUserRecord;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
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
public class TestDataController {

  private final BusinessBankAccountService businessBankAccountService;
  private final BinService binService;
  private final ProgramService programService;
  private final BusinessService businessService;
  private final AllocationService allocationService;
  private final UserService userService;
  private final CardService cardService;
  private final BusinessRepository businessRepository;
  private final AllocationRepository allocationRepository;
  private final CardRepository cardRepository;
  private final Faker faker = new Faker();
  private List<CreateUserRecord> createUserRecordList = new ArrayList<>();

  @GetMapping("/db-content")
  private CreateTestDataResponse getDbContent() {
    return getGeneratedData();
  }

  @GetMapping(value = "/create-all-demo", produces = MediaType.APPLICATION_JSON_VALUE)
  private CreateTestDataResponse createTestData() throws IOException {

    List<Bin> allBins = binService.findAllBins();
    Bin bin = allBins.size() > 0 ? allBins.get(0) : createBin();
    List<Program> allPrograms = programService.findAllPrograms();
    Program program =
        allPrograms.size() > 0 ? allPrograms.get(0) : createProgram(bin, FundingType.INDIVIDUAL);
    BusinessAndAllocationsRecord business = createBusiness(null, program);
    AllocationRecord parentAllocation =
        createAllocation(
            program.getId(), business.business().getId(), faker.company().name(), null);
    AllocationRecord allocation1 =
        createAllocation(
            program.getId(),
            business.business().getId(),
            faker.company().name(),
            parentAllocation.allocation().getId());
    createAllocation(
        program.getId(),
        business.business().getId(),
        faker.company().name(),
        allocation1.allocation().getId());
    AllocationRecord parentAllocation2 =
        createAllocation(
            program.getId(), business.business().getId(), faker.company().name(), null);

    CreateUserRecord user = createUser(business.business());
    CreateUserRecord user2 = createUser(business.business());
    CreateUserRecord user3 = createUser(business.business());
    createUserRecordList.add(user);
    createUserRecordList.add(user2);
    createUserRecordList.add(user3);

    issueCard(
        business.business(),
        parentAllocation.allocation(),
        user.user(),
        bin,
        program,
        Currency.USD,
        business.business().getId());
    issueCard(
        business.business(),
        allocation1.allocation(),
        user2.user(),
        bin,
        program,
        Currency.USD,
        business.business().getId());
    issueCard(
        business.business(),
        parentAllocation2.allocation(),
        user3.user(),
        bin,
        program,
        Currency.USD,
        business.business().getId());

    return getGeneratedData();
  }

  private Bin createBin() {
    return binService.createBin(faker.random().nextInt(500000, 599999) + "", "Test Data BIN");
  }

  private Program createProgram(Bin bin, FundingType fundingType) {
    return programService.createProgram(
        UUID.randomUUID().toString(), bin.getBin(), fundingType, faker.number().digits(8));
  }

  private BusinessAndAllocationsRecord createBusiness(
      TypedId<BusinessId> businessId, Program... programs) {
    return businessService.createBusiness(
        businessId,
        faker.company().name(),
        BusinessType.LLC,
        generateEntityAddress(),
        generateEmployerIdentificationNumber(),
        faker.internet().emailAddress(),
        faker.phoneNumber().phoneNumber(),
        Arrays.stream(programs).map(Program::getId).toList(),
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
      TypedId<ProgramId> programId,
      TypedId<BusinessId> businessId,
      String name,
      TypedId<AllocationId> parentAllocationId) {
    return allocationService.createAllocation(
        programId, businessId, parentAllocationId, name, Amount.of(Currency.USD));
  }

  public CreateUserRecord createUser(com.tranwall.capital.data.model.Business business)
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
      Bin bin,
      Program program,
      Currency currency,
      TypedId<BusinessId> businessId) {
    return cardService.issueCard(
        program,
        business.getId(),
        allocation.getId(),
        user.getId(),
        currency,
        CardType.VIRTUAL,
        true,
        businessService.getBusiness(businessId).business().getLegalName());
  }

  private CreateTestDataResponse getGeneratedData() {
    List<CreateTestDataResponse.TestBusiness> businesses =
        businessRepository.findAll().stream()
            .map(
                business ->
                    new CreateTestDataResponse.TestBusiness(
                        business,
                        userService.retrieveUsersForBusiness(business.getId()),
                        allocationRepository.findAll().stream()
                            .filter(
                                allocation -> allocation.getBusinessId().equals(business.getId()))
                            .collect(Collectors.toList()),
                        cardRepository.findAll().stream()
                            .filter(card -> card.getBusinessId().equals(business.getId()))
                            .collect(Collectors.toList()),
                        createUserRecordList.stream()
                            .filter(
                                createUserRecord ->
                                    createUserRecord
                                        .user()
                                        .getBusinessId()
                                        .equals(business.getId()))
                            .collect(Collectors.toList())))
            .collect(Collectors.toList());
    return new CreateTestDataResponse(
        binService.findAllBins(), programService.findAllPrograms(), businesses);
  }
}
