package com.clearspend.capital.data.repository;

import com.clearspend.capital.common.typedid.data.PlaidLogEntryId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.data.model.PlaidLogEntry;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaidLogEntryRepository
    extends JpaRepository<PlaidLogEntry, TypedId<PlaidLogEntryId>> {

  Optional<PlaidLogEntry> findByBusinessIdAndId(
      TypedId<BusinessId> businessId, TypedId<PlaidLogEntryId> id);

  List<PlaidLogEntry> findByBusinessIdOrderByCreatedAsc(TypedId<BusinessId> businessId);

  Page<PlaidLogEntry> findByBusinessIdOrderByCreatedAsc(
      final TypedId<BusinessId> businessId, final Pageable page);
}
