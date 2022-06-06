package com.clearspend.capital.controller.business;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.MockMvcHelper;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.TestHelper.CreateBusinessRecord;
import com.clearspend.capital.client.stripe.StripeMockClient;
import com.clearspend.capital.common.advice.GlobalControllerExceptionHandler.ControllerError;
import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.PlaidLogEntryId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.controller.type.Address;
import com.clearspend.capital.controller.type.PagedData;
import com.clearspend.capital.controller.type.account.Account;
import com.clearspend.capital.controller.type.allocation.SearchBusinessAllocationRequest;
import com.clearspend.capital.controller.type.allocation.UpdateAllocationBalanceRequest;
import com.clearspend.capital.controller.type.business.BusinessSettings;
import com.clearspend.capital.controller.type.business.BusinessSettings.BusinessLimitOperationRecord;
import com.clearspend.capital.controller.type.business.UpdateBusiness;
import com.clearspend.capital.controller.type.business.reallocation.BusinessFundAllocationResponse;
import com.clearspend.capital.controller.type.business.reallocation.BusinessReallocationRequest;
import com.clearspend.capital.controller.type.plaid.PlaidLogEntryDetails;
import com.clearspend.capital.controller.type.plaid.PlaidLogEntryDetails.DefaultPlaidLogEntryDetails;
import com.clearspend.capital.controller.type.plaid.PlaidLogEntryMetadata;
import com.clearspend.capital.controller.type.plaid.PlaidLogEntryRequest;
import com.clearspend.capital.crypto.data.model.embedded.EncryptedString;
import com.clearspend.capital.data.model.Allocation;
import com.clearspend.capital.data.model.PlaidLogEntry;
import com.clearspend.capital.data.model.User;
import com.clearspend.capital.data.model.business.Business;
import com.clearspend.capital.data.model.enums.AchFundsAvailabilityMode;
import com.clearspend.capital.data.model.enums.BusinessOnboardingStep;
import com.clearspend.capital.data.model.enums.BusinessStatus;
import com.clearspend.capital.data.model.enums.BusinessType;
import com.clearspend.capital.data.model.enums.Country;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.data.model.enums.KnowYourBusinessStatus;
import com.clearspend.capital.data.model.enums.LimitPeriod;
import com.clearspend.capital.data.model.enums.LimitType;
import com.clearspend.capital.data.model.enums.PlaidResponseType;
import com.clearspend.capital.data.model.enums.TimeZone;
import com.clearspend.capital.data.model.security.DefaultRoles;
import com.clearspend.capital.data.repository.AllocationRepository;
import com.clearspend.capital.data.repository.PlaidLogEntryRepository;
import com.clearspend.capital.data.repository.UserRepository;
import com.clearspend.capital.data.repository.business.BusinessRepository;
import com.clearspend.capital.service.AccountService;
import com.clearspend.capital.service.AllocationService;
import com.clearspend.capital.service.AllocationService.AllocationRecord;
import com.clearspend.capital.service.BusinessService;
import com.clearspend.capital.service.ServiceHelper;
import com.clearspend.capital.testutils.permission.PermissionValidationHelper;
import com.clearspend.capital.util.function.ThrowableFunctions;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.plaid.client.model.AccountsGetResponse;
import com.plaid.client.model.AuthGetResponse;
import com.plaid.client.model.IdentityGetResponse;
import com.plaid.client.model.ItemPublicTokenExchangeResponse;
import com.plaid.client.model.LinkTokenCreateResponse;
import com.plaid.client.model.SandboxPublicTokenCreateResponse;
import com.stripe.model.issuing.Cardholder;
import com.stripe.param.issuing.CardholderUpdateParams.Status;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.servlet.http.Cookie;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.utility.ThrowingFunction;

@RequiredArgsConstructor(onConstructor = @__({@Autowired}))
@Slf4j
public class BusinessControllerTest extends BaseCapitalTest {

  private final MockMvc mvc;
  private final MockMvcHelper mvcHelper;
  private final TestHelper testHelper;

  private final BusinessService businessService;
  private final BusinessRepository businessRepository;
  private final StripeMockClient stripeMockClient;
  private final UserRepository userRepository;
  private final AllocationRepository allocationRepository;
  private final PermissionValidationHelper permissionValidationHelper;
  private final PlaidLogEntryRepository plaidLogEntryRepository;

  private final AccountService accountService;
  private final AllocationService allocationService;
  private final ServiceHelper serviceHelper;

  private Cookie authCookie;
  private CreateBusinessRecord createBusinessRecord;

  @BeforeEach
  void init() {
    createBusinessRecord = testHelper.init();
    authCookie = createBusinessRecord.authCookie();
    testHelper.setCurrentUser(createBusinessRecord.user());
  }

  @AfterEach
  void cleanup() {
    stripeMockClient.reset();
  }

  @SneakyThrows
  @Test
  void getBusiness_success() {
    Business business = createBusinessRecord.business();

    MockHttpServletResponse response =
        mvc.perform(get("/businesses/").contentType("application/json").cookie(this.authCookie))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

    com.clearspend.capital.controller.type.business.Business jsonBusiness =
        objectMapper.readValue(
            response.getContentAsString(),
            com.clearspend.capital.controller.type.business.Business.class);

    assertThat(jsonBusiness.getBusinessId()).isEqualTo(business.getId());
    assertThat(jsonBusiness.getLegalName()).isEqualTo(business.getLegalName());
    assertThat(jsonBusiness.getBusinessType()).isEqualTo(business.getType());
    assertThat(jsonBusiness.getEmployerIdentificationNumber())
        .isEqualTo(business.getEmployerIdentificationNumber());
    assertThat(jsonBusiness.getBusinessPhone())
        .isEqualTo(business.getBusinessPhone().getEncrypted());
    assertThat(jsonBusiness.getAddress()).isEqualTo(new Address(business.getClearAddress()));
    assertThat(jsonBusiness.getOnboardingStep()).isEqualTo(business.getOnboardingStep());
    assertThat(jsonBusiness.getKnowYourBusinessStatus())
        .isEqualTo(business.getKnowYourBusinessStatus());
    assertThat(jsonBusiness.getStatus()).isEqualTo(business.getStatus());
    assertThat(jsonBusiness.getTimeZone()).isNotNull();
  }

