package com.tranwall.crypto;

import static org.apache.commons.codec.digest.DigestUtils.getSha512Digest;
import static org.apache.commons.codec.digest.DigestUtils.sha256;
import static org.apache.commons.lang3.StringUtils.stripAccents;

import com.google.common.io.BaseEncoding;
import com.tranwall.crypto.data.ByteString;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

@Slf4j
@UtilityClass
public class HashUtil {

  private final SecureRandom random;
  private final String randomAlgorithm = "SHA1PRNG";
  private static final Pattern furtherNormalizationPattern =
      Pattern.compile("(?:\\p{IsPunctuation}|\\p{IsWhiteSpace})+");

  static {
    try {
      random = SecureRandom.getInstance(randomAlgorithm);
    } catch (NoSuchAlgorithmException e) {
      throw new AssertionError(e);
    }
  }

  public byte[] calculateHash(String clearText) {
    if (StringUtils.isEmpty(clearText)) {
      return null;
    }

    return calculateHash(clearText.getBytes());
  }

  public byte[] calculateHash(byte[] clearText) {
    return calculateHash256(clearText, 2, 32);
  }

  public byte[] calculateHash256(byte[] clearText, int hashPrefixLength, int hashLength) {
    // a null or empty input results in a null output
    if (clearText == null || clearText.length == 0) {
      return null;
    }

    if (clearText[0] == (byte) 0x68
        && clearText[1] == (byte) 0x3a
        && clearText.length == hashPrefixLength + hashLength) {
      // a little hacky, but if the value is already hashed don't do that again. Note: that this is
      // due to cases where this method is called more than once by JPA/Hibernate
      return clearText;
    }

    // hash the input
    byte[] encodedHash = sha256(clearText);
    Assert.isTrue(
        encodedHash.length == hashLength,
        String.format(
            "unexpected hash length. Expected %d, got %d", hashLength, encodedHash.length));

    // create result array to hold the prefix and encodedHash
    byte[] result = new byte[hashPrefixLength + hashLength];

    // add prefix
    result[0] = (byte) 0x68;
    result[1] = (byte) 0x3a;

    // add encodedHash
    System.arraycopy(encodedHash, 0, result, hashPrefixLength, hashLength);

    return result;
  }

  // calculates a hash with Sha512 for the given input
  public String calculateHash512NoPrefix(String clearText, String salt) {

    if (StringUtils.isEmpty(clearText)) {
      // a null input results in a null output
      return null;
    }

    MessageDigest digest512 = getSha512Digest();
    digest512.update(salt.getBytes(StandardCharsets.UTF_8));

    // hash the input
    byte[] encodedHash = digest512.digest(clearText.getBytes(StandardCharsets.UTF_8));

    StringBuilder sb = new StringBuilder();
    for (byte hash : encodedHash) {
      sb.append(Integer.toString((hash & 0xff) + 0x100, 16).substring(1));
    }

    return sb.toString();
  }

  public static ByteString normalizedHash(@NonNull String name) {
    return ByteString.of(
        sha256(
            furtherNormalizationPattern
                .matcher(stripAccents(name))
                .replaceAll("")
                .toLowerCase(Locale.ROOT)));
  }

  public boolean hashAndCompareTo(byte[] hashThis, byte[] andCompareToThisHash) {
    return Objects.deepEquals(calculateHash(hashThis), andCompareToThisHash);
  }

  public String generateKeyString() {
    return BaseEncoding.base16().encode(generateKey());
  }

  public String generateKeyString(int size) {
    return BaseEncoding.base16().encode(generateKey(size));
  }

  public byte[] generateKey() {
    return generateKey(32);
  }

  public byte[] generateKey(int size) {
    byte[] key = new byte[size];
    random.nextBytes(key);

    return key;
  }

  public byte[] generateKey(String randomAlgorithm, int size) throws NoSuchAlgorithmException {
    if (randomAlgorithm.equals(HashUtil.randomAlgorithm)) {
      return generateKey(size);
    }
    SecureRandom random = SecureRandom.getInstance(randomAlgorithm);
    byte[] key = new byte[size];
    random.nextBytes(key);

    return key;
  }
}
