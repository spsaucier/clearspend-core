package com.tranwall.capital.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tranwall.capital.BaseCapitalTest;
import com.tranwall.capital.TestHelper;
import com.tranwall.capital.TestHelper.CreateBusinessRecord;
import com.tranwall.capital.common.data.model.Amount;
import com.tranwall.capital.common.typedid.data.BusinessBankAccountId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.controller.nonprod.TestDataController;
import com.tranwall.capital.controller.type.PagedData;
import com.tranwall.capital.controller.type.activity.AccountActivityRequest;
import com.tranwall.capital.controller.type.activity.AccountActivityResponse;
import com.tranwall.capital.controller.type.common.PageRequest;
import com.tranwall.capital.data.model.Account;
import com.tranwall.capital.data.model.Bin;
import com.tranwall.capital.data.model.Business;
import com.tranwall.capital.data.model.Card;
import com.tranwall.capital.data.model.Program;
import com.tranwall.capital.data.model.enums.AccountActivityType;
import com.tranwall.capital.data.model.enums.BankAccountTransactType;
import com.tranwall.capital.data.model.enums.BusinessReallocationType;
import com.tranwall.capital.data.model.enums.Currency;
import com.tranwall.capital.data.model.enums.NetworkMessageType;
import com.tranwall.capital.service.AccountService;
import com.tranwall.capital.service.AllocationService.AllocationRecord;
import com.tranwall.capital.service.BusinessBankAccountService;
import com.tranwall.capital.service.BusinessService;
import com.tranwall.capital.service.NetworkMessageService;
import com.tranwall.capital.service.UserService;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import javax.servlet.http.Cookie;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

@RequiredArgsConstructor(onConstructor = @__({@Autowired}))
@Slf4j
@Transactional
public class AccountActivityControllerTest extends BaseCapitalTest {

  private final MockMvc mvc;
  private final TestHelper testHelper;
  private final BusinessBankAccountService businessBankAccountService;
  private final BusinessService businessService;
  private final AccountService accountService;
  private final NetworkMessageService networkMessageService;

  private Bin bin;
  private Program program;

  @SneakyThrows
  @Test
  void getLatestAccountActivityPageData() {
    String email = testHelper.generateEmail();
    String password = testHelper.generatePassword();
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    TypedId<BusinessBankAccountId> businessBankAccountId =
        testHelper.createBusinessBankAccount(createBusinessRecord.business().getId());
    Business business = createBusinessRecord.business();

    testHelper.createBusinessOwner(business.getId(), email, password);

    Cookie authCookie = testHelper.login(email, password);

    businessBankAccountService.transactBankAccount(
        business.getId(),
        businessBankAccountId,
        BankAccountTransactType.DEPOSIT,
        Amount.of(Currency.USD, new BigDecimal("1000")),
        false);
    Account account =
        accountService.retrieveRootAllocationAccount(
            business.getId(),
            business.getCurrency(),
            createBusinessRecord.allocationRecord().allocation().getId(),
            false);
    AllocationRecord allocation =
        testHelper.createAllocation(
            business.getId(),
            "",
            createBusinessRecord.allocationRecord().allocation().getId(),
            createBusinessRecord.user());
    accountService.reallocateFunds(
        account.getId(),
        allocation.account().getId(),
        new com.tranwall.capital.common.data.model.Amount(Currency.USD, BigDecimal.valueOf(300)));
    businessService.reallocateBusinessFunds(
        business.getId(),
        allocation.allocation().getId(),
        allocation.account().getId(),
        BusinessReallocationType.BUSINESS_TO_ALLOCATION,
        new Amount(Currency.USD, BigDecimal.valueOf(21)));

    AccountActivityRequest accountActivityRequest = new AccountActivityRequest();
    accountActivityRequest.setPageRequest(new PageRequest(0, 10));
    accountActivityRequest.setType(AccountActivityType.BANK_DEPOSIT);
    accountActivityRequest.setAllocationId(
        createBusinessRecord.allocationRecord().allocation().getId());
    accountActivityRequest.setFrom(OffsetDateTime.now().minusDays(1));
    accountActivityRequest.setTo(OffsetDateTime.now().plusDays(1));

    String body = objectMapper.writeValueAsString(accountActivityRequest);

    MockHttpServletResponse response =
        mvc.perform(
                post("/account-activity")
                    .contentType("application/json")
                    .content(body)
                    .cookie(authCookie))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

    // we should have just one bank deposit
    assertEquals(
        1,
        objectMapper.readValue(response.getContentAsString(), PagedData.class).getContent().size());
    log.info(response.getContentAsString());
  }

