package com.clearspend.capital.service.kyc;

import com.clearspend.capital.client.stripe.types.Account;
import com.clearspend.capital.crypto.HashUtil;
import com.clearspend.capital.data.model.business.Business;
import com.clearspend.capital.data.model.business.BusinessOwner;
import com.clearspend.capital.data.model.enums.BusinessStatus;
import com.clearspend.capital.data.model.enums.KnowYourBusinessStatus;
import com.stripe.model.Account.Requirements;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
@Order(2)
public class BusinessKycStepBusinessHardFail extends BusinessKycStep {

  @Override
  public boolean support(Requirements requirements, Business business, Account account) {
    return StringUtils.isNotEmpty(requirements.getDisabledReason())
        && requirements.getDisabledReason().startsWith(REJECTED);
  }

  @Override
  public List<String> execute(Requirements requirements, Business business, Account account) {
    if (business.getStatus() != BusinessStatus.CLOSED) {
      updateBusiness(business.getId(), BusinessStatus.CLOSED, null, KnowYourBusinessStatus.FAIL);
      BusinessOwner businessOwner =
          businessOwnerRepository
              .findByBusinessIdAndEmailHash(
                  business.getId(),
                  HashUtil.calculateHash(business.getBusinessEmail().getEncrypted()))
              .orElse(
                  businessOwnerRepository.findByBusinessId(business.getId()).stream()
                      .findAny()
                      .orElseThrow());

      List<String> reasons = extractErrorMessages(requirements);
      twilioService.sendKybKycFailEmail(
          business.getBusinessEmail().getEncrypted(),
          businessOwner.getFirstName().getEncrypted(),
          reasons);
      return reasons;
    }
    return extractErrorMessages(requirements);
  }
}
