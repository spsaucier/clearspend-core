package com.tranwall.capital.crypto.data;

import static org.apache.commons.codec.binary.Hex.encodeHexString;

import lombok.EqualsAndHashCode;
import lombok.NonNull;

/** Immutable sequence of bytes. */
@EqualsAndHashCode
public final class ByteString {

  private static final ByteString EMPTY = new ByteString(new byte[0]);

  private final byte[] value;

  private ByteString(byte[] value) {
    this.value = value.clone();
  }

  public byte[] toByteArray() {
    return value.clone();
  }

  @Override
  public String toString() {
    return "0x" + encodeHexString(value);
  }

  public static ByteString of(@NonNull byte... value) {
    return value.length == 0 ? EMPTY : new ByteString(value);
  }
}
