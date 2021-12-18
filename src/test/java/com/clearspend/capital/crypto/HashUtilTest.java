package com.clearspend.capital.crypto;

import static org.apache.commons.codec.digest.DigestUtils.sha256;
import static org.assertj.core.api.Assertions.assertThat;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

@Slf4j
class HashUtilTest {

  @Test
  void calculateHash() {}

  @Test
  public void testCrypto_alreadyHashed() {
    byte[] hash = new byte[2 + 32];
    hash[0] = (byte) 0x68;
    hash[1] = (byte) 0x3a;
    System.arraycopy(sha256("IAmAlreadyHashed"), 0, hash, 2, 32);
    assertThat(hash).isEqualTo(HashUtil.calculateHash(hash));
  }

  @Test
  public void testCrypto_notAlreadyHashed() {
    byte[] hash = "IAmNotAlreadyHashed".getBytes();
    assertThat(hash).isNotEqualTo(HashUtil.calculateHash(hash));
  }

  @Test
  void testCalculateHash() {}

  @Test
  void calculateHash256() {}

  @Test
  void calculateHash512NoPrefix() {}

  @Test
  void normalizedHash() {}

  @Test
  void hashAndCompareTo() {}

  @Test
  void generateKeyString() {}

  @Test
  void testGenerateKeyString() {}

  @Test
  void generateKey() {}

  @Test
  void testGenerateKey() {}

  @Test
  void testGenerateKey1() {}
}
