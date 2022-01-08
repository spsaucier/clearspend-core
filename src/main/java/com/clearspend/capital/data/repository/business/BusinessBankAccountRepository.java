package com.clearspend.capital.data.repository.business;

import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessBankAccountId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.crypto.data.model.embedded.RequiredEncryptedStringWithHash;
import com.clearspend.capital.data.model.business.BusinessBankAccount;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BusinessBankAccountRepository
    extends JpaRepository<BusinessBankAccount, TypedId<BusinessBankAccountId>> {

  List<BusinessBankAccount> findBusinessBankAccountsByBusinessId(TypedId<BusinessId> businessId);

  default List<BusinessBankAccount> findAllByAccessToken(
      RequiredEncryptedStringWithHash accessToken) {
    return findAllByAccessTokenHash(accessToken.getHash());
  }

  List<BusinessBankAccount> findAllByAccessTokenHash(byte[] hash);
}
