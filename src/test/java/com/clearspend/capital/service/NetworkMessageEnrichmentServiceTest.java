package com.clearspend.capital.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.clearspend.capital.client.codat.CodatClient;
import com.clearspend.capital.client.codat.types.GetSuppliersResponse;
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
import com.clearspend.capital.data.model.network.NetworkMerchant;
import com.clearspend.capital.data.repository.network.NetworkMerchantRepository;
import com.clearspend.capital.service.type.NetworkCommon;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class NetworkMessageEnrichmentServiceTest {

  private AccountActivityService mockAccountActivityService;
  private MxClient mockMxClient;
  private CodatClient mockCodatClient;
  private NetworkMerchantRepository mockNetworkMerchantRepository;

  private NetworkMessageEnrichmentService underTest;

  @BeforeEach
  public void setup() {
    mockAccountActivityService = Mockito.mock(AccountActivityService.class);
    mockMxClient = Mockito.mock(MxClient.class);
    mockCodatClient = Mockito.mock(CodatClient.class);
    mockNetworkMerchantRepository = Mockito.mock(NetworkMerchantRepository.class);

    // The final boolean parameter is a switch to allow us to revert back to Clearbit for logo urls.
    // If we decide to keep this functionality clean this up using polymorphism
    underTest =
        new NetworkMessageEnrichmentService(
            mockAccountActivityService,
            mockMxClient,
            mockCodatClient,
            mockNetworkMerchantRepository);
  }

  @Test
  @SneakyThrows
  public void scheduleActvityEnrichment_whenAccountActivityIsNull_noEnrichmentHappens() {
    underTest.scheduleActivityEnrichment(buildCommon("no-aa", 123));
    verifyNoInteractions(mockMxClient);
    verifyNoInteractions(mockAccountActivityService);
    verifyNoInteractions(mockNetworkMerchantRepository);
    verifyNoInteractions(mockCodatClient);
  }

  @Test
  @SneakyThrows
  public void scheduleActivityEnrichment_whenMxReturnsEmptyTransactionList_noEnrichmentHappens() {
    when(mockMxClient.getCleansedMerchantName(anyString(), anyInt()))
        .thenReturn(new EnhanceTransactionResponse(Collections.emptyList()));
    NetworkCommon common = buildCommon("test", 123);
    common.setAccountActivity(new AccountActivity());
    underTest.scheduleActivityEnrichment(common);

    verify(mockMxClient, times(1)).getCleansedMerchantName(eq("test"), eq(123));
    verifyNoInteractions(mockAccountActivityService);
    verifyNoInteractions(mockNetworkMerchantRepository);
    verifyNoInteractions(mockCodatClient);
  }

  @Test
  @SneakyThrows
  public void scheduleActivityEnrichment_whenMxReturnsNullList_noEnrichmentHappens() {
    when(mockMxClient.getCleansedMerchantName(anyString(), anyInt()))
        .thenReturn(new EnhanceTransactionResponse(null));

    NetworkCommon common = buildCommon("test", 123);
    common.setAccountActivity(new AccountActivity());
    underTest.scheduleActivityEnrichment(common);

    verify(mockMxClient, times(1)).getCleansedMerchantName(eq("test"), eq(123));
    verifyNoInteractions(mockAccountActivityService);
    verifyNoInteractions(mockNetworkMerchantRepository);
    verifyNoInteractions(mockCodatClient);
  }

  @Test
  @SneakyThrows
  public void scheduleActivityEnrichment_whenValidMxResultUsingMxLogos_mxIsQueriedAgainForLogo() {
    when(mockMxClient.getCleansedMerchantName(anyString(), anyInt()))
        .thenReturn(
            new EnhanceTransactionResponse(
                List.of(
                    new TransactionRecordResponse(
                        "testing", "enh-name", 123, "guid", "loc-guid", "desc"))));

    when(mockMxClient.getMerchantLogo(eq("guid"))).thenReturn("logo-path");
    when(mockNetworkMerchantRepository.findByMerchantNameAndMerchantCategoryCode(
            anyString(), anyInt()))
        .thenReturn(Optional.empty());
    when(mockCodatClient.getSupplierForBusiness(anyString(), anyString()))
        .thenReturn(new GetSuppliersResponse(Collections.emptyList()));

    NetworkCommon common = buildCommon("test", 123);
    common.setAccountActivity(new AccountActivity());
    underTest.scheduleActivityEnrichment(common);

    verify(mockMxClient, times(1)).getCleansedMerchantName(eq("test"), eq(123));
    verify(mockAccountActivityService, times(1))
        .updateMerchantData(
            eq(common.getBusiness().getId()),
            eq(common.getAccountActivity().getId()),
            eq("enh-name"),
            eq("logo-path"),
            eq("test"),
            isNull(),
            isNull());
    verify(mockNetworkMerchantRepository, times(1)).save(any());
  }

  @Test
  @SneakyThrows
  public void scheduleActivityEnrichment_whenNoMxLogoIsAvailable_noNetworkMerchantIsPersisted() {
    when(mockMxClient.getCleansedMerchantName(anyString(), anyInt()))
        .thenReturn(
            new EnhanceTransactionResponse(
                List.of(
                    new TransactionRecordResponse(
                        "testing", "enh-name", 123, null, "loc-guid", "desc"))));
    when(mockNetworkMerchantRepository.findByMerchantNameAndMerchantCategoryCode(
            anyString(), anyInt()))
        .thenReturn(Optional.empty());

    when(mockCodatClient.getSupplierForBusiness(anyString(), anyString()))
        .thenReturn(new GetSuppliersResponse(Collections.emptyList()));

    NetworkCommon common = buildCommon("test", 123);
    common.setAccountActivity(new AccountActivity());
    underTest.scheduleActivityEnrichment(common);

    verify(mockMxClient, times(1)).getCleansedMerchantName(eq("test"), eq(123));
    verify(mockMxClient, never()).getMerchantLogo(anyString());
    verify(mockAccountActivityService, times(1))
        .updateMerchantData(
            eq(common.getBusiness().getId()),
            eq(common.getAccountActivity().getId()),
            eq("enh-name"),
            eq(""),
            eq("test"),
            isNull(),
            isNull());
  }

  @Test
  @SneakyThrows
  public void scheduleActivityEnrichment_whenAccountActivityIsAlreadyPersisted_updateEntity() {
    when(mockMxClient.getCleansedMerchantName(anyString(), anyInt()))
        .thenReturn(
            new EnhanceTransactionResponse(
                List.of(
                    new TransactionRecordResponse(
                        "testing", "enh-name", 123, "guid", "loc-guid", "desc"))));

    when(mockMxClient.getMerchantLogo(eq("guid"))).thenReturn("logo-path");

    when(mockCodatClient.getSupplierForBusiness(anyString(), anyString()))
        .thenReturn(new GetSuppliersResponse(Collections.emptyList()));

    NetworkCommon common = buildCommon("test", 123);

    Business business = new Business();
    business.setId(new TypedId<>());
    business.setCodatCompanyRef("codat-ref");

    AccountActivity entity = new AccountActivity();
    entity.setMerchant(new MerchantDetails());
    common.setAccountActivity(entity);
    common.setBusiness(business);

    underTest.scheduleActivityEnrichment(common);

    AccountActivity accountActivity = common.getAccountActivity();

    verify(mockMxClient, times(1)).getCleansedMerchantName(eq("test"), eq(123));
    verify(mockAccountActivityService, times(1))
        .updateMerchantData(
            eq(business.getId()),
            eq(accountActivity.getId()),
            eq("enh-name"),
            eq("logo-path"),
            eq("test"),
            isNull(),
            isNull());
  }

  @Test
  @SneakyThrows
  public void
      scheduleActivityEnrichment_whenNetworkMerchantAlreadyExistsAndLogoAlreadyExists_mxIsNotQueriedAgainForLogo() {
    when(mockMxClient.getCleansedMerchantName(anyString(), anyInt()))
        .thenReturn(
            new EnhanceTransactionResponse(
                List.of(
                    new TransactionRecordResponse(
                        "testing", "enh-name", 123, "guid", "loc-guid", "desc"))));

    NetworkMerchant networkMerchant = new NetworkMerchant();
    networkMerchant.setMerchantLogoUrl("cached-logo");
    when(mockNetworkMerchantRepository.findByMerchantNameAndMerchantCategoryCode(
            anyString(), anyInt()))
        .thenReturn(Optional.of(networkMerchant));
    when(mockCodatClient.getSupplierForBusiness(anyString(), anyString()))
        .thenReturn(new GetSuppliersResponse(Collections.emptyList()));

    NetworkCommon common = buildCommon("test", 123);
    common.setAccountActivity(new AccountActivity());
    underTest.scheduleActivityEnrichment(common);

    verify(mockMxClient, times(1)).getCleansedMerchantName(eq("test"), eq(123));
    verify(mockMxClient, never()).getMerchantLogo(anyString());
    verify(mockAccountActivityService, times(1))
        .updateMerchantData(
            eq(common.getBusiness().getId()),
            eq(common.getAccountActivity().getId()),
            eq("enh-name"),
            eq("cached-logo"),
            eq("test"),
            isNull(),
            isNull());
    verify(mockNetworkMerchantRepository, never()).save(any());
  }

  @Test
  @SneakyThrows
  public void
      scheduleActivityEnrichment_whenNetworkMerchantDoesNotExists_mxIsQueriedAgainForLogoAndRecordIsCached() {
    when(mockMxClient.getCleansedMerchantName(anyString(), anyInt()))
        .thenReturn(
            new EnhanceTransactionResponse(
                List.of(
                    new TransactionRecordResponse(
                        "testing", "enh-name", 123, "guid", "loc-guid", "desc"))));

    when(mockNetworkMerchantRepository.findByMerchantNameAndMerchantCategoryCode(
            anyString(), anyInt()))
        .thenReturn(Optional.empty());
    when(mockMxClient.getMerchantLogo(eq("guid"))).thenReturn("logo-path");
    when(mockCodatClient.getSupplierForBusiness(anyString(), anyString()))
        .thenReturn(new GetSuppliersResponse(Collections.emptyList()));

    NetworkCommon common = buildCommon("test", 123);
    common.setAccountActivity(new AccountActivity());
    underTest.scheduleActivityEnrichment(common);

    ArgumentCaptor<NetworkMerchant> merchantCaptor = ArgumentCaptor.forClass(NetworkMerchant.class);
    verify(mockMxClient, times(1)).getCleansedMerchantName(eq("test"), eq(123));
    verify(mockAccountActivityService, times(1))
        .updateMerchantData(
            eq(common.getBusiness().getId()),
            eq(common.getAccountActivity().getId()),
            eq("enh-name"),
            eq("logo-path"),
            eq("test"),
            isNull(),
            isNull());
    verify(mockNetworkMerchantRepository, times(1)).save(merchantCaptor.capture());
    assertThat(merchantCaptor.getValue())
        .matches(it -> "enh-name".equals(it.getMerchantName()))
        .matches(it -> Integer.valueOf(123).equals(it.getMerchantCategoryCode()))
        .matches(it -> "logo-path".equals(it.getMerchantLogoUrl()))
        .matches(it -> "desc".equals(it.getCategory()))
        .matches(it -> "guid".equals(it.getMxMerchantId()))
        .matches(it -> "loc-guid".equals(it.getMxLocationId()));
  }

  private NetworkCommon buildCommon(String merchantName, Integer merchantCategoryCode) {
    Amount amount = Amount.of(Currency.USD, 123L);
    NetworkCommon result =
        new NetworkCommon(
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
    Business business = new Business();
    business.setId(new TypedId<>());
    business.setCodatCompanyRef("company-ref");
    business.setCodatConnectionId("connection-id");
    result.setBusiness(business);
    return result;
  }
}
