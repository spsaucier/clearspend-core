package com.clearspend.capital.data.model.embedded;

import com.clearspend.capital.common.typedid.data.HoldId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.data.model.Hold;
import java.time.OffsetDateTime;
import javax.persistence.Embeddable;
import javax.persistence.JoinColumn;
import javax.persistence.MappedSuperclass;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.hibernate.annotations.Type;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
@MappedSuperclass
public class HoldDetails {

  @JoinColumn(referencedColumnName = "id", table = "hold")
  @Type(type = "com.clearspend.capital.common.typedid.jpatype.TypedIdJpaType")
  private TypedId<HoldId> id;

  @NonNull private OffsetDateTime expirationDate;

  public static HoldDetails of(Hold hold) {
    return new HoldDetails(hold.getId(), hold.getExpirationDate());
  }
}
