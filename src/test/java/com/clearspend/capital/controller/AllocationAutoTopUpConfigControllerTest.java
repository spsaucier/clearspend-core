package com.clearspend.capital.controller;

import static com.clearspend.capital.service.type.JobContextPropertyName.ALLOCATION_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.TestHelper.CreateBusinessRecord;
import com.clearspend.capital.controller.type.Amount;
import com.clearspend.capital.controller.type.allocation.AllocationAutoTopUpConfigCreateRequest;
import com.clearspend.capital.controller.type.allocation.AllocationAutoTopUpConfigResponse;
import com.clearspend.capital.controller.type.allocation.AllocationAutoTopUpConfigUpdateRequest;
import com.clearspend.capital.data.model.Allocation;
import com.clearspend.capital.data.model.enums.BatchSummaryType;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.data.repository.JobConfigRepository;
import com.clearspend.capital.service.AllocationService.AllocationRecord;
import com.fasterxml.jackson.core.type.TypeReference;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import javax.servlet.http.Cookie;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jobrunr.jobs.RecurringJob;
import org.jobrunr.storage.StorageProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@RequiredArgsConstructor(onConstructor = @__({@Autowired}))
@Slf4j
class AllocationAutoTopUpConfigControllerTest extends BaseCapitalTest {

  private final MockMvc mvc;
  private final TestHelper testHelper;
  private final JobConfigRepository allocationAutoTopUpRepository;
  private CreateBusinessRecord createBusinessRecord;

  @Autowired private final StorageProvider storageProvider;

  @BeforeEach
  void init() {
    createBusinessRecord = testHelper.init();
  }

  @SneakyThrows
  @ParameterizedTest
  @ValueSource(ints = {4, 25})
  void createAllocationAutoTopUp_success(int dayOfMonth) {
    final Allocation rootAllocation = createBusinessRecord.allocationRecord().allocation();

    AllocationAutoTopUpConfigCreateRequest request =
        new AllocationAutoTopUpConfigCreateRequest(
            dayOfMonth,
            Amount.of(
                com.clearspend.capital.common.data.model.Amount.of(Currency.USD, BigDecimal.TEN)));

    String body = objectMapper.writeValueAsString(request);

    MockHttpServletResponse response =
        mvc.perform(
                post(String.format("/allocation/%s/auto-top-up", rootAllocation.getId()))
                    .contentType("application/json")
                    .content(body)
                    .cookie(createBusinessRecord.authCookie()))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

    testHelper.flush();
    AllocationAutoTopUpConfigResponse autoTopUpConfigResponse =
        objectMapper.readValue(
            response.getContentAsString(), AllocationAutoTopUpConfigResponse.class);
    assertThat(autoTopUpConfigResponse.isActive()).isTrue();
    assertThat(autoTopUpConfigResponse.getMonthlyDay()).isEqualTo(dayOfMonth);

    RecurringJob recurringJob = getRecurringJobs(rootAllocation).get(0);
    Instant expectedInstant = calculateExpectedNextExecutionTime(dayOfMonth);
    assertThat(recurringJob.getNextRun()).isEqualTo(expectedInstant);
  }

  @SneakyThrows
  @Test
  void createAllocationAutoTopUp_invalidDayOfMonth() {
    final Allocation rootAllocation = createBusinessRecord.allocationRecord().allocation();

    AllocationAutoTopUpConfigCreateRequest request =
        new AllocationAutoTopUpConfigCreateRequest(
            35,
            Amount.of(
                com.clearspend.capital.common.data.model.Amount.of(Currency.USD, BigDecimal.TEN)));

    String body = objectMapper.writeValueAsString(request);

    MvcResult mvcResult =
        mvc.perform(
                post(String.format("/allocation/%s/auto-top-up", rootAllocation.getId()))
                    .contentType("application/json")
                    .content(body)
                    .cookie(createBusinessRecord.authCookie()))
            .andExpect(status().isBadRequest())
            .andReturn();

    Assertions.assertTrue(
        mvcResult
            .getResponse()
            .getContentAsString()
            .contains("Last day of the month to do auto top-up will be 28"));
  }

