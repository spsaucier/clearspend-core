package com.tranwall.capital.data.repository;

import com.tranwall.capital.common.typedid.data.BusinessId;
import com.tranwall.capital.common.typedid.data.PlaidLogEntryId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.data.model.PlaidLogEntry;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaidLogEntryRepository
    extends JpaRepository<PlaidLogEntry, TypedId<PlaidLogEntryId>> {

  List<PlaidLogEntry> findByBusinessIdOrderByCreatedAsc(TypedId<BusinessId> businessId);
}
