package com.tranwall.common.data.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.OffsetDateTime;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.ReadOnlyProperty;

@Getter
@Setter
@NoArgsConstructor
public class Immutable {

  @Id private UUID id = UUID.randomUUID();

  // Null when new so that SimpleJpaRepository::save does an insert (EntityManager::persist) without
  // a select statement (EntityManager::merge) when saving new entities
  @Column(nullable = false, updatable = false)
  @Version
  @ReadOnlyProperty
  private Long version;

  @Column(nullable = false, updatable = false)
  @CreatedDate
  @JsonIgnore
  private OffsetDateTime created = OffsetDateTime.now();
}
