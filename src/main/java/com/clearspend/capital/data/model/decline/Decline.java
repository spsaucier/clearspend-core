package com.clearspend.capital.data.model.decline;

import com.clearspend.capital.common.GlobalObjectMapper;
import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.common.data.model.TypedImmutable;
import com.clearspend.capital.common.typedid.data.AccountId;
import com.clearspend.capital.common.typedid.data.CardId;
import com.clearspend.capital.common.typedid.data.DeclineId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.util.function.ThrowableFunctions;
import com.fasterxml.jackson.databind.JavaType;
import java.util.List;
import java.util.Optional;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.Transient;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.SneakyThrows;
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
@DynamicUpdate
@Slf4j
public class Decline extends TypedImmutable<DeclineId> {

  private static JavaType getDetailsType() {
    return GlobalObjectMapper.get()
        .getTypeFactory()
        .constructParametricType(List.class, DeclineDetails.class);
  }

  @NonNull
  @JoinColumn(referencedColumnName = "id", table = "business")
  @Column(updatable = false)
  @Type(type = "com.clearspend.capital.common.typedid.jpatype.TypedIdJpaType")
  private TypedId<BusinessId> businessId;

  @NonNull
  @JoinColumn(referencedColumnName = "id", table = "account")
  @Column(updatable = false)
  @Type(type = "com.clearspend.capital.common.typedid.jpatype.TypedIdJpaType")
  private TypedId<AccountId> accountId;

  @NonNull
  @JoinColumn(referencedColumnName = "id", table = "card")
  @Column(updatable = false)
  @Type(type = "com.clearspend.capital.common.typedid.jpatype.TypedIdJpaType")
  private TypedId<CardId> cardId;

  @NonNull @Embedded private Amount amount;

  @Transient private List<DeclineDetails> details;

  @NonNull
  @Type(type = "json")
  @Column(name = "details", columnDefinition = "jsonb")
  private String detailsString;

  @SneakyThrows
  public Decline(
      final TypedId<BusinessId> businessId,
      final TypedId<AccountId> accountId,
      final TypedId<CardId> cardId,
      final Amount amount,
      final List<DeclineDetails> details) {
    this.businessId = businessId;
    this.accountId = accountId;
    this.cardId = cardId;
    this.amount = amount;
    this.details = details;
    this.detailsString = GlobalObjectMapper.get().writeValueAsString(details);
  }

  public List<DeclineDetails> getDetails() {
    return Optional.ofNullable(this.details)
        .orElseGet(
            ThrowableFunctions.sneakyThrows(
                () -> {
                  this.details =
                      GlobalObjectMapper.get().readValue(this.detailsString, getDetailsType());
                  return this.details;
                }));
  }
}
