package com.clearspend.capital.crypto;

import static java.lang.Byte.parseByte;

import com.clearspend.capital.crypto.data.model.Key;
import com.clearspend.capital.crypto.data.repository.KeyRepository;
import com.clearspend.capital.crypto.utils.VarInt;
import com.google.common.base.Splitter;
import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

@Component
@Slf4j
public class Crypto {

  static String envPrefix = "clearspend.encryption.key.aes.";
  private static final int maxKeys = 1000;
  private static final String keyDelimiter = "\\|";
  private final HashMap<Integer, byte[]> keyMap = new HashMap<>();

  byte[] currentKey;
  int currentKeyRef;
  private final byte aesFormatVersion = parseByte("0");
  public static final String AES_CFB_NO_PADDING = "AES/CFB/NoPadding";
  public static final String SHA_1_PRNG = "SHA1PRNG";
  public static final String HMAC_SHA_512 = "HmacSHA512";
  public static final String AES = "AES";
  private SecureRandom random;
  private int ivLength = 0;

  @Autowired
  public Crypto(Environment env, KeyRepository keyRepository) {
    try {
      Cipher cipher = Cipher.getInstance(AES_CFB_NO_PADDING);
      ivLength = cipher.getBlockSize();
      random = SecureRandom.getInstance(SHA_1_PRNG);
    } catch (NoSuchPaddingException | NoSuchAlgorithmException e) {
      log.error("Weak security. Bailing because SHA-256 is not available");
      System.exit(-1);
    }

    // load existing keys
    // variables used to load the keys
    int nextKeyRef = 0;
    HashMap<String, Integer> existingKeys = new HashMap<>();
    for (Key key : keyRepository.findAll()) {
      existingKeys.put(Base64.getEncoder().encodeToString(key.getKeyHash()), key.getKeyRef());
      if (nextKeyRef < key.getKeyRef()) {
        nextKeyRef = key.getKeyRef() + 1;
      }
    }

    // read all the environment
    // first the "current" key
    String currentKeyString = env.getProperty(envPrefix + "current");
    Assert.isTrue(Strings.isNotBlank(currentKeyString), "current aes key is null or empty");
    currentKey = Base64.getDecoder().decode(currentKeyString);

    // then numbered keys
    record EnvironmentKey(byte[] key, String name) {}

    Map<String, EnvironmentKey> environmentKeys = new LinkedHashMap<>();
    Loop:
    for (int i = 0; i < maxKeys; i++) {
      String envString = env.getProperty(envPrefix + i);
      Assert.isTrue(StringUtils.isNotBlank(envString), "invalid key: " + i);

      List<String> envParts = Splitter.onPattern(keyDelimiter).splitToList(envString);
      HashMap<String, String> replacementKeys = new HashMap<>();
      switch (envParts.size()) {
        case 2:
          // if we have 2 keys add to the list of replacement keys, they fall through to the single
          // key case
          replacementKeys.put(envParts.get(0), envParts.get(1));
          // fall through
        case 1:
          // add either one or two keys
          for (String keyString : envParts) {
            byte[] key = Base64.getDecoder().decode(keyString);
            EnvironmentKey environmentKey = new EnvironmentKey(key, String.valueOf(i));

            if (environmentKeys.containsKey(keyString)) {
              throw new RuntimeException("Duplicate key " + i);
            }
            environmentKeys.put(keyString, environmentKey);

            if (Arrays.equals(currentKey, key)) {
              // the current index key matches the current key
              break Loop;
            }
          }
          break;
        default:
          // we have an invalid number of keys for this environment variable
          throw new RuntimeException("Invalid key found for " + i);
      }
    }
    // finally, "next" key
    String nextKeyString = env.getProperty(envPrefix + "next");
    org.springframework.util.Assert.isTrue(
        Strings.isNotBlank(nextKeyString), "next aes key is null or empty");
    EnvironmentKey nextKey = new EnvironmentKey(Base64.getDecoder().decode(nextKeyString), "next");
    if (environmentKeys.containsKey(nextKeyString)) {
      throw new RuntimeException("Duplicate next key");
    }
    environmentKeys.put(nextKeyString, nextKey);

    // at a minimum we should have 2 keys, the "current" and  the "next" keys
    org.springframework.util.Assert.isTrue(
        environmentKeys.size() >= 2, "invalid environment size: " + environmentKeys.size());

    // and create any missing key records as needed
    for (EnvironmentKey entry : environmentKeys.values()) {
      byte[] keyHash = HashUtil.calculateHash(entry.key);
      String keyHashStr = Base64.getEncoder().encodeToString(keyHash);
      Integer keyRef = existingKeys.get(keyHashStr);
      if (keyRef == null) {
        keyRef = nextKeyRef;
        Key key = new Key(keyRef, keyHash);
        keyRepository.save(key);
        existingKeys.put(keyHashStr, key.getKeyRef());
        nextKeyRef++;
      }
      keyMap.put(keyRef, entry.key);
    }

    currentKeyRef =
        existingKeys.get(Base64.getEncoder().encodeToString(HashUtil.calculateHash(currentKey)));
  }

