package com.clearspend.capital.common.data.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.OffsetDateTime;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@MappedSuperclass
@NoArgsConstructor
public class Immutable {

  @Id private UUID id = UUID.randomUUID();

  @Column(nullable = false, updatable = false)
  @JsonIgnore
  private OffsetDateTime created;

  @PrePersist
  private void prePersist() {
    setCreated(OffsetDateTime.now());
  }

  @PreUpdate
  private void preUpdate() {
    throw new UnsupportedOperationException(
        String.format("Immutable object %s cannot be updated in db", this.getClass().getName()));
  }
}
