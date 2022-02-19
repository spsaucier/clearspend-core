package com.clearspend.capital.data.repository;

import com.clearspend.capital.common.typedid.data.PendingStripeTransferId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.data.model.PendingStripeTransfer;
import com.clearspend.capital.data.model.enums.PendingStripeTransferState;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PendingStripeTransferRepository
    extends JpaRepository<PendingStripeTransfer, TypedId<PendingStripeTransferId>> {

  List<PendingStripeTransfer> findByBusinessIdAndState(
      TypedId<BusinessId> businessId, PendingStripeTransferState state);
}
