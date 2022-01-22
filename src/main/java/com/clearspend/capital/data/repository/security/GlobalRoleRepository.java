package com.clearspend.capital.data.repository.security;

import com.clearspend.capital.common.typedid.data.GlobalRoleId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.data.model.security.GlobalRole;
import java.util.List;
import lombok.NonNull;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GlobalRoleRepository extends JpaRepository<GlobalRole, TypedId<GlobalRoleId>> {

  @Override
  @Cacheable("globalRoles")
  @NonNull
  List<GlobalRole> findAll();
}
