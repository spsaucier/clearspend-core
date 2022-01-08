package com.clearspend.capital.data.repository.business;

import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.common.typedid.data.business.BusinessOwnerId;
import com.clearspend.capital.data.model.business.BusinessOwner;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BusinessOwnerRepository
    extends JpaRepository<BusinessOwner, TypedId<BusinessOwnerId>> {

  Optional<BusinessOwner> findBySubjectRef(String subjectRef);

  List<BusinessOwner> findByBusinessId(TypedId<BusinessId> businessId);

  Optional<BusinessOwner> findByBusinessIdAndSubjectRefIsNotNull(TypedId<BusinessId> businessId);
}
