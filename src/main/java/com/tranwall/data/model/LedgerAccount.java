package com.tranwall.data.model;

import com.tranwall.common.data.model.Mutable;
import com.tranwall.data.model.enums.Currency;
import com.tranwall.data.model.enums.LedgerAccountType;
import javax.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.DynamicUpdate;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@DynamicUpdate
@Slf4j
public class LedgerAccount extends Mutable {

  @NonNull
  private LedgerAccountType type;

  @NonNull
  private Currency currency;
}
