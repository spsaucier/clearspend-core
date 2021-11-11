package com.tranwall.capital.data.model;

import com.tranwall.capital.common.data.model.TypedMutable;
import com.tranwall.capital.common.typedid.data.ProgramId;
import com.tranwall.capital.data.model.enums.CardType;
import com.tranwall.capital.data.model.enums.FundingType;
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
public class Program extends TypedMutable<ProgramId> {

  @NonNull private String name;

  @NonNull private String bin;

  @NonNull
  @Enumerated(EnumType.STRING)
  private FundingType fundingType;

  @NonNull
  @Enumerated(EnumType.STRING)
  private CardType cardType;

  // from i2c
  @NonNull private String i2cCardProgramRef;
}
