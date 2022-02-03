package com.clearspend.capital.data.repository.network;

import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.network.NetworkMerchantId;
import com.clearspend.capital.data.model.network.NetworkMerchant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NetworkMerchantRepository
    extends JpaRepository<NetworkMerchant, TypedId<NetworkMerchantId>> {

  Optional<NetworkMerchant> findByMerchantNameAndMerchantCategoryCode(
      String merchantName, Integer merchantCategoryCode);
}
