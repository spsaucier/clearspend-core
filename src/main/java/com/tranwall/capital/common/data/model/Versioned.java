package com.tranwall.capital.common.data.model;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Version;
import lombok.Data;
import org.hibernate.annotations.OptimisticLocking;
import org.springframework.data.annotation.ReadOnlyProperty;

@Data
@OptimisticLocking
@MappedSuperclass
public class Versioned {

  // Null when new so that SimpleJpaRepository::save does an insert (EntityManager::persist) without
  // a select statement (EntityManager::merge) when saving new entities
  @Version @ReadOnlyProperty private Long version;

  @Column(updatable = false)
  private OffsetDateTime created =
      OffsetDateTime.now().truncatedTo(ChronoUnit.MICROS); // To match column definition datetime(6)

  private OffsetDateTime updated;

  @PrePersist
  public void onPrePersist() {
    setUpdated(getCreated());
  }

  @PreUpdate
  public void onPreUpdate() {
    setUpdated(OffsetDateTime.now().truncatedTo(ChronoUnit.MICROS));
  }
}
