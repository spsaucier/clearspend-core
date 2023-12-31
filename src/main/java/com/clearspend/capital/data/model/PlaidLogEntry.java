package com.clearspend.capital.data.model;

import com.clearspend.capital.common.data.model.TypedImmutable;
import com.clearspend.capital.common.masking.annotation.Sensitive;
import com.clearspend.capital.common.typedid.data.PlaidLogEntryId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.crypto.data.model.embedded.EncryptedString;
import com.clearspend.capital.data.model.enums.PlaidResponseType;
import java.time.OffsetDateTime;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Type;

@Entity
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@SuppressWarnings("MissingOverride")
@NoArgsConstructor
@RequiredArgsConstructor
@DynamicUpdate
@Slf4j
public class PlaidLogEntry extends TypedImmutable<PlaidLogEntryId> {

  public PlaidLogEntry(
      TypedId<BusinessId> businessId, String message, PlaidResponseType plaidResponseType) {
    this(businessId, new EncryptedString(message), plaidResponseType);
  }

  @NonNull
  @JoinColumn(referencedColumnName = "id", table = "business")
  @Column(updatable = false)
  @Type(type = "com.clearspend.capital.common.typedid.jpatype.TypedIdJpaType")
  private TypedId<BusinessId> businessId;

  @Column(name = "created", insertable = false, updatable = false)
  private OffsetDateTime created;

  @Sensitive @NonNull @Embedded private EncryptedString message;

  @NonNull private PlaidResponseType plaidResponseType;
}
