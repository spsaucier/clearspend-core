package com.clearspend.capital.data.model.embedded;

import com.clearspend.capital.common.data.type.TypedIdSetType;
import javax.persistence.Embeddable;
import javax.persistence.MappedSuperclass;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
@MappedSuperclass
@TypeDefs({@TypeDef(name = "uuid-set", typeClass = TypedIdSetType.class)})
public class AccountingDetails {

  private Boolean sentToAccounting;
}
