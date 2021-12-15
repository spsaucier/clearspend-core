package com.tranwall.capital.data.repository;

import com.tranwall.capital.common.typedid.data.BusinessBankAccountId;
import com.tranwall.capital.common.typedid.data.BusinessId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.crypto.data.model.embedded.RequiredEncryptedStringWithHash;
import com.tranwall.capital.data.model.BusinessBankAccount;
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
