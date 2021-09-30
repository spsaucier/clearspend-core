package com.tranwall.capital.common.data.model;

import java.time.OffsetDateTime;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.annotation.ReadOnlyProperty;

@Getter
@Setter
@MappedSuperclass
@NoArgsConstructor
public class Mutable {

  @Id
  @GeneratedValue(generator = "UUID")
  @GenericGenerator(
      name = "UUID",
      strategy = "org.hibernate.id.UUIDGenerator"
  )
  private UUID id;

  @Version
  @ReadOnlyProperty
  @Setter(AccessLevel.PROTECTED)
  private Long version;

  @Column(updatable = false)
  private OffsetDateTime created;

  private OffsetDateTime updated;

  @PrePersist
  protected void onPrePersist() {
    setCreated(OffsetDateTime.now());
    setUpdated(getCreated());
  }

  @PreUpdate
  protected void onPreUpdate() {
    setUpdated(OffsetDateTime.now());
  }
}
