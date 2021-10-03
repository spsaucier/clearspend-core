package com.tranwall.capital.crypto;

import static org.assertj.core.api.Assertions.assertThat;

import com.tranwall.capital.crypto.HashUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

@Slf4j
class HashUtilTest {

  @Test
  void calculateHash() {
  }

  @Test
  public void testCrypto_alreadyHashed() {
    byte[] hash = "h:IAmAlreadyHashed".getBytes();
    assertThat(hash).isEqualTo(HashUtil.calculateHash(hash));
  }

  @Test
  public void testCrypto_notAlreadyHashed() {
    byte[] hash = "IAmNotAlreadyHashed".getBytes();
    assertThat(hash).isNotEqualTo(HashUtil.calculateHash(hash));
  }

  @Test
  void testCalculateHash() {
  }

  @Test
  void calculateHash256() {
  }

  @Test
  void calculateHash512NoPrefix() {
  }

  @Test
  void normalizedHash() {
  }

  @Test
  void hashAndCompareTo() {
  }

  @Test
  void generateKeyString() {
  }

  @Test
  void testGenerateKeyString() {
  }

  @Test
  void generateKey() {
  }

  @Test
  void testGenerateKey() {
  }

  @Test
  void testGenerateKey1() {
  }
}