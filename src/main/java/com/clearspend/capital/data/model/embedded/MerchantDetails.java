package com.clearspend.capital.data.model.embedded;

import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.data.model.enums.Country;
import com.clearspend.capital.data.model.enums.MccGroup;
import com.clearspend.capital.data.model.enums.MerchantType;
import java.math.BigDecimal;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
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

  private String statementDescriptor;

  @Embedded private Amount amount;

  private String codatSupplierName;

  private String codatSupplierId;

  @Enumerated(EnumType.STRING)
  private MerchantType type;

  private String merchantNumber;

  private Integer merchantCategoryCode;

  @Enumerated(EnumType.STRING)
  private MccGroup merchantCategoryGroup;

  // path only
  private String logoUrl;

  private BigDecimal latitude;

  private BigDecimal longitude;

  @Enumerated(EnumType.STRING)
  private Country country;
}
