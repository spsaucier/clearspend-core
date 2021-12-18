package com.clearspend.capital.data.repository;

import com.clearspend.capital.common.typedid.data.BusinessId;
import com.clearspend.capital.common.typedid.data.BusinessOwnerId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.data.model.BusinessOwner;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BusinessOwnerRepository
    extends JpaRepository<BusinessOwner, TypedId<BusinessOwnerId>> {

  Optional<BusinessOwner> findBySubjectRef(String subjectRef);

  List<BusinessOwner> findByBusinessId(TypedId<BusinessId> businessId);
}
