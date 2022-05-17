package com.clearspend.capital.data.repository.business;

import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.common.typedid.data.business.BusinessSettingsId;
import com.clearspend.capital.data.model.business.BusinessSettings;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BusinessSettingsRepository
    extends JpaRepository<BusinessSettings, TypedId<BusinessSettingsId>> {

  Optional<BusinessSettings> findByBusinessId(TypedId<BusinessId> businessId);

  void deleteByBusinessId(TypedId<BusinessId> businessId);
}
