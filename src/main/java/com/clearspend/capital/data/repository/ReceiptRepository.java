package com.clearspend.capital.data.repository;

import com.clearspend.capital.common.typedid.data.ReceiptId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.data.model.Receipt;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReceiptRepository extends JpaRepository<Receipt, TypedId<ReceiptId>> {

  Optional<Receipt> findReceiptByBusinessIdAndId(
      TypedId<BusinessId> businessId, TypedId<ReceiptId> id);

  List<Receipt> findReceiptByBusinessIdAndUserIdAndLinked(
      TypedId<BusinessId> businessId, TypedId<UserId> userId, boolean linked);

  Optional<Receipt> findReceiptByBusinessIdAndUserIdAndId(
      TypedId<BusinessId> businessId, TypedId<UserId> userId, TypedId<ReceiptId> id);
}
