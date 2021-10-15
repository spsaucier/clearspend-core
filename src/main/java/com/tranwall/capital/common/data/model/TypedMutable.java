package com.tranwall.capital.common.data.model;

import com.tranwall.capital.common.typedid.data.TypedId;
import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Type;

@Data
@MappedSuperclass
@EqualsAndHashCode(callSuper = true)
public abstract class TypedMutable<T> extends Versioned {

  @Column(columnDefinition = "binary(16)")
  @Id
  @Type(type = "com.tranwall.capital.common.typedid.jpatype.TypedIdJpaType")
  //  @Type(type = "org.hibernate.type.descriptor.java.UUIDTypeDescriptor")
  private TypedId<T> id = new TypedId<>();
}
