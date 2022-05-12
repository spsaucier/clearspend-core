package com.clearspend.capital.data.repository.security;

import com.clearspend.capital.common.typedid.data.GlobalRoleId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.data.model.security.GlobalRole;
import java.util.List;
import lombok.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GlobalRoleRepository extends JpaRepository<GlobalRole, TypedId<GlobalRoleId>> {

  @Override
  @NonNull
  List<GlobalRole> findAll();
}