  @Test
  void getPlaidLogsForBusiness_UserPermissions() {
    final ThrowingFunction<Cookie, ResultActions> action =
        cookie ->
            mvc.perform(
                post("/businesses/%s/plaid/logs"
                        .formatted(
                            createBusinessRecord.business().getBusinessId().toUuid().toString()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(new PlaidLogEntryRequest()))
                    .cookie(cookie));
    permissionValidationHelper
        .buildValidator(createBusinessRecord)
        .allowGlobalRoles(DefaultRoles.GLOBAL_CUSTOMER_SERVICE_MANAGER)
        .build()
        .validateMockMvcCall(action);
  }

  @Test
  @SneakyThrows
  void reallocateBusinessFunds_FromAllocationArchived() {
    final AllocationRecord childAllocation =
        testHelper.createAllocation(
            createBusinessRecord.business().getId(),
            "Child",
            createBusinessRecord.allocationRecord().allocation().getId());
    createBusinessRecord.allocationRecord().allocation().setArchived(true);
    allocationRepository.saveAndFlush(createBusinessRecord.allocationRecord().allocation());
    final BusinessReallocationRequest request =
        new BusinessReallocationRequest(
            createBusinessRecord.allocationRecord().allocation().getId(),
            childAllocation.allocation().getId(),
            new com.clearspend.capital.controller.type.Amount(Currency.USD, new BigDecimal("10")));

    final String response =
        mvcHelper
            .query(
                "/businesses/transactions",
                HttpMethod.POST,
                createBusinessRecord.authCookie(),
                request)
            .andExpect(status().isBadRequest())
            .andReturn()
            .getResponse()
            .getContentAsString();
    final ControllerError error = objectMapper.readValue(response, ControllerError.class);
    assertThat(error).hasFieldOrPropertyWithValue("message", "Allocation is archived");
  }

  @Test
  @SneakyThrows
  void reallocateBusinessFunds_ToAllocationArchived() {
    final AllocationRecord childAllocation =
        testHelper.createAllocation(
            createBusinessRecord.business().getId(),
            "Child",
            createBusinessRecord.allocationRecord().allocation().getId());
    childAllocation.allocation().setArchived(true);
    allocationRepository.saveAndFlush(childAllocation.allocation());
    final BusinessReallocationRequest request =
        new BusinessReallocationRequest(
            createBusinessRecord.allocationRecord().allocation().getId(),
            childAllocation.allocation().getId(),
            new com.clearspend.capital.controller.type.Amount(Currency.USD, new BigDecimal("10")));
    final String response =
        mvcHelper
            .query(
                "/businesses/transactions",
                HttpMethod.POST,
                createBusinessRecord.authCookie(),
                request)
            .andExpect(status().isBadRequest())
            .andReturn()
            .getResponse()
            .getContentAsString();
    final ControllerError error = objectMapper.readValue(response, ControllerError.class);
    assertThat(error).hasFieldOrPropertyWithValue("message", "Allocation is archived");
  }

  @Test
  @SneakyThrows
  void updateAllocationBalance_AllocationArchived() {
    final UpdateAllocationBalanceRequest request =
        new UpdateAllocationBalanceRequest(
            new com.clearspend.capital.controller.type.Amount(Currency.USD, new BigDecimal("1")),
            "Changing");
    mvcHelper
        .query(
            "/businesses/%s/allocations/%s/transactions"
                .formatted(
                    createBusinessRecord.business().getBusinessId(),
                    createBusinessRecord.allocationRecord().allocation().getAllocationId()),
            HttpMethod.POST,
            createBusinessRecord.authCookie(),
            request)
        .andExpect(status().is(403));
  }

  @Test
  @SneakyThrows
  void getPlaidLogsForBusiness_WithPagination() {
    final PlaidTestData plaidTestData = setupPlaidLogs();
    final User customerServiceManager =
        testHelper
            .createUserWithGlobalRole(
                createBusinessRecord.business(), DefaultRoles.GLOBAL_CUSTOMER_SERVICE_MANAGER)
            .user();
    final JavaType pagedDataType =
        objectMapper
            .getTypeFactory()
            .constructParametricType(PagedData.class, PlaidLogEntryMetadata.class);
    final Cookie customerServiceManagerCookie = testHelper.login(customerServiceManager);
    final ThrowingFunction<PlaidLogEntryRequest, PagedData<PlaidLogEntryMetadata>> makeRequest =
        request -> {
          final String response =
              mvc.perform(
                      post("/businesses/%s/plaid/logs"
                              .formatted(
                                  createBusinessRecord
                                      .business()
                                      .getBusinessId()
                                      .toUuid()
                                      .toString()))
                          .contentType(MediaType.APPLICATION_JSON)
                          .content(objectMapper.writeValueAsString(request))
                          .cookie(customerServiceManagerCookie))
                  .andExpect(status().isOk())
                  .andReturn()
                  .getResponse()
                  .getContentAsString();
          return objectMapper.readValue(response, pagedDataType);
        };

    final PagedData<PlaidLogEntryMetadata> noQueryPage =
        makeRequest.apply(new PlaidLogEntryRequest());

    final List<PlaidLogEntryMetadata> noQueryExpectedContent =
        plaidTestData.orderedEntries().stream()
            .map(PlaidLogEntryWithData::plaidLogEntry)
            .map(PlaidLogEntryMetadata::fromPlaidLogEntry)
            .toList();

    assertThat(noQueryPage)
        .hasFieldOrPropertyWithValue("pageNumber", 0)
        .hasFieldOrPropertyWithValue("pageSize", 20)
        .hasFieldOrPropertyWithValue("totalElements", 6L)
        .extracting("content")
        .asList()
        .hasSize(6)
        .containsExactlyElementsOf(noQueryExpectedContent);

    final PagedData<PlaidLogEntryMetadata> page0 =
        makeRequest.apply(new PlaidLogEntryRequest(0, 3));
    final List<PlaidLogEntryMetadata> expectedPage0 =
        plaidTestData.orderedEntries().stream()
            .limit(3)
            .map(PlaidLogEntryWithData::plaidLogEntry)
            .map(PlaidLogEntryMetadata::fromPlaidLogEntry)
            .toList();

    assertThat(page0)
        .hasFieldOrPropertyWithValue("pageNumber", 0)
        .hasFieldOrPropertyWithValue("pageSize", 3)
        .hasFieldOrPropertyWithValue("totalElements", 6L)
        .extracting("content")
        .asList()
        .hasSize(3)
        .containsExactlyElementsOf(expectedPage0);

    final PagedData<PlaidLogEntryMetadata> page1 =
        makeRequest.apply(new PlaidLogEntryRequest(1, 3));
    final List<PlaidLogEntryMetadata> expectedPage1 =
        plaidTestData.orderedEntries().stream()
            .skip(3)
            .map(PlaidLogEntryWithData::plaidLogEntry)
            .map(PlaidLogEntryMetadata::fromPlaidLogEntry)
            .toList();

    assertThat(page1)
        .hasFieldOrPropertyWithValue("pageNumber", 1)
        .hasFieldOrPropertyWithValue("pageSize", 3)
        .hasFieldOrPropertyWithValue("totalElements", 6L)
        .extracting("content")
        .asList()
        .hasSize(3)
        .containsExactlyElementsOf(expectedPage1);
  }

  @Test
  @SneakyThrows
  void getPlaidLogDetails_DifferentMessageTypes() {
    final PlaidTestData plaidTestData = setupPlaidLogs();
    final User customerServiceManager =
        testHelper
            .createUserWithGlobalRole(
                createBusinessRecord.business(), DefaultRoles.GLOBAL_CUSTOMER_SERVICE_MANAGER)
            .user();
    final Cookie customerServiceManagerCookie = testHelper.login(customerServiceManager);
    final ThrowingFunction<
            Pair<TypedId<PlaidLogEntryId>, PlaidResponseType>, PlaidLogEntryDetails<?>>
        makeRequest =
            pair -> {
              final PlaidResponseType type = pair.getRight();
              final String idString = pair.getLeft().toUuid().toString();
              final String content =
                  mvc.perform(
                          get("/businesses/%s/plaid/logs/%s"
                                  .formatted(
                                      createBusinessRecord
                                          .business()
                                          .getBusinessId()
                                          .toUuid()
                                          .toString(),
                                      idString))
                              .cookie(customerServiceManagerCookie))
                      .andExpect(status().isOk())
                      .andReturn()
                      .getResponse()
                      .getContentAsString();
              final JavaType responseType =
                  objectMapper
                      .getTypeFactory()
                      .constructParametricType(
                          DefaultPlaidLogEntryDetails.class, type.getResponseClass());
              return objectMapper.readValue(content, responseType);
            };

    plaidTestData
        .entriesByType()
        .forEach(
            (plaidResponseType, plaidEntryWithData) -> {
              final PlaidLogEntryDetails<?> details =
                  ThrowableFunctions.sneakyThrows(makeRequest::apply)
                      .apply(
                          new ImmutablePair<>(
                              plaidEntryWithData.plaidLogEntry().getId(), plaidResponseType));
              try {
                assertThat(details)
                    .hasFieldOrPropertyWithValue("id", plaidEntryWithData.plaidLogEntry().getId())
                    .hasFieldOrPropertyWithValue(
                        "businessId", plaidEntryWithData.plaidLogEntry().getBusinessId())
                    .hasFieldOrPropertyWithValue(
                        "created", plaidEntryWithData.plaidLogEntry().getCreated())
                    .hasFieldOrPropertyWithValue(
                        "plaidResponseType",
                        plaidEntryWithData.plaidLogEntry().getPlaidResponseType())
                    .hasFieldOrPropertyWithValue("message", plaidEntryWithData.data());
              } catch (AssertionError ex) {
                throw new AssertionError(
                    "Error evaluating response for %s".formatted(plaidResponseType), ex);
              }
            });
  }

  @Test
  void getPlaidLogDetails_UserPermissions() {
    final PlaidTestData plaidTestData = setupPlaidLogs();
    final String plaidLogId =
        plaidTestData.orderedEntries().get(0).plaidLogEntry().getId().toUuid().toString();
    final ThrowingFunction<Cookie, ResultActions> action =
        cookie ->
            mvc.perform(
                get("/businesses/%s/plaid/logs/%s"
                        .formatted(
                            createBusinessRecord.business().getBusinessId().toUuid().toString(),
                            plaidLogId))
                    .cookie(cookie));
    permissionValidationHelper
        .buildValidator(createBusinessRecord)
        .allowGlobalRoles(DefaultRoles.GLOBAL_CUSTOMER_SERVICE_MANAGER)
        .build()
        .validateMockMvcCall(action);
  }

  private PlaidTestData setupPlaidLogs() {
    final AccountsGetResponse accountsGetResponse = new AccountsGetResponse();
    accountsGetResponse.setRequestId("AccountsGetRequest");
    final PlaidLogEntryWithData balanceLog =
        createAndSaveLog(PlaidResponseType.BALANCE, accountsGetResponse);

    final IdentityGetResponse identityGetResponse = new IdentityGetResponse();
    identityGetResponse.setRequestId("IdentityGetRequest");
    final PlaidLogEntryWithData ownerLog =
        createAndSaveLog(PlaidResponseType.OWNER, identityGetResponse);

    final AuthGetResponse authGetResponse = new AuthGetResponse();
    authGetResponse.setRequestId("AuthGetRequest");
    final PlaidLogEntryWithData accountLog =
        createAndSaveLog(PlaidResponseType.ACCOUNT, authGetResponse);

    final LinkTokenCreateResponse linkTokenCreateResponse = new LinkTokenCreateResponse();
    linkTokenCreateResponse.setRequestId("LinkTokenCreateRequest");
    final PlaidLogEntryWithData linkTokenLog =
        createAndSaveLog(PlaidResponseType.LINK_TOKEN, linkTokenCreateResponse);

    final ItemPublicTokenExchangeResponse itemPublicTokenExchangeResponse =
        new ItemPublicTokenExchangeResponse();
    itemPublicTokenExchangeResponse.setRequestId("ItemTokenPublicExchangeRequest");
    final PlaidLogEntryWithData accessTokenLog =
        createAndSaveLog(PlaidResponseType.ACCESS_TOKEN, itemPublicTokenExchangeResponse);

    final SandboxPublicTokenCreateResponse sandboxPublicTokenCreateResponse =
        new SandboxPublicTokenCreateResponse();
    sandboxPublicTokenCreateResponse.setRequestId("SandboxPublicTokenCreateRequest");
    final PlaidLogEntryWithData sandboxTokenLog =
        createAndSaveLog(PlaidResponseType.SANDBOX_LINK_TOKEN, sandboxPublicTokenCreateResponse);

    return new PlaidTestData(
        List.of(balanceLog, ownerLog, accountLog, linkTokenLog, accessTokenLog, sandboxTokenLog),
        Map.of(
            PlaidResponseType.BALANCE, balanceLog,
            PlaidResponseType.OWNER, ownerLog,
            PlaidResponseType.ACCOUNT, accountLog,
            PlaidResponseType.LINK_TOKEN, linkTokenLog,
            PlaidResponseType.ACCESS_TOKEN, accessTokenLog,
            PlaidResponseType.SANDBOX_LINK_TOKEN, sandboxTokenLog));
  }

  private record PlaidTestData(
      List<PlaidLogEntryWithData> orderedEntries,
      Map<PlaidResponseType, PlaidLogEntryWithData> entriesByType) {}

  private record PlaidLogEntryWithData(PlaidLogEntry plaidLogEntry, Object data) {}

  @SneakyThrows
  private PlaidLogEntryWithData createAndSaveLog(final PlaidResponseType type, final Object data) {
    final String message = objectMapper.writeValueAsString(data);
    final PlaidLogEntry entry =
        plaidLogEntryRepository.save(
            new PlaidLogEntry(
                createBusinessRecord.business().getId(), new EncryptedString(message), type));
    return new PlaidLogEntryWithData(entry, data);
  }

  @SneakyThrows
  @Test
  public void reallocateBusinessFunds_success() {
    AllocationRecord allocationRecord =
        testHelper.createAllocation(
            createBusinessRecord.business().getId(),
            testHelper.generateAllocationName(),
            createBusinessRecord.allocationRecord().allocation().getId());

    serviceHelper
        .accountService()
        .depositFunds(
            createBusinessRecord.business().getId(),
            createBusinessRecord.allocationRecord().account(),
            Amount.of(Currency.USD, new BigDecimal("1000")),
            false);

    // move $100 from root allocation (balance $1000) to newly created allocation (balance $0)
    BusinessReallocationRequest request =
        new BusinessReallocationRequest(
            createBusinessRecord.allocationRecord().allocation().getId(),
            allocationRecord.allocation().getId(),
            com.clearspend.capital.controller.type.Amount.of(
                new Amount(Currency.USD, BigDecimal.valueOf(100))));

    String body = objectMapper.writeValueAsString(request);

    MockHttpServletResponse mockHttpServletResponse =
        mvc.perform(
                post("/businesses/transactions")
                    .content(body)
                    .contentType(APPLICATION_JSON_VALUE)
                    .cookie(createBusinessRecord.authCookie()))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

    BusinessFundAllocationResponse response =
        objectMapper.readValue(
            mockHttpServletResponse.getContentAsString(), BusinessFundAllocationResponse.class);
    assertEquals(
        900.00,
        response.getLedgerBalanceFrom().getAmount().doubleValue(),
        "The businessLedgerBalance result is not as expected");
    assertEquals(
        100.00,
        response.getLedgerBalanceTo().getAmount().doubleValue(),
        "The allocationLedgerBalance result is not as expected");

    // move $90 from new allocation (balance $100) to root allocation (balance $900)
    request =
        new BusinessReallocationRequest(
            allocationRecord.allocation().getId(),
            createBusinessRecord.allocationRecord().allocation().getId(),
            com.clearspend.capital.controller.type.Amount.of(
                new Amount(Currency.USD, BigDecimal.valueOf(90))));

    body = objectMapper.writeValueAsString(request);

    mockHttpServletResponse =
        mvc.perform(
                post("/businesses/transactions")
                    .content(body)
                    .contentType(APPLICATION_JSON_VALUE)
                    .cookie(createBusinessRecord.authCookie()))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

    response =
        objectMapper.readValue(
            mockHttpServletResponse.getContentAsString(), BusinessFundAllocationResponse.class);
    assertEquals(
        10.00,
        response.getLedgerBalanceFrom().getAmount().doubleValue(),
        "The businessLedgerBalance result is not as expected");
    assertEquals(
        990.00,
        response.getLedgerBalanceTo().getAmount().doubleValue(),
        "The allocationLedgerBalance result is not as expected");
  }

  @SneakyThrows
  @Test
  public void getBusinessAllocations_success() {
    CreateBusinessRecord businessRecord = testHelper.createBusiness(100L);
    testHelper.setCurrentUser(businessRecord.user());
    Allocation rootAllocation = businessRecord.allocationRecord().allocation();
    AllocationRecord allocationChild1 =
        testHelper.createAllocation(
            businessRecord.business().getId(), "child_1", rootAllocation.getId());
    AllocationRecord allocationGrandchild1 =
        testHelper.createAllocation(
            businessRecord.business().getId(),
            "grandchild_1",
            allocationChild1.allocation().getId());
    AllocationRecord allocationGrandchild2 =
        testHelper.createAllocation(
            businessRecord.business().getId(),
            "grandchild_2",
            allocationChild1.allocation().getId());

    serviceHelper
        .accountService()
        .depositFunds(
            businessRecord.business().getId(),
            serviceHelper
                .allocationService()
                .getRootAllocation(businessRecord.business().getId())
                .account(),
            Amount.of(Currency.USD, new BigDecimal(200)),
            true);

    MockHttpServletResponse response =
        mvc.perform(
                get("/businesses/allocations")
                    .header("businessId", businessRecord.business().getId())
                    .contentType(APPLICATION_JSON_VALUE)
                    .cookie(businessRecord.authCookie()))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

    List<com.clearspend.capital.controller.type.allocation.Allocation> allocations =
        objectMapper.readValue(response.getContentAsString(), new TypeReference<>() {});

    assertThat(allocations)
        .extracting("name")
        .containsExactlyInAnyOrder(
            rootAllocation.getName(),
            allocationChild1.allocation().getName(),
            allocationGrandchild1.allocation().getName(),
            allocationGrandchild2.allocation().getName());

    Map<TypedId<AllocationId>, com.clearspend.capital.controller.type.allocation.Allocation>
        allocationMap =
            allocations.stream()
                .collect(
                    Collectors.toMap(
                        com.clearspend.capital.controller.type.allocation.Allocation
                            ::getAllocationId,
                        Function.identity()));

    // checking tree structure
    // root node
    assertThat(allocationMap.get(rootAllocation.getId()).getParentAllocationId()).isNull();
    assertThat(allocationMap.get(rootAllocation.getId()).getChildrenAllocationIds())
        .containsExactly(allocationChild1.allocation().getId());
    assertThat(
            allocationMap.get(rootAllocation.getId()).getAccount().getLedgerBalance().getAmount())
        .isEqualTo(new BigDecimal("300.00"));
    assertThat(
            allocationMap
                .get(rootAllocation.getId())
                .getAccount()
                .getAvailableBalance()
                .getAmount())
        .isEqualTo(new BigDecimal("100.00"));

    // child 1
    assertThat(allocationMap.get(allocationChild1.allocation().getId()).getParentAllocationId())
        .isEqualTo(rootAllocation.getId());
    assertThat(allocationMap.get(allocationChild1.allocation().getId()).getChildrenAllocationIds())
        .containsExactlyInAnyOrder(
            allocationGrandchild1.allocation().getId(), allocationGrandchild2.allocation().getId());

    // grandchild 1
    assertThat(
            allocationMap.get(allocationGrandchild1.allocation().getId()).getParentAllocationId())
        .isEqualTo(allocationChild1.allocation().getId());
    assertThat(
            allocationMap
                .get(allocationGrandchild1.allocation().getId())
                .getChildrenAllocationIds())
        .isEmpty();

    // grandchild 2
    assertThat(
            allocationMap.get(allocationGrandchild2.allocation().getId()).getParentAllocationId())
        .isEqualTo(allocationChild1.allocation().getId());
    assertThat(
            allocationMap
                .get(allocationGrandchild2.allocation().getId())
                .getChildrenAllocationIds())
        .isEmpty();

    log.info(String.valueOf(allocations));
  }

  @SneakyThrows
  @Test
  public void getRootAllocation_ForUnknownBusinessId_expectStatus404() {
    Business business = createBusinessRecord.business();

    testHelper.deleteBusinessOwner(createBusinessRecord.businessOwner().getId());
    testHelper.deleteAllocation(business.getId());
    testHelper.deleteAccount(business.getId());
    testHelper.deleteUser(userRepository.findByBusinessId(business.getId()).get(0).getId());
    testHelper.deleteBusinessLimit(business.getId());
    testHelper.deleteSpendLimit(business.getId());
    testHelper.deleteBusinessBankAccount(business.getId());
    testHelper.deleteBusiness(business.getId());

    mvc.perform(
            get("/businesses/allocations")
                .contentType(APPLICATION_JSON_VALUE)
                .cookie(createBusinessRecord.authCookie()))
        .andExpect(status().isNotFound())
        .andReturn()
        .getResponse();
  }

  @SneakyThrows
  @Test
  public void searchBusinessAllocation_success() {
    SearchBusinessAllocationRequest request =
        new SearchBusinessAllocationRequest(
            createBusinessRecord.allocationRecord().allocation().getName());

    String body = objectMapper.writeValueAsString(request);

    MockHttpServletResponse response =
        mvc.perform(
                post("/businesses/allocations")
                    .header("businessId", createBusinessRecord.business().getId())
                    .content(body)
                    .contentType(APPLICATION_JSON_VALUE)
                    .cookie(createBusinessRecord.authCookie()))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

    List<com.clearspend.capital.controller.type.allocation.Allocation> responseAllocationList =
        objectMapper.readValue(response.getContentAsString(), new TypeReference<>() {});
    assertEquals(1, responseAllocationList.size(), "The expected result is not ok");
  }

  @SneakyThrows
  @Test
  void getBusinessAccountWithFetchHoldTrueWhenAmountIsAddedOnHold() {
    Business business = createBusinessRecord.business();
    serviceHelper
        .accountService()
        .depositFunds(
            business.getId(),
            serviceHelper.allocationService().getRootAllocation(business.getId()).account(),
            Amount.of(Currency.USD, new BigDecimal(200)),
            true);
    MockHttpServletResponse response =
        mvc.perform(get("/businesses/accounts").contentType("application/json").cookie(authCookie))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
    log.info(response.getContentAsString());
    Account account = objectMapper.readValue(response.getContentAsString(), Account.class);
    assertThat(
            com.clearspend.capital.controller.type.Amount.of(
                Amount.of(Currency.USD, BigDecimal.valueOf(200))))
        .isEqualTo(account.getLedgerBalance());
  }

  @Test
  void defaultBusinessLimit() {
    testHelper.setIssuedPhysicalCardsLimit(createBusinessRecord.business().getId(), 10);
    BusinessSettings businessSettings =
        mvcHelper.queryObject(
            "/businesses/business-settings", HttpMethod.GET, authCookie, BusinessSettings.class);

    assertThat(businessSettings.getIssuedPhysicalCardsLimit()).isEqualTo(10);
    assertThat(businessSettings.getIssuedPhysicalCardsTotal()).isEqualTo(0);
  }

  @Test
  void getBusinessLimit_UserPermissions() {
    final Allocation allocation =
        testHelper
            .createAllocation(
                createBusinessRecord.business().getId(),
                "Child",
                createBusinessRecord.allocationRecord().allocation().getId())
            .allocation();
    final ThrowingFunction<Cookie, ResultActions> action =
        cookie -> mvc.perform(get("/businesses/business-settings").cookie(cookie));
    permissionValidationHelper
        .buildValidator(createBusinessRecord)
        .setAllocation(allocation)
        .allowRolesOnRootAllocation(
            Set.of(
                DefaultRoles.ALLOCATION_ADMIN,
                DefaultRoles.ALLOCATION_MANAGER,
                DefaultRoles.ALLOCATION_VIEW_ONLY))
        .build()
        .validateMockMvcCall(action);
  }

  @Test
  void updateBusinessLimits() {
    testHelper.setIssuedPhysicalCardsLimit(createBusinessRecord.business().getId(), 10);
    BusinessSettings businessSettings =
        new BusinessSettings(
            null,
            Set.of(
                new BusinessSettings.LimitOperationRecord(
                    Currency.USD,
                    Set.of(
                        new BusinessSettings.BusinessLimitOperationRecord(
                            LimitType.ACH_DEPOSIT,
                            Set.of(
                                new BusinessSettings.LimitPeriodOperationRecord(
                                    LimitPeriod.DAILY, 1)))))),
            110,
            1,
            BigDecimal.valueOf(5.0),
            AchFundsAvailabilityMode.FAST,
            BigDecimal.TEN);

    BusinessSettings businessSettingsResponse =
        mvcHelper.queryObject(
            "/businesses/business-settings",
            HttpMethod.PATCH,
            authCookie,
            businessSettings,
            BusinessSettings.class);

    Integer dailyAchDepositOperations =
        businessSettingsResponse.getOperationLimits().stream()
            .filter(limitOperationRecord -> limitOperationRecord.currency() == Currency.USD)
            .findAny()
            .get()
            .businessLimitOperations()
            .stream()
            .filter(
                businessLimitOperationRecord ->
                    businessLimitOperationRecord.businessLimitType() == LimitType.ACH_DEPOSIT)
            .findAny()
            .get()
            .limitOperationPeriods()
            .stream()
            .filter(
                limitPeriodOperationRecord ->
                    limitPeriodOperationRecord.period() == LimitPeriod.DAILY)
            .findAny()
            .get()
            .value();

    Integer monthlyAchDepositOperations =
        businessSettingsResponse.getOperationLimits().stream()
            .filter(limitOperationRecord -> limitOperationRecord.currency() == Currency.USD)
            .findAny()
            .get()
            .businessLimitOperations()
            .stream()
            .filter(
                businessLimitOperationRecord ->
                    businessLimitOperationRecord.businessLimitType() == LimitType.ACH_DEPOSIT)
            .findAny()
            .get()
            .limitOperationPeriods()
            .stream()
            .filter(
                limitPeriodOperationRecord ->
                    limitPeriodOperationRecord.period() == LimitPeriod.MONTHLY)
            .findAny()
            .get()
            .value();

    assertThat(dailyAchDepositOperations).isEqualTo(1);
    assertThat(monthlyAchDepositOperations).isEqualTo(6);
    assertThat(businessSettingsResponse.getIssuedPhysicalCardsLimit()).isEqualTo(110);
    assertThat(businessSettingsResponse.getIssuedPhysicalCardsTotal()).isEqualTo(0);
    assertThat(businessSettingsResponse.getForeignTransactionFeePercents())
        .isEqualTo(BigDecimal.valueOf(5.0));
    assertThat(businessSettingsResponse.getAchFundsAvailabilityMode())
        .isEqualTo(AchFundsAvailabilityMode.FAST);
    assertThat(businessSettingsResponse.getImmediateAchFundsLimit()).isEqualTo(BigDecimal.TEN);
  }

  @Test
  void deleteBusinessLimits() {
    testHelper.setIssuedPhysicalCardsLimit(createBusinessRecord.business().getId(), 10);
    BusinessSettings businessSettings =
        new BusinessSettings(
            null,
            Set.of(
                new BusinessSettings.LimitOperationRecord(
                    Currency.USD,
                    Set.of(
                        new BusinessSettings.BusinessLimitOperationRecord(
                            LimitType.ACH_DEPOSIT, null)))),
            110,
            1,
            BigDecimal.TEN,
            AchFundsAvailabilityMode.STANDARD,
            BigDecimal.TEN);

    BusinessSettings businessSettingsResponse =
        mvcHelper.queryObject(
            "/businesses/business-settings",
            HttpMethod.PATCH,
            authCookie,
            businessSettings,
            BusinessSettings.class);

    Optional<BusinessLimitOperationRecord> dailyAchDepositOperations =
        businessSettingsResponse.getOperationLimits().stream()
            .filter(limitOperationRecord -> limitOperationRecord.currency() == Currency.USD)
            .findAny()
            .get()
            .businessLimitOperations()
            .stream()
            .filter(
                businessLimitOperationRecord ->
                    businessLimitOperationRecord.businessLimitType() == LimitType.ACH_DEPOSIT)
            .findAny();

    assertThat(dailyAchDepositOperations).isEmpty();
    assertThat(businessSettingsResponse.getIssuedPhysicalCardsLimit()).isEqualTo(110);
    assertThat(businessSettingsResponse.getIssuedPhysicalCardsTotal()).isEqualTo(0);
    assertThat(businessSettingsResponse.getForeignTransactionFeePercents())
        .isEqualTo(BigDecimal.TEN);
    assertThat(businessSettingsResponse.getAchFundsAvailabilityMode())
        .isEqualTo(AchFundsAvailabilityMode.STANDARD);
    assertThat(businessSettingsResponse.getImmediateAchFundsLimit()).isEqualTo(BigDecimal.TEN);
  }

  @Test
  void addBusinessLimits() {
    testHelper.setIssuedPhysicalCardsLimit(createBusinessRecord.business().getId(), 10);
    BusinessSettings businessSettings =
        new BusinessSettings(
            null,
            Set.of(
                new BusinessSettings.LimitOperationRecord(
                    Currency.AMD,
                    Set.of(
                        new BusinessSettings.BusinessLimitOperationRecord(
                            LimitType.ACH_DEPOSIT,
                            Set.of(
                                new BusinessSettings.LimitPeriodOperationRecord(
                                    LimitPeriod.DAILY, 111)))))),
            110,
            1,
            BigDecimal.TEN,
            AchFundsAvailabilityMode.IMMEDIATE,
            BigDecimal.ONE);

    BusinessSettings businessSettingsResponse =
        mvcHelper.queryObject(
            "/businesses/business-settings",
            HttpMethod.PATCH,
            authCookie,
            businessSettings,
            BusinessSettings.class);

    Integer dailyAchDepositOperations =
        businessSettingsResponse.getOperationLimits().stream()
            .filter(limitOperationRecord -> limitOperationRecord.currency() == Currency.AMD)
            .findAny()
            .get()
            .businessLimitOperations()
            .stream()
            .filter(
                businessLimitOperationRecord ->
                    businessLimitOperationRecord.businessLimitType() == LimitType.ACH_DEPOSIT)
            .findAny()
            .get()
            .limitOperationPeriods()
            .stream()
            .filter(
                limitPeriodOperationRecord ->
                    limitPeriodOperationRecord.period() == LimitPeriod.DAILY)
            .findAny()
            .get()
            .value();

    Integer monthlyAchDepositOperations =
        businessSettingsResponse.getOperationLimits().stream()
            .filter(limitOperationRecord -> limitOperationRecord.currency() == Currency.USD)
            .findAny()
            .get()
            .businessLimitOperations()
            .stream()
            .filter(
                businessLimitOperationRecord ->
                    businessLimitOperationRecord.businessLimitType() == LimitType.ACH_DEPOSIT)
            .findAny()
            .get()
            .limitOperationPeriods()
            .stream()
            .filter(
                limitPeriodOperationRecord ->
                    limitPeriodOperationRecord.period() == LimitPeriod.MONTHLY)
            .findAny()
            .get()
            .value();

    assertThat(dailyAchDepositOperations).isEqualTo(111);
    assertThat(monthlyAchDepositOperations).isEqualTo(6);
    assertThat(businessSettingsResponse.getIssuedPhysicalCardsLimit()).isEqualTo(110);
    assertThat(businessSettingsResponse.getIssuedPhysicalCardsTotal()).isEqualTo(0);
    assertThat(businessSettingsResponse.getForeignTransactionFeePercents())
        .isEqualTo(BigDecimal.TEN);
    assertThat(businessSettingsResponse.getAchFundsAvailabilityMode())
        .isEqualTo(AchFundsAvailabilityMode.IMMEDIATE);
    assertThat(businessSettingsResponse.getImmediateAchFundsLimit()).isEqualTo(BigDecimal.ONE);
  }

  @SneakyThrows
  @Test
  void addDuplicatesIntoBusinessLimits() {
    testHelper.setIssuedPhysicalCardsLimit(createBusinessRecord.business().getId(), 10);
    BusinessSettings businessSettings =
        new BusinessSettings(
            null,
            Set.of(
                new BusinessSettings.LimitOperationRecord(
                    Currency.AMD,
                    Set.of(
                        new BusinessSettings.BusinessLimitOperationRecord(
                            LimitType.ACH_DEPOSIT,
                            Set.of(
                                new BusinessSettings.LimitPeriodOperationRecord(
                                    LimitPeriod.DAILY, 111))))),
                new BusinessSettings.LimitOperationRecord(
                    Currency.AMD,
                    Set.of(
                        new BusinessSettings.BusinessLimitOperationRecord(
                            LimitType.ACH_DEPOSIT,
                            Set.of(
                                new BusinessSettings.LimitPeriodOperationRecord(
                                    LimitPeriod.DAILY, 222)))))),
            110,
            1,
            BigDecimal.TEN,
            AchFundsAvailabilityMode.STANDARD,
            BigDecimal.ONE);

    mvc.perform(
            patch("/businesses/business-settings")
                .content(objectMapper.writeValueAsString(businessSettings))
                .contentType(APPLICATION_JSON_VALUE)
                .cookie(authCookie))
        .andExpect(status().is4xxClientError());
  }

  @Test
  @SneakyThrows
  void completeOnboarding() {
    businessService.updateBusinessForOnboarding(
        createBusinessRecord.business().getId(),
        BusinessStatus.ONBOARDING,
        BusinessOnboardingStep.LINK_ACCOUNT,
        KnowYourBusinessStatus.PASS);

    mvc.perform(
            post("/businesses/complete-onboarding")
                .contentType("application/json")
                .cookie(authCookie))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse();

    Business business =
        serviceHelper
            .businessService()
            .getBusiness(createBusinessRecord.business().getId())
            .business();
    assertThat(business.getOnboardingStep()).isEqualTo(BusinessOnboardingStep.COMPLETE);
    assertThat(business.getStatus()).isEqualTo(BusinessStatus.ACTIVE);
    assertThat(business.getFormationDate()).isBefore(OffsetDateTime.now(Clock.systemUTC()));
  }

  @Test
  @SneakyThrows
  void updateBusiness() {
    final String cardholderRef = UUID.randomUUID().toString().substring(0, 30);
    createBusinessRecord.business().setCardholderExternalRef(cardholderRef);
    businessRepository.saveAndFlush(createBusinessRecord.business());
    final Address address =
        new Address("Line 1", "Line 2", "AFG", "Washington", "123456", Country.AFG);

    final UpdateBusiness updateBusiness = new UpdateBusiness();
    updateBusiness.setBusinessType(BusinessType.INDIVIDUAL);
    updateBusiness.setLegalName("My legal name");
    updateBusiness.setBusinessPhone("+123213121");
    updateBusiness.setBusinessName("My Business Name");
    updateBusiness.setEmployerIdentificationNumber("887721121");
    updateBusiness.setDescription("My Description");
    updateBusiness.setUrl("My URL");
    updateBusiness.setMcc("8241");
    updateBusiness.setAddress(address);
    updateBusiness.setTimeZone(TimeZone.US_MOUNTAIN);

    assertNull(stripeMockClient.getCreatedObject(cardholderRef));

    mvc.perform(
            post("/businesses/update")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(updateBusiness))
                .cookie(authCookie))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse();

    final Business businessAfterUpdate =
        serviceHelper
            .businessService()
            .getBusiness(createBusinessRecord.business().getId())
            .business();

    assertThat(businessAfterUpdate.getBusinessName()).isEqualTo(updateBusiness.getBusinessName());
    assertThat(businessAfterUpdate.getBusinessPhone().getEncrypted())
        .isEqualTo(updateBusiness.getBusinessPhone());
    assertThat(businessAfterUpdate.getDescription()).isEqualTo(updateBusiness.getDescription());
    assertThat(businessAfterUpdate.getEmployerIdentificationNumber())
        .isEqualTo(updateBusiness.getEmployerIdentificationNumber());
    assertThat(businessAfterUpdate.getLegalName()).isEqualTo(updateBusiness.getLegalName());
    assertThat(businessAfterUpdate.getMcc()).isEqualTo(updateBusiness.getMcc());
    assertThat(businessAfterUpdate.getTimeZone()).isEqualTo(updateBusiness.getTimeZone());
    assertThat(new Address(businessAfterUpdate.getClearAddress())).isEqualTo(address);

    assertNotNull(stripeMockClient.getCreatedObject(cardholderRef));
  }

