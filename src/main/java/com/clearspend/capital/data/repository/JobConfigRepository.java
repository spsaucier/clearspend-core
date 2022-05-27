package com.clearspend.capital.data.repository;

import com.clearspend.capital.common.typedid.data.JobConfigId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.data.model.JobConfig;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobConfigRepository extends JpaRepository<JobConfig, TypedId<JobConfigId>> {

  Optional<JobConfig> findByBusinessIdAndId(
      TypedId<BusinessId> businessId, TypedId<JobConfigId> allocationId);

  List<JobConfig> findByBusinessId(TypedId<BusinessId> businessId);

  List<JobConfig> findByBusinessIdAndConfigOwnerId(
      TypedId<BusinessId> businessId, TypedId<UserId> userId);
}
