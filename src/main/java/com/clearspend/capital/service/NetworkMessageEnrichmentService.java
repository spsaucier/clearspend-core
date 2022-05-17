package com.clearspend.capital.service;

import com.clearspend.capital.client.clearbit.ClearbitClient;
import com.clearspend.capital.client.codat.CodatClient;
import com.clearspend.capital.client.codat.types.CodatSupplier;
import com.clearspend.capital.client.mx.MxClient;
import com.clearspend.capital.client.mx.types.EnhanceTransactionResponse;
import com.clearspend.capital.client.mx.types.TransactionRecordResponse;
import com.clearspend.capital.data.model.network.NetworkMerchant;
import com.clearspend.capital.data.repository.network.NetworkMerchantRepository;
import com.clearspend.capital.service.type.NetworkCommon;
import com.google.common.annotations.VisibleForTesting;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class NetworkMessageEnrichmentService {

  private final AccountActivityService accountActivityService;
  private final ClearbitClient clearbitClient;
  private final CodatClient codatClient;
  private final MxClient mxClient;
  private final NetworkMerchantRepository networkMerchantRepository;

  private boolean useMxLogos;

  public NetworkMessageEnrichmentService(
      AccountActivityService accountActivityService,
      MxClient mxClient,
      ClearbitClient clearbitClient,
      CodatClient codatClient,
      NetworkMerchantRepository networkMerchantRepository,
      @Value("${client.mx.use-mx-logos:true}") boolean useMxLogos) {

    this.accountActivityService = accountActivityService;
    this.mxClient = mxClient;
    this.clearbitClient = clearbitClient;
    this.codatClient = codatClient;
    this.networkMerchantRepository = networkMerchantRepository;
    this.useMxLogos = useMxLogos;
  }

  @Async
  public void scheduleActivityEnrichment(NetworkCommon common) {
    if (common.getAccountActivity() != null) {
      EnhanceTransactionResponse mxResponse =
          mxClient.getCleansedMerchantName(
              common.getMerchantName(), common.getMerchantCategoryCode());
      if (mxResponse.getTransactions() != null && mxResponse.getTransactions().size() > 0) {
        TransactionRecordResponse mxDetail = mxResponse.getTransactions().get(0);
        String logoPath = "";
        Optional<NetworkMerchant> entity =
            networkMerchantRepository.findByMerchantNameAndMerchantCategoryCode(
                mxDetail.getEnhancedName(), mxDetail.getEnhancedCategoryCode());
        if (entity.isPresent()) {
          logoPath = entity.get().getMerchantLogoUrl();
        } else if (useMxLogos && mxDetail.getExternalMerchantId() != null) {
          logoPath = mxClient.getMerchantLogo(mxDetail.getExternalMerchantId());
        } else {
          logoPath = clearbitClient.getLogo(mxDetail.getEnhancedName());
        }
        // If we haven't cached this Merchant, we should
        if (entity.isEmpty()) {
          createNetworkMerchant(mxDetail, logoPath);
        }

        // Check if the merchant exists in the Accounting Integration and set those values too
        Optional<CodatSupplier> supplier = getCodatSupplierDetailsIfPresent(common, mxDetail);

        saveEnhancedMerchantDetails(common, mxDetail, logoPath, supplier);
      }
    }
  }

  private Optional<CodatSupplier> getCodatSupplierDetailsIfPresent(
      NetworkCommon common, TransactionRecordResponse mxDetail) {
    if (common.getBusiness() != null
        && !StringUtils.isBlank(common.getBusiness().getCodatCompanyRef())) {
      return codatClient
          .getSupplierForBusiness(
              common.getBusiness().getCodatCompanyRef(), mxDetail.getEnhancedName())
          .getResults()
          .stream()
          .findFirst();
    }
    return Optional.empty();
  }

  private void createNetworkMerchant(TransactionRecordResponse mxDetail, String logoPath) {
    NetworkMerchant networkMerchant = new NetworkMerchant();
    networkMerchant.setMerchantName(mxDetail.getEnhancedName());
    networkMerchant.setMerchantCategoryCode(mxDetail.getEnhancedCategoryCode());
    networkMerchant.setMerchantLogoUrl(logoPath);
    networkMerchant.setCategory(mxDetail.getCategoryDescription());
    networkMerchant.setMxMerchantId(mxDetail.getExternalMerchantId());
    networkMerchant.setMxLocationId(mxDetail.getExternalLocationId());
    networkMerchantRepository.save(networkMerchant);
  }

  private void saveEnhancedMerchantDetails(
      NetworkCommon common,
      TransactionRecordResponse mxDetail,
      String logoPath,
      Optional<CodatSupplier> supplier) {

    log.info(
        "Enriched merchant details for AccountActivity. From <%s> to <%s>",
        common.getMerchantName(), mxDetail.getEnhancedName());
    CodatSupplier supplierDetails = supplier.orElse(new CodatSupplier());

    accountActivityService.updateMerchantData(
        common.getBusiness().getId(),
        common.getAccountActivity().getId(),
        mxDetail.getEnhancedName(),
        logoPath,
        common.getMerchantName(),
        supplierDetails.getId(),
        supplierDetails.getSupplierName());
  }

  @VisibleForTesting
  public void setUseMxLogos(boolean useMxLogos) {
    this.useMxLogos = useMxLogos;
  }
}
