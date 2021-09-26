package com.tranwall.crypto.data.model;

import com.tranwall.common.data.model.Immutable;
import com.tranwall.common.data.model.Mutable;
import javax.persistence.Column;
import javax.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@RequiredArgsConstructor
@Slf4j
public class Key extends Immutable {
  // table that holds a list of all keyHashes that the application is and has been using to encrypt
  // data

  // a numeric key reference that's used as part of the encrypted data to determine which key was
  // used
  @NonNull private Integer keyRef;

  @NonNull
  @Column(length = 50)
  // the hash of the key (the actual key is not stored in the database aside from the DEK (data
  // encryption key) which is first encrypted with the KEK (key encryption key))
  private byte[] keyHash;
}
