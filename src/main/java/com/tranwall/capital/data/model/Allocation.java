package com.tranwall.capital.data.model;

import com.tranwall.capital.common.data.model.Mutable;
import java.util.List;
import java.util.UUID;
import javax.persistence.Column;
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
public class Allocation extends Mutable {

  @NonNull
  @JoinColumn(referencedColumnName = "id", table = "business")
  @Column(updatable = false)
  private UUID businessId;

  @NonNull
  @JoinColumn(referencedColumnName = "id", table = "program")
  @Column(updatable = false)
  private UUID programId;

  @JoinColumn(referencedColumnName = "id", table = "allocation")
  @Column(updatable = false)
  private UUID parentAllocationId;

  private List<UUID> ancestorAllocationIds;

  @NonNull private String name;
}
