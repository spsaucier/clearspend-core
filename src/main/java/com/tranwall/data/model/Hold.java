package com.tranwall.data.model;

import com.tranwall.common.data.model.Amount;
import com.tranwall.common.data.model.Mutable;
import com.tranwall.data.model.enums.AdjustmentType;
import com.tranwall.data.model.enums.HoldStatus;
import java.time.OffsetDateTime;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.DynamicUpdate;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@DynamicUpdate
@Slf4j
public class Hold extends Mutable {

  @NonNull
  @JoinColumn(referencedColumnName = "id", table = "account")
  @Column(updatable = false)
  private UUID accountId;

  @NonNull
  @Enumerated(EnumType.STRING)
  private HoldStatus status;

  @NonNull @Embedded private Amount amount;

  private OffsetDateTime expirationDate;
}
