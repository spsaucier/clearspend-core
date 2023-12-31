package com.clearspend.capital.data.model;

import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.common.data.model.TypedMutable;
import com.clearspend.capital.common.typedid.data.AccountActivityId;
import com.clearspend.capital.common.typedid.data.AccountId;
import com.clearspend.capital.common.typedid.data.AdjustmentId;
import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.data.audit.AccountActivityEntityEventListener;
import com.clearspend.capital.data.model.decline.DeclineDetails;
import com.clearspend.capital.data.model.embedded.AccountingDetails;
import com.clearspend.capital.data.model.embedded.AllocationDetails;
import com.clearspend.capital.data.model.embedded.BankAccountDetails;
import com.clearspend.capital.data.model.embedded.CardDetails;
import com.clearspend.capital.data.model.embedded.ExpenseDetails;
import com.clearspend.capital.data.model.embedded.HoldDetails;
import com.clearspend.capital.data.model.embedded.MerchantDetails;
import com.clearspend.capital.data.model.embedded.PaymentDetails;
import com.clearspend.capital.data.model.embedded.ReceiptDetails;
import com.clearspend.capital.data.model.embedded.UserDetails;
import com.clearspend.capital.data.model.enums.AccountActivityIntegrationSyncStatus;
import com.clearspend.capital.data.model.enums.AccountActivityStatus;
import com.clearspend.capital.data.model.enums.AccountActivityType;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
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
@NoArgsConstructor
@RequiredArgsConstructor
@DynamicUpdate
@Slf4j
@EntityListeners(AccountActivityEntityEventListener.class)
public class AccountActivity extends TypedMutable<AccountActivityId> implements Permissionable {

  @NonNull
  @JoinColumn(referencedColumnName = "id", table = "business")
  @Column(updatable = false)
  @Type(type = "com.clearspend.capital.common.typedid.jpatype.TypedIdJpaType")
  private TypedId<BusinessId> businessId;

  @JoinColumn(referencedColumnName = "id", table = "account")
  @Column(updatable = false)
  @Type(type = "com.clearspend.capital.common.typedid.jpatype.TypedIdJpaType")
  private TypedId<AccountId> accountId;

  @JoinColumn(referencedColumnName = "id", table = "adjustment")
  @Column(updatable = false)
  @Type(type = "com.clearspend.capital.common.typedid.jpatype.TypedIdJpaType")
  private TypedId<AdjustmentId> adjustmentId;

  @NonNull
  @Enumerated(EnumType.STRING)
  private AccountActivityType type;

  @NonNull
  @Enumerated(EnumType.STRING)
  private AccountActivityStatus status;

  // the time after which this record should be hidden from the user. Set to a non-null value if
  // this record should always be hidden.
  private OffsetDateTime hideAfter;

  // the time after which this record should be visible to the user. Leave null if this record
  // should always be visible.
  private OffsetDateTime visibleAfter;

  @NonNull @Embedded private AllocationDetails allocation;

  // filled for reallocation activities only. Could be a source allocation for positive amounts,
  // target allocation for negative ones
  @Embedded private AllocationDetails flipAllocation;

  @Embedded private HoldDetails hold;

  @Embedded private UserDetails user;

  @Embedded private BankAccountDetails bankAccount;

  @Embedded private MerchantDetails merchant;

  @Embedded private ExpenseDetails expenseDetails;

  @Embedded private CardDetails card;

  @Embedded private ReceiptDetails receipt;

  @Embedded private AccountingDetails accountingDetails;

  @Embedded private PaymentDetails paymentDetails;

  @NonNull private OffsetDateTime activityTime;

  @NonNull @Embedded private Amount amount;

  // the amount we received from Stripe or a copy of amount otherwise
  @NonNull @Embedded private Amount requestedAmount;

  private String notes;

  @NonNull
  @Enumerated(EnumType.STRING)
  private AccountActivityIntegrationSyncStatus integrationSyncStatus;

  private OffsetDateTime lastSyncTime;

  @Type(type = "json")
  @Column(columnDefinition = "jsonb")
  private List<DeclineDetails> declineDetails;

  @Override
  public TypedId<UserId> getOwnerId() {
    return Optional.ofNullable(user).map(UserDetails::getId).orElse(null);
  }

  @Override
  public TypedId<AllocationId> getAllocationId() {
    return allocation.getId();
  }

  @Nullable
  public TypedId<UserId> getUserDetailsId() {
    return Optional.ofNullable(user).map(UserDetails::getId).orElse(null);
  }
}
