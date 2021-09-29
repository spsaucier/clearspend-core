package com.tranwall.data.model;

import com.tranwall.common.data.model.Address;
import com.tranwall.common.data.model.Mutable;
import com.tranwall.common.masking.annotation.Sensitive;
import com.tranwall.crypto.data.model.embedded.NullableEncryptedString;
import com.tranwall.crypto.data.model.embedded.RequiredEncryptedString;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
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
public class Employee extends Mutable {

  @NonNull
  @JoinColumn(referencedColumnName = "id", table = "business")
  @Column(updatable = false)
  private UUID businessId;

  @Sensitive @Embedded private NullableEncryptedString firstName;

  @Sensitive @Embedded private NullableEncryptedString lastName;

  @Embedded private Address address;

  @Sensitive @Embedded private RequiredEncryptedString email;

  @Sensitive @Embedded private RequiredEncryptedString phone;

  // link to FusionAuth
  private String subjectRef;
}