  @SneakyThrows
  @ParameterizedTest
  @ValueSource(ints = {4, 25})
  void updateAllocationAutoTopUp_success(int dayOfMonth) {
    final Allocation rootAllocation = createBusinessRecord.allocationRecord().allocation();

    AllocationAutoTopUpConfigCreateRequest request =
        new AllocationAutoTopUpConfigCreateRequest(
            dayOfMonth,
            Amount.of(
                com.clearspend.capital.common.data.model.Amount.of(Currency.USD, BigDecimal.TEN)));

    String body = objectMapper.writeValueAsString(request);

    MockHttpServletResponse response =
        mvc.perform(
                post(String.format("/allocation/%s/auto-top-up", rootAllocation.getId()))
                    .contentType("application/json")
                    .content(body)
                    .cookie(createBusinessRecord.authCookie()))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

    AllocationAutoTopUpConfigResponse autoTopUpConfigCreateResponse =
        objectMapper.readValue(
            response.getContentAsString(), AllocationAutoTopUpConfigResponse.class);

    AllocationAutoTopUpConfigUpdateRequest requestUpdate =
        new AllocationAutoTopUpConfigUpdateRequest(
            autoTopUpConfigCreateResponse.getId(),
            dayOfMonth,
            Amount.of(
                com.clearspend.capital.common.data.model.Amount.of(Currency.USD, BigDecimal.ONE)),
            true);

    String bodyUpdate = objectMapper.writeValueAsString(requestUpdate);

    MockHttpServletResponse responseUpdate =
        mvc.perform(
                patch(String.format("/allocation/%s/auto-top-up", rootAllocation.getId()))
                    .contentType("application/json")
                    .content(bodyUpdate)
                    .cookie(createBusinessRecord.authCookie()))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

    AllocationAutoTopUpConfigResponse autoTopUpConfigResponse =
        objectMapper.readValue(
            responseUpdate.getContentAsString(), AllocationAutoTopUpConfigResponse.class);
    assertThat(autoTopUpConfigResponse.isActive()).isTrue();
    assertThat(autoTopUpConfigResponse.getMonthlyDay()).isEqualTo(dayOfMonth);
    assertThat(autoTopUpConfigResponse.getAmount().getAmount().longValue()).isEqualTo(1L);

    RecurringJob recurringJob = getRecurringJobs(rootAllocation).get(0);
    assertThat(recurringJob.getId())
        .isEqualTo(String.format("%s_%s", BatchSummaryType.AUTO_TOP_UP, rootAllocation.getId()));
    Instant expectedInstant = calculateExpectedNextExecutionTime(dayOfMonth);
    assertThat(recurringJob.getNextRun()).isEqualTo(expectedInstant);
  }

  @SneakyThrows
  @Test
  void getAllocationAutoTopUp_success() {
    AllocationRecord allocationRecord = createBusinessRecord.allocationRecord();
    Cookie authCookie = createBusinessRecord.authCookie();

    AllocationAutoTopUpConfigCreateRequest request =
        new AllocationAutoTopUpConfigCreateRequest(
            4,
            Amount.of(
                com.clearspend.capital.common.data.model.Amount.of(Currency.USD, BigDecimal.TEN)));

    String body = objectMapper.writeValueAsString(request);

    mvc.perform(
            post(String.format(
                    "/allocation/%s/auto-top-up", allocationRecord.allocation().getAllocationId()))
                .contentType("application/json")
                .content(body)
                .cookie(createBusinessRecord.authCookie()))
        .andExpect(status().isOk());

    MockHttpServletResponse response =
        mvc.perform(
                get(String.format(
                        "/allocation/%s/auto-top-up", allocationRecord.allocation().getId()))
                    .contentType("application/json")
                    .cookie(authCookie))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
    log.info(response.getContentAsString());
    List<AllocationAutoTopUpConfigResponse> actual =
        objectMapper.readValue(response.getContentAsString(), new TypeReference<>() {});
    assertThat(actual).hasSize(1);
    assertThat(actual.get(0).isActive()).isTrue();
  }

  @SneakyThrows
  @Test
  void deactivateAllocationAutoTopUp_success() {
    AllocationRecord allocationRecord = createBusinessRecord.allocationRecord();

    AllocationAutoTopUpConfigCreateRequest request =
        new AllocationAutoTopUpConfigCreateRequest(
            4,
            Amount.of(
                com.clearspend.capital.common.data.model.Amount.of(Currency.USD, BigDecimal.TEN)));

    String body = objectMapper.writeValueAsString(request);

    MockHttpServletResponse allocationAutoTopUpSaveResponse =
        mvc.perform(
                post(String.format(
                        "/allocation/%s/auto-top-up",
                        allocationRecord.allocation().getAllocationId()))
                    .contentType("application/json")
                    .content(body)
                    .cookie(createBusinessRecord.authCookie()))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

    AllocationAutoTopUpConfigResponse allocationAutoTopUpConfigResponse =
        objectMapper.readValue(
            allocationAutoTopUpSaveResponse.getContentAsString(), new TypeReference<>() {});

    MockHttpServletResponse response =
        mvc.perform(
                delete(
                        String.format(
                            "/allocation/auto-top-up/%s",
                            allocationAutoTopUpConfigResponse.getId()))
                    .contentType("application/json")
                    .cookie(createBusinessRecord.authCookie()))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

    assertTrue(objectMapper.readValue(response.getContentAsString(), Boolean.class));
    assertThat(
            allocationAutoTopUpRepository
                .findByBusinessId(allocationRecord.allocation().getBusinessId())
                .stream()
                .filter(
                    jobConfig ->
                        jobConfig
                            .getJobContext()
                            .get(ALLOCATION_ID)
                            .equals(allocationRecord.allocation().getId()))
                .toList())
        .isEmpty();
    testHelper.flush();
    assertThat(getRecurringJobs(allocationRecord.allocation())).isEmpty();
  }

  @NonNull
  private List<RecurringJob> getRecurringJobs(Allocation allocationRecord) {
    return storageProvider.getRecurringJobs().stream()
        .filter(
            job -> {
              String jobId =
                  String.format("%s_%s", BatchSummaryType.AUTO_TOP_UP, allocationRecord.getId());
              return job.getId().equals(jobId);
            })
        .toList();
  }

  private Instant calculateExpectedNextExecutionTime(int dayOfMonth) {
    LocalDate localDate = LocalDate.now().withDayOfMonth(dayOfMonth);
    if (LocalDate.now().getDayOfMonth() > dayOfMonth) {
      localDate = localDate.plusMonths(1);
    }

    return localDate.atStartOfDay().toInstant(ZoneOffset.UTC);
  }
}
