package com.clearspend.capital.crypto.data.converter.canonicalization;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.clearspend.capital.crypto.HashUtil;
import org.junit.jupiter.api.Test;

public class EmailCanonicalizerTest {
  private final EmailCanonicalizer canonicalizer = new EmailCanonicalizer();

  @Test
  void forHash() {
    assertEquals("", canonicalizer.forHash(""));
    assertEquals("craig@gmail.com", canonicalizer.forHash("craig@gmail.com"));
    assertEquals("craigmiller@gmail.com", canonicalizer.forHash("Craig Miller@gmail.com"));
  }

  @Test
  void forEncryption() {
    assertEquals("", canonicalizer.forEncryption(""));
    assertEquals("craig@gmail.com", canonicalizer.forEncryption("craig@gmail.com"));
    assertEquals("craigmiller@gmail.com", canonicalizer.forEncryption("Craig Miller@gmail.com"));
  }

  @Test
  void getCanonicalizedHash() {
    final byte[] expected = HashUtil.calculateHash("craigmiller@gmail.com");
    final byte[] actual = canonicalizer.getCanonicalizedHash("Craig Miller@gmail.com");
    assertArrayEquals(expected, actual);
  }
}