  public byte[] encrypt(String clearText) {
    if (clearText == null || clearText.length() == 0) {
      return null;
    }

    return encrypt(clearText.getBytes());
  }

  public byte[] encrypt(byte[] clearText) {
    if (clearText == null || clearText.length == 0) {
      return null;
    }

    // create IV
    byte[] iv = new byte[ivLength];
    random.nextBytes(iv);

    // create key spec
    IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
    SecretKeySpec secretKeySpec = new SecretKeySpec(currentKey, AES);

    // create cipher for encryption
    Cipher cipher;
    try {
      cipher = Cipher.getInstance(AES_CFB_NO_PADDING);
      cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec);
    } catch (InvalidAlgorithmParameterException
        | NoSuchPaddingException
        | NoSuchAlgorithmException
        | InvalidKeyException e) {
      log.error("failed to encrypt bytes: {}", Arrays.toString(clearText));
      throw new RuntimeException(e);
    }

    // determine result size
    int outputSize = cipher.getOutputSize(clearText.length);

    int keyRefSize = VarInt.varIntSize(currentKeyRef);
    // create byte[] to hold the format (1 byte), the IV (16 bytes) and the encrypted bytes
    ByteBuffer buffer = ByteBuffer.allocate(1 + keyRefSize + iv.length + outputSize);

    // the format being used
    buffer.put(aesFormatVersion);

    // write the keyRef
    VarInt.putVarInt(currentKeyRef, buffer);

    // the IV
    buffer.put(iv);

    // finally, the encrypted bytes
    try {
      buffer.put(cipher.doFinal(clearText));
    } catch (IllegalBlockSizeException | BadPaddingException e) {
      log.error("failed to encrypt bytes: {}", Arrays.toString(clearText));
      throw new RuntimeException(e);
    }

    return buffer.array();
  }

  public byte[] decrypt(byte[] cipherText) {
    if (cipherText == null) {
      return null;
    }

    if (cipherText.length == 0) {
      String message =
          String.format(
              "invalid cipherText length, expected at least %d bytes got 0", 1 + ivLength + 1);
      log.error(message);
      throw new RuntimeException(message);
    }

    // wrap cipherText so we can read parts of it
    ByteBuffer buffer = ByteBuffer.wrap(cipherText);

    if (buffer.get() != aesFormatVersion) {
      String message =
          String.format(
              "invalid cipherText format, expected %02X got %02X", aesFormatVersion, cipherText[0]);
      log.error(message);
      throw new RuntimeException(message);
    }

    // look up the key that was used to encrypt this data
    int encKeyRef = VarInt.getVarInt(buffer);

    Assert.isTrue(
        keyMap.containsKey(encKeyRef), String.format("key not found for keyRef %d", encKeyRef));

    byte[] key = keyMap.get(encKeyRef);

    byte[] iv = new byte[ivLength];
    buffer.get(iv);

    // create key spec
    IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
    SecretKeySpec secretKeySpec = new SecretKeySpec(key, AES);

    try {
      // create cipher for decryption
      Cipher cipher = Cipher.getInstance(AES_CFB_NO_PADDING);
      cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec);

      ByteBuffer slice = buffer.slice();
      byte[] encryptedBytes = new byte[slice.capacity()];
      slice.get(encryptedBytes);

      return cipher.doFinal(encryptedBytes);
    } catch (Exception e) {
      log.error("failed to decrypt bytes: {}", Arrays.toString(cipherText));
      throw new RuntimeException(e);
    }
  }
}
