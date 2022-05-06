package com.clearspend.capital.data.model;

import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.common.data.model.TypedMutable;
import com.clearspend.capital.common.data.type.TypedIdSetType;
import com.clearspend.capital.common.typedid.data.AccountId;
import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.ReceiptId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
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
@TypeDefs({@TypeDef(name = "uuid-set", typeClass = TypedIdSetType.class)})
public class Receipt extends TypedMutable<ReceiptId> implements MultiOwnerPermissionable {

  public Receipt(final TypedId<BusinessId> businessid, final TypedId<UserId> uploadUserId) {
    this.businessId = businessid;
    this.uploadUserId = uploadUserId;
  }

  @NonNull
  @JoinColumn(referencedColumnName = "id", table = "business")
  @Column(updatable = false)
  @Type(type = "com.clearspend.capital.common.typedid.jpatype.TypedIdJpaType")
  private TypedId<BusinessId> businessId;

  @NonNull
  @JoinColumn(referencedColumnName = "id", table = "users")
  @Column(updatable = false)
  @Type(type = "com.clearspend.capital.common.typedid.jpatype.TypedIdJpaType")
  private TypedId<UserId> uploadUserId;

  @NonNull
  @Column(columnDefinition = "uuid[]")
  @Type(type = "uuid-set")
  private Set<TypedId<UserId>> linkUserIds = new HashSet<>();

  public Set<TypedId<UserId>> getLinkUserIds() {
    return Collections.unmodifiableSet(linkUserIds);
  }

  @JoinColumn(referencedColumnName = "id", table = "allocation")
  @Type(type = "com.clearspend.capital.common.typedid.jpatype.TypedIdJpaType")
  private TypedId<AllocationId> allocationId;

  @JoinColumn(referencedColumnName = "id", table = "account")
  @Type(type = "com.clearspend.capital.common.typedid.jpatype.TypedIdJpaType")
  private TypedId<AccountId> accountId;

  @Embedded private Amount amount;

  private String path;

  private String contentType;

  private boolean linked;

  @Override
  public Set<TypedId<UserId>> getOwnerIds() {
    final Set<TypedId<UserId>> ownerIds = new HashSet<>(linkUserIds);
    ownerIds.add(uploadUserId);
    return ownerIds;
  }

  public void addLinkUserId(@Nullable final TypedId<UserId> linkUserId) {
    Optional.ofNullable(linkUserId).ifPresent(linkUserIds::add);
  }

  public void removeLinkUserId(@Nullable final TypedId<UserId> linkUserId) {
    Optional.ofNullable(linkUserId).ifPresent(linkUserIds::remove);
  }
}
