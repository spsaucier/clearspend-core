package com.tranwall.capital.crypto.data.model;

import com.tranwall.capital.common.data.model.Immutable;
import javax.persistence.Column;
import javax.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class Key extends Immutable {
  // table that holds a list of all keyHashes that the application is and has been using to encrypt
  // data

  // a numeric key reference that's used as part of the encrypted data to determine which key was
  // used
  @NonNull private Integer keyRef;

  @Column(length = 50)
  // the hash of the key (the actual key is not stored in the database aside from the DEK (data
  // encryption key) which is first encrypted with the KEK (key encryption key))
  private byte[] keyHash;
}
