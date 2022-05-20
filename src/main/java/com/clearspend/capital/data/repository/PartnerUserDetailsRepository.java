package com.clearspend.capital.data.repository;

import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.data.model.PartnerUserDetails;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PartnerUserDetailsRepository
    extends JpaRepository<PartnerUserDetails, TypedId<UserId>> {}
