package com.tranwall.capital.data.model;

import com.tranwall.capital.common.data.model.Mutable;
import com.tranwall.capital.data.model.enums.FundingType;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
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
public class Program extends Mutable {

  @NonNull private String name;

  @NonNull private String bin;

  @NonNull
  @Enumerated(EnumType.STRING)
  private FundingType fundingType;
}
