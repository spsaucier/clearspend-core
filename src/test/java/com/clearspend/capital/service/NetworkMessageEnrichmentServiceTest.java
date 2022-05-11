package com.clearspend.capital.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.clearspend.capital.client.clearbit.ClearbitClient;
import com.clearspend.capital.client.mx.MxClient;
import com.clearspend.capital.client.mx.types.EnhanceTransactionResponse;
import com.clearspend.capital.client.mx.types.TransactionRecordResponse;
import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.common.data.model.ClearAddress;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.data.model.AccountActivity;
import com.clearspend.capital.data.model.business.Business;
import com.clearspend.capital.data.model.embedded.MerchantDetails;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.data.model.enums.MerchantType;
import com.clearspend.capital.data.model.enums.network.NetworkMessageType;
import com.clearspend.capital.service.type.NetworkCommon;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class NetworkMessageEnrichmentServiceTest {

  private AccountActivityService mockAccountActivityService;
  private MxClient mockMxClient;
  private ClearbitClient mockClearbitClient;
  private NetworkMessageEnrichmentService underTest;

  @BeforeEach
  public void setup() {
    mockAccountActivityService = Mockito.mock(AccountActivityService.class);
    mockMxClient = Mockito.mock(MxClient.class);
    mockClearbitClient = Mockito.mock(ClearbitClient.class);

    // The final boolean parameter is a switch to allow us to revert back to Clearbit for logo urls.
    // If we decide to keep this functionality clean this up using polymorphism
    underTest =
        new NetworkMessageEnrichmentService(
            mockAccountActivityService, mockMxClient, mockClearbitClient, true);
  }

  @Test
  @SneakyThrows
  public void scheduleActivityEnrichment_whenMxReturnsEmptyTransactionList_noEnrichmentHappens() {
    when(mockMxClient.getCleansedMerchantName(anyString(), anyInt()))
        .thenReturn(new EnhanceTransactionResponse(Collections.emptyList()));

    underTest.scheduleActivityEnrichment(buildCommon("test", 123));

    verify(mockMxClient, times(1)).getCleansedMerchantName(eq("test"), eq(123));
    verifyNoInteractions(mockAccountActivityService);
    verifyNoInteractions(mockClearbitClient);
  }

  @Test
  @SneakyThrows
  public void scheduleActivityEnrichment_whenMxReturnsNullList_noEnrichmentHappens() {
    when(mockMxClient.getCleansedMerchantName(anyString(), anyInt()))
        .thenReturn(new EnhanceTransactionResponse(null));

    underTest.scheduleActivityEnrichment(buildCommon("test", 123));

    verify(mockMxClient, times(1)).getCleansedMerchantName(eq("test"), eq(123));
    verifyNoInteractions(mockAccountActivityService);
    verifyNoInteractions(mockClearbitClient);
  }

  @Test
  @SneakyThrows
  public void scheduleActivityEnrichment_whenValidMxResultUsingMxLogos_mxIsQueriedAgainForLogo() {
    when(mockMxClient.getCleansedMerchantName(anyString(), anyInt()))
        .thenReturn(
            new EnhanceTransactionResponse(
                List.of(new TransactionRecordResponse("testing", "enh-name", "guid"))));

    when(mockMxClient.getMerchantLogo(eq("guid"))).thenReturn("logo-path");

    NetworkCommon common = buildCommon("test", 123);
    common.setAccountActivity(null);
    underTest.scheduleActivityEnrichment(common);

    verify(mockMxClient, times(1)).getCleansedMerchantName(eq("test"), eq(123));
    verifyNoInteractions(mockAccountActivityService);
    verifyNoInteractions(mockClearbitClient);
    assertThat(common)
        .matches(it -> "test".equals(it.getMerchantStatementDescriptor()))
        .matches(it -> "enh-name".equals(it.getMerchantName()))
        .matches(it -> "logo-path".equals(it.getEnhancedMerchantLogo()));
  }

  @Test
  @SneakyThrows
  public void
      scheduleActivityEnrichment_whenValidMxResultUsingClearbitLogos_clearbitIsQueriedForLogos() {
    underTest =
        new NetworkMessageEnrichmentService(
            mockAccountActivityService, mockMxClient, mockClearbitClient, false);

    when(mockMxClient.getCleansedMerchantName(anyString(), anyInt()))
        .thenReturn(
            new EnhanceTransactionResponse(
                List.of(new TransactionRecordResponse("testing", "enh-name", "guid"))));

    when(mockClearbitClient.getLogo(eq("enh-name"))).thenReturn("logo-path");

    NetworkCommon common = buildCommon("test", 123);
    common.setAccountActivity(null);
    underTest.scheduleActivityEnrichment(common);

    verify(mockMxClient, times(1)).getCleansedMerchantName(eq("test"), eq(123));
    verify(mockMxClient, never()).getMerchantLogo(anyString());
    verifyNoInteractions(mockAccountActivityService);
    assertThat(common)
        .matches(it -> "test".equals(it.getMerchantStatementDescriptor()))
        .matches(it -> "enh-name".equals(it.getMerchantName()))
        .matches(it -> "logo-path".equals(it.getEnhancedMerchantLogo()));
  }

  @Test
  @SneakyThrows
  public void scheduleActivityEnrichment_whenAccountActvityIsAlreadyPersisted_updateEntity() {
    when(mockMxClient.getCleansedMerchantName(anyString(), anyInt()))
        .thenReturn(
            new EnhanceTransactionResponse(
                List.of(new TransactionRecordResponse("testing", "enh-name", "guid"))));

    when(mockMxClient.getMerchantLogo(eq("guid"))).thenReturn("logo-path");

    NetworkCommon common = buildCommon("test", 123);

    Business business = new Business();
    business.setId(new TypedId<>());

    AccountActivity entity = new AccountActivity();
    entity.setMerchant(new MerchantDetails());
    common.setAccountActivity(entity);
    common.setBusiness(business);

    underTest.scheduleActivityEnrichment(common);

    AccountActivity accountActivity = common.getAccountActivity();

    verify(mockMxClient, times(1)).getCleansedMerchantName(eq("test"), eq(123));
    verify(mockAccountActivityService, times(1))
        .updateMerchantData(
            business.getId(), accountActivity.getId(), "enh-name", "logo-path", "test");
    verifyNoInteractions(mockClearbitClient);

    assertThat(accountActivity.getMerchant().getStatementDescriptor()).isEqualTo("test");
    assertThat(accountActivity.getMerchant().getName()).isEqualTo("enh-name");
    assertThat(accountActivity.getMerchant().getLogoUrl()).isEqualTo("logo-path");
  }

  private NetworkCommon buildCommon(String merchantName, Integer merchantCategoryCode) {
    Amount amount = Amount.of(Currency.USD, 123L);
    return new NetworkCommon(
        "",
        NetworkMessageType.AUTH_CREATED,
        amount,
        amount,
        "",
        merchantName,
        amount,
        new ClearAddress(),
        merchantCategoryCode,
        MerchantType.ACCOUNTING_BOOKKEEPING_SERVICES,
        OffsetDateTime.now().minus(30, ChronoUnit.MINUTES),
        "",
        "",
        amount);
  }

  private class MyInLineExecutor implements ExecutorService {

    @Override
    public void shutdown() {}

    @NotNull
    @Override
    public List<Runnable> shutdownNow() {
      return null;
    }

    @Override
    public boolean isShutdown() {
      return false;
    }

    @Override
    public boolean isTerminated() {
      return false;
    }

    @Override
    public boolean awaitTermination(long timeout, @NotNull TimeUnit unit)
        throws InterruptedException {
      return false;
    }

    @NotNull
    @Override
    public <T> Future<T> submit(@NotNull Callable<T> task) {
      return null;
    }

    @NotNull
    @Override
    public <T> Future<T> submit(@NotNull Runnable task, T result) {
      return null;
    }

    @NotNull
    @Override
    public Future<?> submit(@NotNull Runnable task) {
      task.run();
      return null;
    }

    @NotNull
    @Override
    public <T> List<Future<T>> invokeAll(@NotNull Collection<? extends Callable<T>> tasks)
        throws InterruptedException {
      return null;
    }

    @NotNull
    @Override
    public <T> List<Future<T>> invokeAll(
        @NotNull Collection<? extends Callable<T>> tasks, long timeout, @NotNull TimeUnit unit)
        throws InterruptedException {
      return null;
    }

    @NotNull
    @Override
    public <T> T invokeAny(@NotNull Collection<? extends Callable<T>> tasks)
        throws InterruptedException, ExecutionException {
      return null;
    }

    @Override
    public <T> T invokeAny(
        @NotNull Collection<? extends Callable<T>> tasks, long timeout, @NotNull TimeUnit unit)
        throws InterruptedException, ExecutionException, TimeoutException {
      return null;
    }

    @Override
    public void execute(@NotNull Runnable command) {}
  }
}
