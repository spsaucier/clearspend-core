package com.clearspend.capital.data.repository;

import com.clearspend.capital.common.typedid.data.GlobalRoleId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.data.model.GlobalRole;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GlobalRoleRepository extends JpaRepository<GlobalRole, TypedId<GlobalRoleId>> {}
