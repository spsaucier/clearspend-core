package com.clearspend.capital.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.TestHelper.CreateBusinessRecord;
import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.controller.nonprod.TestDataController;
import com.clearspend.capital.controller.nonprod.TestDataController.NetworkCommonAuthorization;
import com.clearspend.capital.controller.type.PagedData;
import com.clearspend.capital.controller.type.activity.AccountActivityRequest;
import com.clearspend.capital.controller.type.activity.AccountActivityResponse;
import com.clearspend.capital.controller.type.activity.FilterAmount;
import com.clearspend.capital.controller.type.common.PageRequest;
import com.clearspend.capital.data.model.Account;
import com.clearspend.capital.data.model.AccountActivity;
import com.clearspend.capital.data.model.Card;
import com.clearspend.capital.data.model.ExpenseCategory;
import com.clearspend.capital.data.model.business.Business;
import com.clearspend.capital.data.model.business.BusinessBankAccount;
import com.clearspend.capital.data.model.embedded.ExpenseDetails;
import com.clearspend.capital.data.model.embedded.MerchantDetails;
import com.clearspend.capital.data.model.embedded.ReceiptDetails;
import com.clearspend.capital.data.model.enums.AccountActivityType;
import com.clearspend.capital.data.model.enums.BankAccountTransactType;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.data.model.enums.FundingType;
import com.clearspend.capital.data.model.enums.MccGroup;
import com.clearspend.capital.data.model.enums.MerchantType;
import com.clearspend.capital.data.model.enums.card.CardType;
import com.clearspend.capital.data.model.security.DefaultRoles;
import com.clearspend.capital.data.repository.AccountActivityRepository;
import com.clearspend.capital.data.repository.ExpenseCategoryRepository;
import com.clearspend.capital.service.AccountService;
import com.clearspend.capital.service.AllocationService.AllocationRecord;
import com.clearspend.capital.service.BusinessBankAccountService;
import com.clearspend.capital.service.BusinessService;
import com.clearspend.capital.service.NetworkMessageService;
import com.clearspend.capital.service.ReceiptService;
import com.clearspend.capital.service.ServiceHelper;
import com.clearspend.capital.service.UserService.CreateUpdateUserRecord;
import com.clearspend.capital.testutils.data.TestDataHelper;
import com.clearspend.capital.testutils.data.TestDataHelper.AccountActivityConfig;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

@SuppressWarnings("JavaTimeDefaultTimeZone")
@RequiredArgsConstructor(onConstructor = @__({@Autowired}))
@Slf4j
public class AccountActivityControllerTest extends BaseCapitalTest {

  private final MockMvc mvc;
  private final TestHelper testHelper;
  private final TestDataHelper testDataHelper;
  private final BusinessBankAccountService businessBankAccountService;
  private final BusinessService businessService;
  private final AccountService accountService;
  private final NetworkMessageService networkMessageService;
  private final AccountActivityRepository accountActivityRepository;
  private final ReceiptService receiptService;
  private final EntityManager entityManager;
  private final ServiceHelper serviceHelper;
  private final ExpenseCategoryRepository expenseCategoryRepository;

  @SneakyThrows
  @Test
  void getLatestAccountActivityPageData() {
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    testHelper.setCurrentUser(createBusinessRecord.user());
    BusinessBankAccount businessBankAccount =
        testHelper.createBusinessBankAccount(createBusinessRecord.business().getId());
    Business business = createBusinessRecord.business();

    testHelper.setCurrentUser(createBusinessRecord.user());

    Amount amount = Amount.of(Currency.USD, new BigDecimal("1000"));
    businessBankAccountService.transactBankAccount(
        business.getId(),
        businessBankAccount.getId(),
        createBusinessRecord.user().getId(),
        BankAccountTransactType.DEPOSIT,
        amount,
        false);
    Account account =
        serviceHelper
            .accountService()
            .retrieveRootAllocationAccount(
                business.getId(),
                business.getCurrency(),
                createBusinessRecord.allocationRecord().allocation().getId(),
                false);
    AllocationRecord allocation =
        testHelper.createAllocation(
            business.getId(), "", createBusinessRecord.allocationRecord().allocation().getId());
    serviceHelper
        .accountService()
        .reallocateFunds(
            account.getId(),
            allocation.account().getId(),
            new Amount(Currency.USD, BigDecimal.valueOf(300)));
    businessService.reallocateBusinessFunds(
        business.getId(),
        createBusinessRecord.user().getId(),
        createBusinessRecord.allocationRecord().allocation().getId(),
        allocation.allocation().getId(),
        new Amount(Currency.USD, BigDecimal.valueOf(21)));

    AccountActivityRequest accountActivityRequest = new AccountActivityRequest();
    accountActivityRequest.setPageRequest(new PageRequest(0, 10));
    accountActivityRequest.setTypes(List.of(AccountActivityType.BANK_DEPOSIT_STRIPE));
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
                    .cookie(createBusinessRecord.authCookie()))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

