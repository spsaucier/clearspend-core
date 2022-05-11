package com.clearspend.capital.service;

import com.clearspend.capital.client.clearbit.ClearbitClient;
import com.clearspend.capital.client.mx.MxClient;
import com.clearspend.capital.client.mx.types.EnhanceTransactionResponse;
import com.clearspend.capital.client.mx.types.TransactionRecordResponse;
import com.clearspend.capital.data.model.embedded.MerchantDetails;
import com.clearspend.capital.service.type.NetworkCommon;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class NetworkMessageEnrichmentService {

  private final AccountActivityService accountActivityService;
  private final MxClient mxClient;
  private final ClearbitClient clearbitClient;
  private final boolean useMxLogos;

  public NetworkMessageEnrichmentService(
      AccountActivityService accountActivityService,
      MxClient mxClient,
      ClearbitClient clearbitClient,
      @Value("${client.mx.use-mx-logo:true}") boolean useMxLogos) {
    this.accountActivityService = accountActivityService;
    this.mxClient = mxClient;
    this.clearbitClient = clearbitClient;
    this.useMxLogos = useMxLogos;
  }

  @Async
  public void scheduleActivityEnrichment(NetworkCommon common) {
    EnhanceTransactionResponse mxResponse =
        mxClient.getCleansedMerchantName(
            common.getMerchantName(), common.getMerchantCategoryCode());
    if (mxResponse.getTransactions() != null && mxResponse.getTransactions().size() > 0) {
      TransactionRecordResponse mxDetail = mxResponse.getTransactions().get(0);
      String logoPath = "";
      if (useMxLogos && mxDetail.getExternalGuid() != null) {
        logoPath = mxClient.getMerchantLogo(mxDetail.getExternalGuid());
      } else {
        logoPath = clearbitClient.getLogo(mxDetail.getEnhancedName());
      }
      if (common.getAccountActivity() == null) {
        log.info(
            "Enriched merchant details for NetworkCommon. From <{}> to <{}>",
            common.getMerchantName(),
            mxDetail.getEnhancedName());
        common.setMerchantStatementDescriptor(common.getMerchantName());
        common.setMerchantName(mxDetail.getEnhancedName());
        common.setEnhancedMerchantLogo(logoPath);
      } else {
        log.info(
            "Enriched merchant details for AccountActivity. From <{}> to <{}>",
            common.getMerchantName(),
            mxDetail.getEnhancedName());
        MerchantDetails merchantDetails = common.getAccountActivity().getMerchant();
        merchantDetails.setStatementDescriptor(common.getMerchantName());
        merchantDetails.setName(mxDetail.getEnhancedName());
        merchantDetails.setLogoUrl(logoPath);

        accountActivityService.updateMerchantData(
            common.getBusiness().getId(),
            common.getAccountActivity().getId(),
            merchantDetails.getName(),
            merchantDetails.getLogoUrl(),
            merchantDetails.getStatementDescriptor());
      }
    }
  }
}
