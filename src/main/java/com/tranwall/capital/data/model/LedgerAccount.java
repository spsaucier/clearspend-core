package com.tranwall.capital.data.model;

import com.tranwall.capital.common.data.model.Mutable;
import com.tranwall.capital.data.model.enums.Currency;
import com.tranwall.capital.data.model.enums.LedgerAccountType;
import javax.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.DynamicUpdate;

@Entity
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@DynamicUpdate
@Slf4j
public class LedgerAccount extends Mutable {

  @NonNull private LedgerAccountType type;

  @NonNull private Currency currency;
}
