package com.clearspend.capital.data.model.embedded;

import com.clearspend.capital.common.typedid.data.ReceiptId;
import com.clearspend.capital.common.typedid.data.TypedId;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.MappedSuperclass;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
@MappedSuperclass
public class ReceiptDetails {

  @Column(columnDefinition = "uuid[]")
  @Type(type = "uuid-array")
  private Set<TypedId<ReceiptId>> receiptIds = new HashSet<>();
}
