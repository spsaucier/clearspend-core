package com.clearspend.capital.data.model.notifications;

import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.common.data.model.TypedMutable;
import com.clearspend.capital.common.data.type.TypedIdSetType;
import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.AllocationNotificationSettingId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import java.util.Set;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.ToString;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "allocation_notification_settings")
@TypeDefs({@TypeDef(name = "uuid-set", typeClass = TypedIdSetType.class)})
@NoArgsConstructor
public class AllocationNotificationsSettings extends TypedMutable<AllocationNotificationSettingId> {
  @JoinColumn(referencedColumnName = "id", table = "allocation")
  @Type(type = "com.clearspend.capital.common.typedid.jpatype.TypedIdJpaType")
  @NonNull
  private TypedId<AllocationId> allocationId;

  private boolean lowBalance;
  @NonNull @Embedded private Amount lowBalanceLevel;

  @NonNull
  @Type(type = "uuid-set")
  @Column(columnDefinition = "uuid[]")
  private Set<TypedId<UserId>> recipients;

  public AllocationNotificationsSettings(
      @NonNull final TypedId<AllocationId> allocationId,
      final boolean lowBalance,
      @NonNull final Amount lowBalanceLevel,
      @NonNull final Set<TypedId<UserId>> recipients) {
    this.allocationId = allocationId;
    this.lowBalance = lowBalance;
    this.lowBalanceLevel = lowBalanceLevel;
    this.recipients = recipients;
  }
}
