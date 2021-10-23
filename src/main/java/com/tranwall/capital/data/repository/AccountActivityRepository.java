package com.tranwall.capital.data.repository;

import com.tranwall.capital.common.typedid.data.AccountActivityId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.data.model.AccountActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AccountActivityRepository
    extends JpaRepository<AccountActivity, TypedId<AccountActivityId>>,
        JpaSpecificationExecutor<AccountActivity> {}
