package com.clearspend.capital.crypto.data.converter.canonicalization;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.clearspend.capital.crypto.HashUtil;
import org.junit.jupiter.api.Test;

public class PhoneCanonicalizerTest {
  private PhoneCanonicalizer canonicalizer = new PhoneCanonicalizer();

  @Test
  void forHash() {
    assertEquals("", canonicalizer.forHash(""));
    assertEquals("+12345678901", canonicalizer.forHash("234-567-8901"));
    assertEquals("+12345678901", canonicalizer.forHash("1-234-567-8901"));
  }

  @Test
  void forEncryption() {
    assertEquals("", canonicalizer.forEncryption(""));
    assertEquals("+12345678901", canonicalizer.forEncryption("234-567-8901"));
    assertEquals("+12345678901", canonicalizer.forEncryption("1-234-567-8901"));
  }

  @Test
  void getCanonicalizedHash() {
    final byte[] expected = HashUtil.calculateHash("+12345678901");
    final byte[] actual = canonicalizer.getCanonicalizedHash("1-234-567-8901");
    assertArrayEquals(expected, actual);
  }
}
