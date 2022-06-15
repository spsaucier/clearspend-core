package com.clearspend.capital.crypto.data.converter.canonicalization;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.clearspend.capital.crypto.HashUtil;
import org.junit.jupiter.api.Test;

public class NameCanonicalizerTest {
  private final NameCanonicalizer canonicalizer = new NameCanonicalizer();

  @Test
  void forHash() {
    assertEquals("", canonicalizer.forHash(""));
    assertEquals("craig miller", canonicalizer.forHash("Crãig  Miller"));
  }

  @Test
  void forEncryption() {
    assertEquals("", canonicalizer.forEncryption(""));
    assertEquals("Craig Miller", canonicalizer.forEncryption("Craig  Miller"));
  }

  @Test
  void getCanonicalizedHash() {
    final byte[] expected = HashUtil.calculateHash("craig miller");
    final byte[] actual = canonicalizer.getCanonicalizedHash("Crãig  Miller");
    assertArrayEquals(expected, actual);
  }
}
