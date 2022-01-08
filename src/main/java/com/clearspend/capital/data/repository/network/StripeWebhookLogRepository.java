package com.clearspend.capital.data.repository.network;

import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.network.NetworkMessageId;
import com.clearspend.capital.common.typedid.data.network.StripeWebhookLogId;
import com.clearspend.capital.data.model.network.StripeWebhookLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StripeWebhookLogRepository
    extends JpaRepository<StripeWebhookLog, TypedId<StripeWebhookLogId>> {

  StripeWebhookLog findByNetworkMessageId(TypedId<NetworkMessageId> networkMessageIdTypedId);
}
