package com.clearspend.capital.data.model;

import com.clearspend.capital.common.data.model.TypedMutable;
import com.clearspend.capital.common.typedid.data.NetworkMessageId;
import com.clearspend.capital.common.typedid.data.StripeWebhookLogId;
import com.clearspend.capital.common.typedid.data.TypedId;
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
public class StripeWebhookLog extends TypedMutable<StripeWebhookLogId> {

  private String stripeEventRef;

  private String eventType;

  private String request;

  private String error;

  private Long processingTimeMs;

  @JoinColumn(referencedColumnName = "id", table = "network_message")
  @Column(updatable = false)
  @Type(type = "com.clearspend.capital.common.typedid.jpatype.TypedIdJpaType")
  private TypedId<NetworkMessageId> networkMessageId;
}
