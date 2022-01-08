package com.clearspend.capital.data.repository.business;

import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessOwnerId;
import com.clearspend.capital.common.typedid.data.business.BusinessProspectId;
import com.clearspend.capital.data.model.business.BusinessProspect;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BusinessProspectRepository
    extends JpaRepository<BusinessProspect, TypedId<BusinessProspectId>> {

  Optional<BusinessProspect> findBySubjectRef(String subjectRef);

  Optional<BusinessProspect> findByBusinessOwnerId(TypedId<BusinessOwnerId> businessOwnerId);

  Optional<BusinessProspect> findByEmailHash(byte[] hash);
}
