package com.clearspend.capital.common.data.model;

import com.clearspend.capital.common.typedid.data.TypedId;
import java.time.OffsetDateTime;
import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Type;

@Data
@MappedSuperclass
@EqualsAndHashCode
public abstract class TypedImmutable<T> {

  @Id
  @Type(type = "com.clearspend.capital.common.typedid.jpatype.TypedIdJpaType")
  private TypedId<T> id = new TypedId<>();

  @Column(nullable = false, updatable = false)
  private OffsetDateTime created;

  @PrePersist
  private void onPrePersist() {
    setCreated(OffsetDateTime.now());
  }

  @PreUpdate
  private void onPreUpdate() {
    throw new UnsupportedOperationException(
        String.format("Immutable object %s cannot be updated in db", this.getClass().getName()));
  }
}
