package com.clearspend.capital.data.repository.security;

import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserAllocationRoleId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.data.model.security.UserAllocationRole;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAllocationRoleRepository
    extends JpaRepository<UserAllocationRole, TypedId<UserAllocationRoleId>>,
        UserAllocationRoleRepositoryCustom {

  List<UserAllocationRole> findAllByUserId(TypedId<UserId> userId);

  /**
   * @param userId
   * @param allocationId
   * @return A list, presumably of 0 or 1 records.
   */
  Optional<UserAllocationRole> findByUserIdAndAllocationId(
      TypedId<UserId> userId, TypedId<AllocationId> allocationId);

  List<UserAllocationRole> findAllByAllocationId(TypedId<AllocationId>... allocationId);
}