    // we should have just one bank deposit
    PagedData<AccountActivityResponse> pagedData =
        objectMapper.readValue(
            response.getContentAsString(),
            objectMapper
                .getTypeFactory()
                .constructParametricType(PagedData.class, AccountActivityResponse.class));
    assertEquals(1, pagedData.getContent().size());
    assertEquals(pagedData.getTotalElements(), 1);
    log.info(response.getContentAsString());
    assertThat(pagedData.getContent().get(0).getAmount()).isEqualTo(amount);
    assertThat(pagedData.getContent().get(0).getRequestedAmount()).isEqualTo(amount);
  }

  @SneakyThrows
  @Test
  void getFilteredByTextAccountActivityPageData() {
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    testHelper.setCurrentUser(createBusinessRecord.user());
    BusinessBankAccount businessBankAccount =
        testHelper.createBusinessBankAccount(createBusinessRecord.business().getId());
    Business business = createBusinessRecord.business();

    testHelper.setCurrentUser(createBusinessRecord.user());

    businessBankAccountService.transactBankAccount(
        business.getId(),
        businessBankAccount.getId(),
        createBusinessRecord.user().getId(),
        BankAccountTransactType.DEPOSIT,
        Amount.of(Currency.USD, new BigDecimal("1000")),
        false);
    Account account =
        serviceHelper
            .accountService()
            .retrieveRootAllocationAccount(
                business.getId(),
                business.getCurrency(),
                createBusinessRecord.allocationRecord().allocation().getId(),
                false);
    AllocationRecord allocation =
        testHelper.createAllocation(
            business.getId(), "", createBusinessRecord.allocationRecord().allocation().getId());
    serviceHelper
        .accountService()
        .reallocateFunds(
            account.getId(),
            allocation.account().getId(),
            new Amount(Currency.USD, BigDecimal.valueOf(300)));
    businessService.reallocateBusinessFunds(
        business.getId(),
        createBusinessRecord.user().getId(),
        createBusinessRecord.allocationRecord().allocation().getId(),
        allocation.allocation().getId(),
        new Amount(Currency.USD, BigDecimal.valueOf(21)));

    CreateUpdateUserRecord user =
        testHelper.createUserWithRole(
            createBusinessRecord.allocationRecord().allocation(), DefaultRoles.ALLOCATION_EMPLOYEE);
    Card card =
        testHelper.issueCard(
            business,
            createBusinessRecord.allocationRecord().allocation(),
            user.user(),
            Currency.USD,
            FundingType.POOLED,
            CardType.PHYSICAL,
            true);

    Amount amount = Amount.of(Currency.USD, BigDecimal.valueOf(100));

    NetworkCommonAuthorization networkCommonAuthorization =
        TestDataController.generateAuthorizationNetworkCommon(
            user.user(), card, createBusinessRecord.allocationRecord().account(), amount);
    testHelper.runWithWebhookUser(
        createBusinessRecord.user(),
        () -> {
          networkMessageService.processNetworkMessage(networkCommonAuthorization.networkCommon());
        });
    assertThat(networkCommonAuthorization.networkCommon().isPostAdjustment()).isFalse();
    assertThat(networkCommonAuthorization.networkCommon().isPostDecline()).isFalse();
    assertThat(networkCommonAuthorization.networkCommon().isPostHold()).isTrue();
    entityManager.flush();

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
                    .cookie(createBusinessRecord.authCookie()))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

    // we should have just one bank deposit
    PagedData<AccountActivityResponse> pagedData =
        objectMapper.readValue(
            response.getContentAsString(),
            objectMapper
                .getTypeFactory()
                .constructParametricType(PagedData.class, AccountActivityResponse.class));
    assertEquals(1, pagedData.getContent().size());
    assertEquals(pagedData.getTotalElements(), 1);
    log.info(response.getContentAsString());
  }

  @SneakyThrows
  @Test
  void getFilteredAccountActivityPageDataForVisibleAfterCase() {
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    testHelper.setCurrentUser(createBusinessRecord.user());
    BusinessBankAccount businessBankAccount =
        testHelper.createBusinessBankAccount(createBusinessRecord.business().getId());
    Business business = createBusinessRecord.business();

    testHelper.setCurrentUser(createBusinessRecord.user());

    businessBankAccountService.transactBankAccount(
        business.getId(),
        businessBankAccount.getId(),
        createBusinessRecord.user().getId(),
        BankAccountTransactType.DEPOSIT,
        Amount.of(Currency.USD, new BigDecimal("100")),
        true);

    AccountActivityRequest accountActivityRequest = new AccountActivityRequest();
    accountActivityRequest.setPageRequest(new PageRequest(0, 10));
    accountActivityRequest.setFrom(OffsetDateTime.now().minusDays(1));
    accountActivityRequest.setTo(OffsetDateTime.now().plusDays(1));

    String body = objectMapper.writeValueAsString(accountActivityRequest);

    MockHttpServletResponse response =
        mvc.perform(
                post("/account-activity")
                    .contentType("application/json")
                    .content(body)
                    .cookie(createBusinessRecord.authCookie()))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

    PagedData<AccountActivityResponse> pagedData =
        objectMapper.readValue(
            response.getContentAsString(),
            objectMapper
                .getTypeFactory()
                .constructParametricType(PagedData.class, AccountActivityResponse.class));
    assertEquals(1, pagedData.getTotalElements());
    List<AccountActivityResponse> pagedDataContent = pagedData.getContent();
    AccountActivity accountActivity =
        accountActivityRepository
            .findById(pagedDataContent.get(0).getAccountActivityId())
            .orElse(null);
    assertThat(accountActivity).isNotNull();
    assertTrue(accountActivity.getHideAfter().isAfter(OffsetDateTime.now()));
    assertEquals(2, accountActivityRepository.findAll().size());
    log.info(response.getContentAsString());
  }

  @SneakyThrows
  @Test
  void getFilteredAccountActivityPageDataForHideAfterCase() {
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    testHelper.setCurrentUser(createBusinessRecord.user());
    BusinessBankAccount businessBankAccount =
        testHelper.createBusinessBankAccount(createBusinessRecord.business().getId());
    Business business = createBusinessRecord.business();

    testHelper.setCurrentUser(createBusinessRecord.user());

    businessBankAccountService.transactBankAccount(
        business.getId(),
        businessBankAccount.getId(),
        createBusinessRecord.user().getId(),
        BankAccountTransactType.DEPOSIT,
        Amount.of(Currency.USD, new BigDecimal("100")),
        true);

    accountActivityRepository.saveAllAndFlush(
        accountActivityRepository.findAll().stream()
            .peek(
                accountActivity -> {
                  if (accountActivity.getHideAfter() != null) {
                    accountActivity.setHideAfter(accountActivity.getHideAfter().minusDays(20));
                  }
                  if (accountActivity.getVisibleAfter() != null) {
                    accountActivity.setVisibleAfter(
                        accountActivity.getVisibleAfter().minusDays(20));
                  }
                })
            .collect(Collectors.toList()));

    AccountActivityRequest accountActivityRequest = new AccountActivityRequest();
    accountActivityRequest.setPageRequest(new PageRequest(0, 10));
    accountActivityRequest.setFrom(OffsetDateTime.now().minusDays(1));
    accountActivityRequest.setTo(OffsetDateTime.now().plusDays(1));

    String body = objectMapper.writeValueAsString(accountActivityRequest);

    MockHttpServletResponse response =
        mvc.perform(
                post("/account-activity")
                    .contentType("application/json")
                    .content(body)
                    .cookie(createBusinessRecord.authCookie()))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

    PagedData<AccountActivityResponse> pagedData =
        objectMapper.readValue(
            response.getContentAsString(),
            objectMapper
                .getTypeFactory()
                .constructParametricType(PagedData.class, AccountActivityResponse.class));
    assertEquals(1, pagedData.getTotalElements());
    List<AccountActivityResponse> pagedDataContent = pagedData.getContent();
    AccountActivity accountActivity =
        accountActivityRepository
            .findById(pagedDataContent.get(0).getAccountActivityId())
            .orElse(null);
    assertThat(accountActivity).isNotNull();
    assertTrue(accountActivity.getVisibleAfter().isBefore(OffsetDateTime.now()));
    assertEquals(2, accountActivityRepository.findAll().size());
    log.info(response.getContentAsString());
  }

  @SneakyThrows
  @Test
  void getFilteredAccountActivityPageDataWithReceipt() {
    final CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    testHelper.setCurrentUser(createBusinessRecord.user());

    testDataHelper.createAccountActivity(
        AccountActivityConfig.fromCreateBusinessRecord(createBusinessRecord).build());
    testDataHelper.createAccountActivity(
        AccountActivityConfig.fromCreateBusinessRecord(createBusinessRecord)
            .receipt(new ReceiptDetails())
            .build());
    testDataHelper.createAccountActivity(
        AccountActivityConfig.fromCreateBusinessRecord(createBusinessRecord)
            .receipt(new ReceiptDetails(Set.of(new TypedId<>())))
            .build());

    AccountActivityRequest accountActivityRequest = new AccountActivityRequest();
    accountActivityRequest.setPageRequest(new PageRequest(0, 10));
    accountActivityRequest.setAllocationId(
        createBusinessRecord.allocationRecord().allocation().getId());
    accountActivityRequest.setFrom(OffsetDateTime.now().minusDays(1));
    accountActivityRequest.setTo(OffsetDateTime.now().plusDays(1));
    accountActivityRequest.setWithReceipt(true);

    String body = objectMapper.writeValueAsString(accountActivityRequest);

    MockHttpServletResponse response =
        mvc.perform(
                post("/account-activity")
                    .contentType("application/json")
                    .content(body)
                    .cookie(createBusinessRecord.authCookie()))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

    PagedData<AccountActivityResponse> pagedData =
        objectMapper.readValue(
            response.getContentAsString(),
            objectMapper
                .getTypeFactory()
                .constructParametricType(PagedData.class, AccountActivityResponse.class));
    assertEquals(1, pagedData.getContent().size());
    assertEquals(1, pagedData.getTotalElements());
    log.info(response.getContentAsString());
  }

  @SneakyThrows
  @Test
  void getFilteredAccountActivityPageDataWithOutReceipt() {
    final CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    testHelper.setCurrentUser(createBusinessRecord.user());

    testDataHelper.createAccountActivity(
        AccountActivityConfig.fromCreateBusinessRecord(createBusinessRecord).build());
    testDataHelper.createAccountActivity(
        AccountActivityConfig.fromCreateBusinessRecord(createBusinessRecord)
            .receipt(new ReceiptDetails())
            .build());
    testDataHelper.createAccountActivity(
        AccountActivityConfig.fromCreateBusinessRecord(createBusinessRecord)
            .receipt(new ReceiptDetails(Set.of()))
            .build());

    AccountActivityRequest accountActivityRequest = new AccountActivityRequest();
    accountActivityRequest.setPageRequest(new PageRequest(0, 10));
    accountActivityRequest.setAllocationId(
        createBusinessRecord.allocationRecord().allocation().getId());
    accountActivityRequest.setFrom(OffsetDateTime.now().minusDays(1));
    accountActivityRequest.setTo(OffsetDateTime.now().plusDays(1));
    accountActivityRequest.setWithoutReceipt(true);

    entityManager.flush();

    String body = objectMapper.writeValueAsString(accountActivityRequest);

    MockHttpServletResponse response =
        mvc.perform(
                post("/account-activity")
                    .contentType("application/json")
                    .content(body)
                    .cookie(createBusinessRecord.authCookie()))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

    PagedData<AccountActivityResponse> pagedData =
        objectMapper.readValue(
            response.getContentAsString(),
            objectMapper
                .getTypeFactory()
                .constructParametricType(PagedData.class, AccountActivityResponse.class));
    assertEquals(3, pagedData.getContent().size());
    assertEquals(3, pagedData.getTotalElements());
    log.info(response.getContentAsString());
  }

  @SneakyThrows
  @Test
  void getFilteredAccountActivityPageDataForAmountMinMax() {
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    testHelper.setCurrentUser(createBusinessRecord.user());
    BusinessBankAccount businessBankAccount =
        testHelper.createBusinessBankAccount(createBusinessRecord.business().getId());
    Business business = createBusinessRecord.business();

    testHelper.setCurrentUser(createBusinessRecord.user());

    businessBankAccountService.transactBankAccount(
        business.getId(),
        businessBankAccount.getId(),
        createBusinessRecord.user().getId(),
        BankAccountTransactType.DEPOSIT,
        Amount.of(Currency.USD, new BigDecimal("1000")),
        false);
    Account account =
        serviceHelper
            .accountService()
            .retrieveRootAllocationAccount(
                business.getId(),
                business.getCurrency(),
                createBusinessRecord.allocationRecord().allocation().getId(),
                false);
    AllocationRecord allocation =
        testHelper.createAllocation(
            business.getId(), "", createBusinessRecord.allocationRecord().allocation().getId());
    serviceHelper
        .accountService()
        .reallocateFunds(
            account.getId(),
            allocation.account().getId(),
            new Amount(Currency.USD, BigDecimal.valueOf(300)));
    businessService.reallocateBusinessFunds(
        business.getId(),
        createBusinessRecord.user().getId(),
        createBusinessRecord.allocationRecord().allocation().getId(),
        allocation.allocation().getId(),
        new Amount(Currency.USD, BigDecimal.valueOf(21)));

    Card card =
        testHelper.issueCard(
            business,
            createBusinessRecord.allocationRecord().allocation(),
            createBusinessRecord.user(),
            Currency.USD,
            FundingType.POOLED,
            CardType.PHYSICAL,
            true);

    Amount amount = Amount.of(Currency.USD, BigDecimal.valueOf(100));

    NetworkCommonAuthorization networkCommonAuthorization =
        TestDataController.generateAuthorizationNetworkCommon(
            createBusinessRecord.user(),
            card,
            createBusinessRecord.allocationRecord().account(),
            amount);
    testHelper.runWithWebhookUser(
        createBusinessRecord.user(),
        () -> {
          networkMessageService.processNetworkMessage(networkCommonAuthorization.networkCommon());
        });
    assertThat(networkCommonAuthorization.networkCommon().isPostAdjustment()).isFalse();
    assertThat(networkCommonAuthorization.networkCommon().isPostDecline()).isFalse();
    assertThat(networkCommonAuthorization.networkCommon().isPostHold()).isTrue();
    entityManager.flush();

    AccountActivityRequest accountActivityRequest = new AccountActivityRequest();
    accountActivityRequest.setPageRequest(new PageRequest(0, 10));
    accountActivityRequest.setAllocationId(
        createBusinessRecord.allocationRecord().allocation().getId());
    accountActivityRequest.setFrom(OffsetDateTime.now().minusDays(1));
    accountActivityRequest.setTo(OffsetDateTime.now().plusDays(1));
    accountActivityRequest.setFilterAmount(
        new FilterAmount(BigDecimal.valueOf(20), BigDecimal.valueOf(150)));

    String body = objectMapper.writeValueAsString(accountActivityRequest);

    MockHttpServletResponse response =
        mvc.perform(
                post("/account-activity")
                    .contentType("application/json")
                    .content(body)
                    .cookie(createBusinessRecord.authCookie()))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

    PagedData<AccountActivityResponse> pagedData =
        objectMapper.readValue(
            response.getContentAsString(),
            objectMapper
                .getTypeFactory()
                .constructParametricType(PagedData.class, AccountActivityResponse.class));
    assertEquals(2, pagedData.getContent().size());
    assertEquals(pagedData.getTotalElements(), 2);
    log.info(response.getContentAsString());
  }

  @SneakyThrows
  @Test
  void getFilteredAccountActivityPageDataForCategoriesAsset() {
    final CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    testHelper.setCurrentUser(createBusinessRecord.user());

    ExpenseCategory assets = expenseCategoryRepository.findFirstCategoryByName("Assets").get();
    ExpenseDetails expenseDetails =
        new ExpenseDetails(assets.getIconRef(), assets.getId(), assets.getCategoryName());

    ExpenseCategory fuel = expenseCategoryRepository.findFirstCategoryByName("Fuel").get();
    ExpenseDetails expenseDetailsFuel =
        new ExpenseDetails(fuel.getIconRef(), fuel.getId(), fuel.getCategoryName());

    testDataHelper.createAccountActivity(
        AccountActivityConfig.fromCreateBusinessRecord(createBusinessRecord)
            .expenseDetails(expenseDetails)
            .build());
    testDataHelper.createAccountActivity(
        AccountActivityConfig.fromCreateBusinessRecord(createBusinessRecord)
            .receipt(new ReceiptDetails())
            .expenseDetails(expenseDetailsFuel)
            .build());
    testDataHelper.createAccountActivity(
        AccountActivityConfig.fromCreateBusinessRecord(createBusinessRecord)
            .receipt(new ReceiptDetails(Set.of(new TypedId<>())))
            .build());

    AccountActivityRequest accountActivityRequest = new AccountActivityRequest();
    accountActivityRequest.setPageRequest(new PageRequest(0, 10));
    accountActivityRequest.setCategories(List.of("Assets", "Fuel", "Mandarina"));

    String body = objectMapper.writeValueAsString(accountActivityRequest);

    MockHttpServletResponse response =
        mvc.perform(
                post("/account-activity")
                    .contentType("application/json")
                    .content(body)
                    .cookie(createBusinessRecord.authCookie()))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

    PagedData<AccountActivityResponse> pagedData =
        objectMapper.readValue(
            response.getContentAsString(),
            objectMapper
                .getTypeFactory()
                .constructParametricType(PagedData.class, AccountActivityResponse.class));
    assertEquals(2, pagedData.getContent().size());
    assertEquals(pagedData.getTotalElements(), 2);
    log.info(response.getContentAsString());
  }

  @SneakyThrows
  @Test
  void accountActivitySearchResultsContainMerchantStatementDescriptorIfPresent() {
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    testHelper.setCurrentUser(createBusinessRecord.user());
    BusinessBankAccount businessBankAccount =
        testHelper.createBusinessBankAccount(createBusinessRecord.business().getId());
    Business business = createBusinessRecord.business();

    testHelper.setCurrentUser(createBusinessRecord.user());

    MerchantDetails details = new MerchantDetails();
    details.setName("Merch Name");
    details.setType(MerchantType.UNKNOWN);
    details.setMerchantNumber("123");
    details.setMerchantCategoryCode(123);
    details.setMerchantCategoryGroup(MccGroup.GAMBLING);
    details.setStatementDescriptor("test descriptor");
    testDataHelper.createAccountActivity(
        AccountActivityConfig.fromCreateBusinessRecord(createBusinessRecord)
            .merchant(details)
            .build());

    AccountActivityRequest accountActivityRequest = new AccountActivityRequest();
    accountActivityRequest.setPageRequest(new PageRequest(0, 10));

    String body = objectMapper.writeValueAsString(accountActivityRequest);

    MockHttpServletResponse response =
        mvc.perform(
                post("/account-activity")
                    .contentType("application/json")
                    .content(body)
                    .cookie(createBusinessRecord.authCookie()))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

    // we should have just one bank deposit
    PagedData<AccountActivityResponse> pagedData =
        objectMapper.readValue(
            response.getContentAsString(),
            objectMapper
                .getTypeFactory()
                .constructParametricType(PagedData.class, AccountActivityResponse.class));
    assertEquals(1, pagedData.getContent().size());
    assertEquals(pagedData.getTotalElements(), 1);
    log.info(response.getContentAsString());
    assertThat(pagedData.getContent().get(0).getMerchant().getStatementDescriptor())
        .isEqualTo("test descriptor");
  }
}
