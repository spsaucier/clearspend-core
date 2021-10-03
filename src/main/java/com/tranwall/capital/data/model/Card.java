package com.tranwall.capital.data.model;

import com.tranwall.capital.common.data.model.Mutable;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.DynamicUpdate;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@RequiredArgsConstructor
@DynamicUpdate
@Slf4j
public class Card extends Mutable {

  @NonNull
  @JoinColumn(referencedColumnName = "id", table = "business")
  @Column(updatable = false)
  private UUID businessId;

  @NonNull
  @JoinColumn(referencedColumnName = "id", table = "allocation")
  @Column(updatable = false)
  private UUID allocationId;

  @NonNull
  @JoinColumn(referencedColumnName = "id", table = "users")
  @Column(updatable = false)
  private UUID userId;

  @NonNull
  @JoinColumn(referencedColumnName = "id", table = "bin")
  @Column(updatable = false)
  private String bin;

  @NonNull
  @JoinColumn(referencedColumnName = "id", table = "program")
  @Column(updatable = false)
  private UUID programId;

  @JoinColumn(referencedColumnName = "id", table = "accountId")
  @Column(updatable = false)
  private UUID accountId;

  private String i2cCardRef;
}
