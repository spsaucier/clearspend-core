package com.tranwall.capital.data.repository;

import com.tranwall.capital.common.typedid.data.BusinessOwnerId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.data.model.BusinessOwner;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BusinessOwnerRepository
    extends JpaRepository<BusinessOwner, TypedId<BusinessOwnerId>> {}
