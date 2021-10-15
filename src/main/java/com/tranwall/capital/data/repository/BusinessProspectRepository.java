package com.tranwall.capital.data.repository;

import com.tranwall.capital.common.typedid.data.BusinessProspectId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.data.model.BusinessProspect;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BusinessProspectRepository
    extends JpaRepository<BusinessProspect, TypedId<BusinessProspectId>> {}
