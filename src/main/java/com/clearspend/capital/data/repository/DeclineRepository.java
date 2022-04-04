package com.clearspend.capital.data.repository;

import com.clearspend.capital.common.typedid.data.DeclineId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.data.model.decline.Decline;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeclineRepository extends JpaRepository<Decline, TypedId<DeclineId>> {}
