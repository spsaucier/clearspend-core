package com.clearspend.capital.service;

import com.clearspend.capital.client.clearbit.ClearbitClient;
import com.clearspend.capital.client.mx.MxClient;
import com.clearspend.capital.client.mx.types.EnhanceTransactionResponse;
import com.clearspend.capital.client.mx.types.TransactionRecordResponse;
import com.clearspend.capital.data.model.AccountActivity;
import com.clearspend.capital.data.repository.AccountActivityRepository;
import com.clearspend.capital.service.type.NetworkCommon;
import com.google.common.annotations.VisibleForTesting;
import java.util.concurrent.ExecutorService;
import javax.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class NetworkMessageEnrichmentService {

  private final AccountActivityRepository accountActivityRepository;
  private final MxClient mxClient;
  private final ClearbitClient clearbitClient;
  private final ExecutorService executor;

  private boolean useMxLogos;

  public NetworkMessageEnrichmentService(
      AccountActivityRepository accountActivityRepository,
      MxClient mxClient,
      ClearbitClient clearbitClient,
      @Qualifier("MessageEnrichment") ExecutorService executor,
      @Value("${client.mx.use-mx-logo:true}") boolean useMxLogos) {
    this.accountActivityRepository = accountActivityRepository;
    this.mxClient = mxClient;
    this.clearbitClient = clearbitClient;
    this.executor = executor;
    this.useMxLogos = useMxLogos;
  }

  @Transactional
  public void scheduleActivityEnrichment(NetworkCommon common) {
    executor.submit(
        () -> {
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
                  "Enriched merchant details for NetworkCommon. From <%s> to <%s>",
                  common.getMerchantName(), mxDetail.getEnhancedName());
              common.setMerchantStatementDescriptor(common.getMerchantName());
              common.setMerchantName(mxDetail.getEnhancedName());
              common.setEnhancedMerchantLogo(logoPath);
            } else {
              log.info(
                  "Enriched merchant details for AccountActivity. From <%s> to <%s>",
                  common.getMerchantName(), mxDetail.getEnhancedName());
              AccountActivity accountActivity = common.getAccountActivity();
              accountActivity.getMerchant().setStatementDescriptor(common.getMerchantName());
              accountActivity.getMerchant().setName(mxDetail.getEnhancedName());
              accountActivity.getMerchant().setLogoUrl(logoPath);
              accountActivityRepository.save(accountActivity);
            }
          }
        });
  }

  @VisibleForTesting
  public void setUseMxLogos(boolean useMxLogos) {
    this.useMxLogos = useMxLogos;
  }
}
