package com.clearspend.capital.data.repository;

import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.data.model.Allocation;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AllocationRepository
    extends JpaRepository<Allocation, TypedId<AllocationId>>, AllocationRepositoryCustom {

  Optional<Allocation> findByBusinessIdAndId(
      TypedId<BusinessId> businessId, TypedId<AllocationId> allocationId);

  List<Allocation> findByBusinessId(TypedId<BusinessId> businessId);

  List<Allocation> findByBusinessIdAndIdIn(
      TypedId<BusinessId> businessId, Set<TypedId<AllocationId>> allocationId);

  Allocation findByBusinessIdAndParentAllocationIdIsNull(TypedId<BusinessId> businessId);

  List<Allocation> findByBusinessIdAndParentAllocationId(
      TypedId<BusinessId> businessId, TypedId<AllocationId> parentBusinessAllocationId);

  List<Allocation> findByBusinessIdAndNameIgnoreCaseContaining(
      TypedId<BusinessId> businessId, String name);

  // for deleting businesses in tests only
  void deleteByBusinessId(TypedId<BusinessId> businessId);
}
