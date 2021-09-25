package com.tranwall.crypto;

import com.google.common.io.BaseEncoding;
import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Objects;
import lombok.Data;
import lombok.NonNull;
import org.springframework.security.crypto.keygen.BytesKeyGenerator;
import org.springframework.security.crypto.keygen.KeyGenerators;
import org.springframework.util.Assert;

@Data
public class PasswordAndSalt {
  // static variables to avoid having to construct them on every operation
  private static BytesKeyGenerator generator = KeyGenerators.secureRandom(16);
  static MessageDigest digest;

  static {
    try {
      digest = MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException e) {
      throw new MissingAlgorithmException("SHA-256 algorithm not found");
    }
  }

  // Base 16 encode strings as that's what the encryptors need as input
  private String password;
  private String salt;

  // generate a new password and salt
  public PasswordAndSalt() {
    password = BaseEncoding.base16().encode(generator.generateKey());
    salt = BaseEncoding.base16().encode(generator.generateKey());
  }

  // passwordAndSalt is a string separated by a : that contains the password and salt values that
  // are base16 encoded
  public PasswordAndSalt(@NonNull String passwordAndSalt) {
    String[] parts = passwordAndSalt.split(":");
    Assert.isTrue(
        parts.length == 2,
        "invalid password and salt value (missing separator): " + passwordAndSalt);
    password = parts[0];
    Assert.isTrue(password.length() == 32, "invalid password length: " + password.length());
    salt = parts[1];
    Assert.isTrue(salt.length() == 32, "invalid salt length: " + salt.length());
  }

  // passwordAndSalt is the concatenation of the password and salt byte[] each of which is 16 bytes
  public PasswordAndSalt(byte[] passwordAndSalt) {
    Assert.isTrue(
        passwordAndSalt.length == 32, "invalid binary representation of password and salt");
    password = BaseEncoding.base16().encode(Arrays.copyOfRange(passwordAndSalt, 0, 16));
    Assert.isTrue(password.length() == 32, "invalid representation of password");
    salt = BaseEncoding.base16().encode(Arrays.copyOfRange(passwordAndSalt, 16, 32));
    Assert.isTrue(salt.length() == 32, "invalid representation of salt");
  }

  // generate a hash value suitable for storing in the DB and to be used as a comparison with other
  // keys
  public String getHash() {
    return BaseEncoding.base64().encode(digest.digest(toString().getBytes()));
  }

  // convert the password and salt into a single byte[] used to encrypt a DEK
  public byte[] toBytes() {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    outputStream.writeBytes(BaseEncoding.base16().decode(password));
    outputStream.writeBytes(BaseEncoding.base16().decode(salt));
    byte[] bytes = outputStream.toByteArray();
    Assert.isTrue(bytes.length == 32, "invalid binary representation of password and salt");
    return bytes;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PasswordAndSalt that = (PasswordAndSalt) o;
    return Objects.equals(password, that.password) && Objects.equals(salt, that.salt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(password, salt);
  }

  @Override
  // custom toString method so we can generate human readable keys
  public String toString() {
    return password + ':' + salt;
  }
}
