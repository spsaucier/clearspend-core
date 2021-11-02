package com.tranwall.capital.data.repository;

import com.tranwall.capital.common.typedid.data.BusinessId;
import com.tranwall.capital.common.typedid.data.ReceiptId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.common.typedid.data.UserId;
import com.tranwall.capital.data.model.Receipt;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReceiptRepository extends JpaRepository<Receipt, TypedId<ReceiptId>> {

  Optional<Receipt> findReceiptByBusinessIdAndUserIdAndId(
      TypedId<BusinessId> businessId, TypedId<UserId> userId, TypedId<ReceiptId> id);
}