  @Test
  @SneakyThrows
  void suspendBusiness() {
    final String cardholderRef = UUID.randomUUID().toString().substring(0, 30);
    createBusinessRecord.business().setCardholderExternalRef(cardholderRef);
    businessRepository.saveAndFlush(createBusinessRecord.business());

    final User customerServiceManager =
        testHelper
            .createUserWithGlobalRole(
                createBusinessRecord.business(), DefaultRoles.GLOBAL_CUSTOMER_SERVICE_MANAGER)
            .user();
    final Cookie cookie = testHelper.login(customerServiceManager);

    mvc.perform(
            post("/businesses/%s/suspend".formatted(createBusinessRecord.business().getId()))
                .cookie(cookie))
        .andExpect(status().isOk());

    final Business dbBusiness =
        businessRepository.findById(createBusinessRecord.business().getId()).orElseThrow();
    assertThat(dbBusiness).hasFieldOrPropertyWithValue("status", BusinessStatus.SUSPENDED);

    final Cardholder cardholder = (Cardholder) stripeMockClient.getCreatedObject(cardholderRef);
    assertThat(cardholder)
        .isNotNull()
        .hasFieldOrPropertyWithValue("status", Status.INACTIVE.getValue());
  }

  @Test
  @SneakyThrows
  void restoreBusiness() {
    final Business newBusiness = testHelper.createBusiness().business();

    final String cardholderRef = UUID.randomUUID().toString().substring(0, 30);
    newBusiness.setCardholderExternalRef(cardholderRef);
    newBusiness.setStatus(BusinessStatus.CLOSED);
    businessRepository.saveAndFlush(newBusiness);

    final User customerServiceManager =
        testHelper
            .createUserWithGlobalRole(
                createBusinessRecord.business(), DefaultRoles.GLOBAL_CUSTOMER_SERVICE_MANAGER)
            .user();
    final Cookie cookie = testHelper.login(customerServiceManager);

    mvc.perform(post("/businesses/%s/restore".formatted(newBusiness.getId())).cookie(cookie))
        .andExpect(status().isOk());

    final Business dbBusiness = businessRepository.findById(newBusiness.getId()).orElseThrow();
    assertThat(dbBusiness).hasFieldOrPropertyWithValue("status", BusinessStatus.ACTIVE);

    final Cardholder cardholder = (Cardholder) stripeMockClient.getCreatedObject(cardholderRef);
    assertThat(cardholder)
        .isNotNull()
        .hasFieldOrPropertyWithValue("status", Status.ACTIVE.getValue());
  }
}
