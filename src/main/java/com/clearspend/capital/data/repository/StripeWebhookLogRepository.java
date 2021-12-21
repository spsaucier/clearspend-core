package com.clearspend.capital.data.repository;

import com.clearspend.capital.common.typedid.data.StripeWebhookLogId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.data.model.StripeWebhookLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StripeWebhookLogRepository
    extends JpaRepository<StripeWebhookLog, TypedId<StripeWebhookLogId>> {}
