package com.clearspend.capital.data.model;

import com.clearspend.capital.common.data.model.TypedMutable;
import com.clearspend.capital.common.typedid.data.DeviceRegistrationId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.vladmihalcea.hibernate.type.array.StringArrayType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@DynamicUpdate
@Slf4j
@Table(name = "device_registration")
@TypeDefs(@TypeDef(name = "string-array", typeClass = StringArrayType.class))
public class DeviceRegistration extends TypedMutable<DeviceRegistrationId> {

  @NonNull
  @Column(updatable = false)
  @Type(type = "com.clearspend.capital.common.typedid.jpatype.TypedIdJpaType")
  private TypedId<UserId> userId;

  @NonNull
  @Type(type = "string-array")
  @Column(name = "device_ids", columnDefinition = "text[]")
  private String[] deviceIds;
}
