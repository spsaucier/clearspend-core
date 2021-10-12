package com.tranwall.capital.service;

import com.github.javafaker.Faker;
import com.tranwall.capital.common.data.model.Address;
import com.tranwall.capital.crypto.data.model.embedded.EncryptedString;
import com.tranwall.capital.data.model.Allocation;
import com.tranwall.capital.data.model.Bin;
import com.tranwall.capital.data.model.Business;
import com.tranwall.capital.data.model.Card;
import com.tranwall.capital.data.model.Program;
import com.tranwall.capital.data.model.User;
import com.tranwall.capital.data.model.enums.BusinessType;
import com.tranwall.capital.data.model.enums.Country;
import com.tranwall.capital.data.model.enums.Currency;
import com.tranwall.capital.data.model.enums.FundingType;
import com.tranwall.capital.data.model.enums.UserType;
import com.tranwall.capital.service.BusinessService.BusinessAndAllocationsRecord;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ServiceHelper {

  private final BinService binService;
  private final BusinessService businessService;
  private final ProgramService programService;
  private final UserService userService;
  private final CardService cardService;

  private final Faker faker = new Faker();

  // not entirely sure if this is a good idea or not
  //  private BusinessRecord businessRecord;
  //  private Bin bin;
  //  private Program program;
  //  private User user;
  //
  //  synchronized public void initialize() {
  //    if (bin == null) {
  //      bin = createBin();
  //      program = createProgram(bin);
  //      businessRecord = createBusiness(program);
  //      user = createUser(businessRecord.business());
  //    }
  //  }

  public Bin createBin() {
    return binService.createBin(faker.random().nextInt(500000, 599999).toString(), "Unit test BIN");
  }

  public Program createProgram(Bin bin) {
    return programService.createProgram(
        UUID.randomUUID().toString(), bin.getBin(), FundingType.POOLED);
  }

  public BusinessAndAllocationsRecord createBusiness(Program program) {
    return businessService.createBusiness(
        null,
        faker.company().name(),
        BusinessType.LLC,
        generateAddress(),
        faker.number().digits(9),
        faker.internet().emailAddress(),
        faker.phoneNumber().phoneNumber(),
        List.of(program.getId()),
        Currency.USD);
  }

  public User createUser(Business business) {
    return userService.createUser(
        business.getId(),
        UserType.EMPLOYEE,
        faker.name().firstName(),
        faker.name().lastName(),
        generateAddress(),
        faker.internet().emailAddress(),
        faker.phoneNumber().phoneNumber(),
        UUID.randomUUID().toString());
  }

  public Card issueCard(
      Business business,
      Allocation allocation,
      User user,
      Bin bin,
      Program program,
      FundingType fundingType,
      Currency currency) {
    return cardService.issueCard(
        business.getId(),
        allocation.getId(),
        user.getId(),
        bin.getBin(),
        program.getId(),
        fundingType,
        currency);
  }

  public Address generateAddress() {
    return new Address(
        new EncryptedString(faker.address().streetAddress()),
        new EncryptedString(faker.address().secondaryAddress()),
        faker.address().city(),
        faker.address().state(),
        new EncryptedString(faker.address().zipCode()),
        Country.USA);
  }
}
