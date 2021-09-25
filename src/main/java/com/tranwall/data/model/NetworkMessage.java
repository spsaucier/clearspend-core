package com.tranwall.data.model;

import com.tranwall.common.data.model.Amount;
import com.tranwall.common.data.model.Mutable;
import com.tranwall.crypto.data.model.embedded.NullableEncryptedString;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
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
public class NetworkMessage extends Mutable {

  @NonNull
  @JoinColumn(referencedColumnName = "id", table = "business")
  @Column(updatable = false)
  private UUID businessId;

  @NonNull
  @JoinColumn(referencedColumnName = "id", table = "allocation")
  @Column(updatable = false)
  private UUID allocationId;

  @JoinColumn(referencedColumnName = "id", table = "card")
  @Column(updatable = false)
  private UUID cardId;

  @JoinColumn(referencedColumnName = "id", table = "hold")
  @Column(updatable = false)
  private UUID holdId;

  @JoinColumn(referencedColumnName = "id", table = "adjustment")
  @Column(updatable = false)
  private UUID adjustmentId;

  @Embedded
  private NullableEncryptedString cardNumber;

  @NonNull
  @Column(updatable = false)
  private UUID networkMessageGroupId;

  @Embedded
  @NonNull
  private Amount amount;
}
