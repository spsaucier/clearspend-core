package com.clearspend.capital.service;

import com.clearspend.capital.common.error.RecordNotFoundException;
import com.clearspend.capital.common.error.Table;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.common.typedid.data.business.BusinessBankAccountId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.data.model.User;
import com.clearspend.capital.data.model.business.Business;
import com.clearspend.capital.data.model.business.BusinessBankAccount;
import com.clearspend.capital.data.repository.UserRepository;
import com.clearspend.capital.data.repository.business.BusinessBankAccountRepository;
import com.clearspend.capital.data.repository.business.BusinessRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Package protected service holding entity retrieval methods. Can be safely used by other services
 * without a risk of running into circular dependencies
 */
@Service
@RequiredArgsConstructor
class RetrievalService {

  private final BusinessRepository businessRepository;
  private final BusinessBankAccountRepository businessBankAccountRepository;
  private final UserRepository userRepository;

  Business retrieveBusiness(TypedId<BusinessId> businessId, boolean mustExist) {
    return businessRepository
        .findById(businessId)
        .orElseThrow(() -> new RecordNotFoundException(Table.BUSINESS, mustExist, businessId));
  }

  BusinessBankAccount retrieveBusinessBankAccount(
      TypedId<BusinessBankAccountId> businessBankAccountId) {
    return businessBankAccountRepository
        .findById(businessBankAccountId)
        .orElseThrow(
            () -> new RecordNotFoundException(Table.BUSINESS_BANK_ACCOUNT, businessBankAccountId));
  }

  User retrieveUser(TypedId<BusinessId> businessId, TypedId<UserId> userId) {
    return userRepository
        .findByBusinessIdAndId(businessId, userId)
        .orElseThrow(() -> new RecordNotFoundException(Table.USER, userId, businessId));
  }
}
