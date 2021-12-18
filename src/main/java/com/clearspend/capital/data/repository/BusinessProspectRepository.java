package com.clearspend.capital.data.repository;

import com.clearspend.capital.common.typedid.data.BusinessOwnerId;
import com.clearspend.capital.common.typedid.data.BusinessProspectId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.data.model.BusinessProspect;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BusinessProspectRepository
    extends JpaRepository<BusinessProspect, TypedId<BusinessProspectId>> {

  Optional<BusinessProspect> findBySubjectRef(String subjectRef);

  Optional<BusinessProspect> findByBusinessOwnerId(TypedId<BusinessOwnerId> businessOwnerId);

  Optional<BusinessProspect> findByEmailHash(byte[] hash);
}
