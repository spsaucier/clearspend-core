package com.tranwall.capital.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tranwall.capital.BaseCapitalTest;
import com.tranwall.capital.TestHelper;
import com.tranwall.capital.TestHelper.CreateBusinessRecord;
import com.tranwall.capital.common.data.model.Amount;
import com.tranwall.capital.common.typedid.data.BusinessBankAccountId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.controller.type.activity.AccountActivityRequest;
import com.tranwall.capital.data.model.Account;
import com.tranwall.capital.data.model.Bin;
import com.tranwall.capital.data.model.Business;
import com.tranwall.capital.data.model.Program;
import com.tranwall.capital.data.model.enums.AccountActivityType;
import com.tranwall.capital.data.model.enums.BankAccountTransactType;
import com.tranwall.capital.data.model.enums.BusinessReallocationType;
import com.tranwall.capital.data.model.enums.Currency;
import com.tranwall.capital.service.AccountService;
import com.tranwall.capital.service.AccountService.AdjustmentRecord;
import com.tranwall.capital.service.AllocationService.AllocationRecord;
import com.tranwall.capital.service.BusinessBankAccountService;
import com.tranwall.capital.service.BusinessService;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import javax.servlet.http.Cookie;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

@RequiredArgsConstructor(onConstructor = @__({@Autowired}))
@Slf4j
public class AccountActivityControllerTest extends BaseCapitalTest {

  private final MockMvc mvc;
  private final TestHelper testHelper;
  private final BusinessBankAccountService businessBankAccountService;
  private final BusinessService businessService;
  private final AccountService accountService;

  private Bin bin;
  private Program program;
  private Cookie authCookie;

  @BeforeEach
  public void setup() {
    if (bin == null) {
      bin = testHelper.createBin();
      program = testHelper.createProgram(bin);
    }
    this.authCookie = testHelper.login("business-owner-tester@clearspend.com", "Password1!");
  }

  @SneakyThrows
  @Test
  void getLatestAccountActivityPageData() {

    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    TypedId<BusinessBankAccountId> businessBankAccountId =
        testHelper.createBusinessBankAccount(createBusinessRecord.business().getId());
    Business business = createBusinessRecord.business();
    AdjustmentRecord adjustmentRecord =
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
    AllocationRecord parentAllocationRecord =
        testHelper.createAllocation(
            business.getId(), "", createBusinessRecord.allocationRecord().allocation().getId());
    accountService.reallocateFunds(
        account.getId(),
        parentAllocationRecord.account().getId(),
        new com.tranwall.capital.common.data.model.Amount(Currency.USD, BigDecimal.valueOf(300)));
    businessService.reallocateBusinessFunds(
        business.getId(),
        parentAllocationRecord.allocation().getId(),
        parentAllocationRecord.account().getId(),
        BusinessReallocationType.BUSINESS_TO_ALLOCATION,
        new Amount(Currency.USD, BigDecimal.valueOf(21)));

    AccountActivityRequest accountActivityRequest = new AccountActivityRequest();
    accountActivityRequest.setPageRequest(null);
    accountActivityRequest.setAccountId(account.getId());
    accountActivityRequest.setType(AccountActivityType.BANK_DEPOSIT);
    accountActivityRequest.setAllocationId(parentAllocationRecord.allocation().getId());
    accountActivityRequest.setFrom(OffsetDateTime.now());
    accountActivityRequest.setTo(OffsetDateTime.now());

    String body = objectMapper.writeValueAsString(accountActivityRequest);

    MockHttpServletResponse response =
        mvc.perform(
                post("/account-activity")
                    .header("businessId", business.getId())
                    .contentType("application/json")
                    .content(body)
                    .cookie(authCookie))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

    log.info(response.getContentAsString());
  }
}