  @SneakyThrows
  @Test
  void getFilteredAccountActivityPageData() {
    if (bin == null) {
      bin = testHelper.createBin();
      program = testHelper.createProgram(bin);
    }
    String email = testHelper.generateEmail();
    String password = testHelper.generatePassword();
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    TypedId<BusinessBankAccountId> businessBankAccountId =
        testHelper.createBusinessBankAccount(createBusinessRecord.business().getId());
    Business business = createBusinessRecord.business();

    testHelper.createBusinessOwner(business.getId(), email, password);

    Cookie authCookie = testHelper.login(email, password);

    businessBankAccountService.transactBankAccount(
        business.getId(),
        businessBankAccountId,
        BankAccountTransactType.DEPOSIT,
        Amount.of(Currency.USD, new BigDecimal("1000")),
        false);
    Account account =
        accountService.retrieveRootAllocationAccount(
            business.getId(),
            business.getCurrency(),
            createBusinessRecord.allocationRecord().allocation().getId(),
            false);
    AllocationRecord allocation =
        testHelper.createAllocation(
            business.getId(),
            "",
            createBusinessRecord.allocationRecord().allocation().getId(),
            testHelper.createUser(business).user());
    accountService.reallocateFunds(
        account.getId(),
        allocation.account().getId(),
        new com.tranwall.capital.common.data.model.Amount(Currency.USD, BigDecimal.valueOf(300)));
    businessService.reallocateBusinessFunds(
        business.getId(),
        allocation.allocation().getId(),
        allocation.account().getId(),
        BusinessReallocationType.BUSINESS_TO_ALLOCATION,
        new Amount(Currency.USD, BigDecimal.valueOf(21)));

    UserService.CreateUpdateUserRecord user = testHelper.createUser(business);
    Card card =
        testHelper.issueCard(
            business,
            createBusinessRecord.allocationRecord().allocation(),
            user.user(),
            program,
            Currency.USD);

    Amount amount = Amount.of(Currency.USD, BigDecimal.valueOf(100));

    networkMessageService.processNetworkMessage(
        TestDataController.generateNetworkCommon(
            NetworkMessageType.PRE_AUTH_TRANSACTION,
            user.user(),
            card,
            createBusinessRecord.allocationRecord().account(),
            program,
            amount));

    AccountActivityRequest accountActivityRequest = new AccountActivityRequest();
    accountActivityRequest.setPageRequest(new PageRequest(0, 10));
    accountActivityRequest.setSearchText(card.getLastFour());
    accountActivityRequest.setAllocationId(
        createBusinessRecord.allocationRecord().allocation().getId());
    accountActivityRequest.setFrom(OffsetDateTime.now().minusDays(1));
    accountActivityRequest.setTo(OffsetDateTime.now().plusDays(1));

    String body = objectMapper.writeValueAsString(accountActivityRequest);

    MockHttpServletResponse response =
        mvc.perform(
                post("/account-activity")
                    .contentType("application/json")
                    .content(body)
                    .cookie(authCookie))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

    // we should have just one bank deposit
    PagedData<AccountActivityResponse> pagedData =
        objectMapper.readValue(response.getContentAsString(), PagedData.class);
    assertEquals(1, pagedData.getContent().size());
    assertEquals(pagedData.getTotalElements(), 1);
    log.info(response.getContentAsString());
  }
}
