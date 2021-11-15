package com.tranwall.capital.data.model.embedded;

import com.tranwall.capital.data.model.enums.MerchantType;
import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.MappedSuperclass;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@NoArgsConstructor // required for Hibernate but shouldn't be used otherwise
@AllArgsConstructor
@MappedSuperclass
public class MerchantDetails {

  private String name;

  @Enumerated(EnumType.STRING)
  private MerchantType type;

  private String merchantNumber;

  private Integer merchantCategoryCode;
}
