package com.clearspend.capital.data.model;

import com.clearspend.capital.common.data.model.TypedMutable;
import com.clearspend.capital.common.typedid.data.LedgerAccountId;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.data.model.enums.LedgerAccountType;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.DynamicUpdate;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@RequiredArgsConstructor
@DynamicUpdate
@Slf4j
public class LedgerAccount extends TypedMutable<LedgerAccountId> {

  @NonNull
  @Enumerated(EnumType.STRING)
  private LedgerAccountType type;

  @NonNull
  @Enumerated(EnumType.STRING)
  private Currency currency;
}
