package com.tranwall.common.data.model;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.ReadOnlyProperty;

@Getter
@Setter
@NoArgsConstructor
public class Mutable {

  @Id private UUID id = UUID.randomUUID();

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
