package com.clearspend.capital.data.model.network;

import com.clearspend.capital.common.data.model.TypedMutable;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.network.NetworkMessageId;
import com.clearspend.capital.common.typedid.data.network.StripeWebhookLogId;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Type;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@DynamicUpdate
@Slf4j
// Captures the raw request from Stripe payment network requests
public class StripeWebhookLog extends TypedMutable<StripeWebhookLogId> {

  // the ID from the request from Stripe (e.g. evt_1KCzOkGAnZyEKADzkAOHzOnU)
  private String stripeEventRef;

  // the contents of the "object" field from the Stripe request
  private String eventType;

  // the raw request in JSON
  private String request;

  // if not null, this contains the error Capital encountered during processing
  private String error;

  // the time Capital spent processing the request (note this is the time when the request gets into
  // the controller until we return from the controller method).
  private Long processingTimeMs;

  @JoinColumn(referencedColumnName = "id", table = "network_message")
  @Column(updatable = false)
  @Type(type = "com.clearspend.capital.common.typedid.jpatype.TypedIdJpaType")
  private TypedId<NetworkMessageId> networkMessageId;
}
