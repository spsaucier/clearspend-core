package com.clearspend.capital.data.model;

import com.clearspend.capital.common.data.model.TypedMutable;
import com.clearspend.capital.common.typedid.data.StripeWebhookLogId;
import javax.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.DynamicUpdate;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@DynamicUpdate
@Slf4j
public class StripeWebhookLog extends TypedMutable<StripeWebhookLogId> {

  private String eventType;

  private String request;

  private String error;

  private Long processingTimeMs;
}
