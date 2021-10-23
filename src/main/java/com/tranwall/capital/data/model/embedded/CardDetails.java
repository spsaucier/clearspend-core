package com.tranwall.capital.data.model.embedded;

import com.tranwall.capital.common.masking.annotation.Sensitive;
import com.tranwall.capital.crypto.data.model.embedded.RequiredEncryptedStringWithHash;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.MappedSuperclass;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@NoArgsConstructor // required for Hibernate but shouldn't be used otherwise
@AllArgsConstructor
@MappedSuperclass
public class CardDetails {

  @Sensitive private String number;

  @Sensitive @Embedded private RequiredEncryptedStringWithHash owner;
}
