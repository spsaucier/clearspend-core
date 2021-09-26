package com.tranwall.crypto;

import static com.tranwall.crypto.PasswordAndSalt.digest;
import static java.lang.Byte.parseByte;
import static org.apache.commons.codec.digest.DigestUtils.sha256;

import com.google.common.io.BaseEncoding;
import com.tranwall.crypto.data.model.Key;
import com.tranwall.crypto.data.repository.KeyRepository;
import com.tranwall.crypto.utils.VarInt;
import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.util.Assert;
import java.util.ArrayList;
import java.util.HashMap;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.encrypt.BytesEncryptor;
import org.springframework.security.crypto.encrypt.Encryptors;

// @Component
@Slf4j
public class Crypto {
  static String envPrefix = "tranwall.encryption.key.aes.";
  private static String keyVersion = "0";
  private static int maxKeys = 1000;
  private static String keyDelimiter = "\\|";
  private PasswordAndSalt currentPasswordAndSalt;
  private Integer currentPasswordAndSaltRef;
  private HashMap<Integer, BytesEncryptor> bytesEncryptorHashMap = new HashMap<>();
  private HashMap<String, String> replacementKeys = new HashMap<>();
  // variables used to load the keys
  private HashMap<String, Integer> existingKeys = new HashMap<>();
  private Integer nextKeyRef = 0;

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
    for (Key key : keyRepository.findAll()) {
      existingKeys.put(key.getKeyHash(), key.getKeyRef());
      if (nextKeyRef < key.getKeyRef()) {
        nextKeyRef = key.getKeyRef() + 1;
      }
    }

    // read all the environment
    // first the "current" key
    String currentKeyString = env.getProperty(envPrefix + "current");
    Assert.isTrue(
        Strings.isNotBlank(currentKeyString), "current aes key is null or empty");
    currentPasswordAndSalt = new PasswordAndSalt(currentKeyString);

    // then numbered keys
    ArrayList<EnvironmentKey> environmentKeys = new ArrayList<>();
    Loop:
    for (int i = 0; i < maxKeys; i++) {
      String envString = env.getProperty(envPrefix + i);
      Assert.isTrue(StringUtils.isNotBlank(envString), "invalid key: " + i);

      String[] envParts = envString.split(keyDelimiter);
      switch (envParts.length) {
        case 2:
          // if we have 2 keys add to the list of replacement keys, they fall through to the single
          // key case
          replacementKeys.put(envParts[0], envParts[1]);
        case 1:
          // add either one or two keys
          for (String keyString : envParts) {
            PasswordAndSalt passwordAndSalt = new PasswordAndSalt(keyString);
            EnvironmentKey environmentKey = new EnvironmentKey(passwordAndSalt, String.valueOf(i));

            if (environmentKeys.contains(environmentKey)) {
              throw new RuntimeException("Duplicate key " + i);
            }
            environmentKeys.add(environmentKey);

            if (currentPasswordAndSalt.equals(passwordAndSalt)) {
              // the current index key matches the current key
              break Loop;
            }
          }
          break;
        default:
          // we have an invalid number of keys for this envrionment variable
          throw new RuntimeException("Invalid key found for " + i);
      }
    }
    // finally "next" key
    String nextKeyString = env.getProperty(envPrefix + "next");
    org.springframework.util.Assert.isTrue(
        Strings.isNotBlank(nextKeyString), "next aes key is null or empty");
    PasswordAndSalt nextKey = new PasswordAndSalt(nextKeyString);
    environmentKeys.add(new EnvironmentKey(nextKey, "next"));

    // at a minimum we should have 2 keys, the "current" and  the "next" keys
    org.springframework.util.Assert.isTrue(
        environmentKeys.size() >= 2, "invalid environment size: " + environmentKeys.size());

    // and create any missing key records as needed
    for (EnvironmentKey entry : environmentKeys) {
      String keyHash = entry.key.getHash();
      Integer keyRef = existingKeys.get(keyHash);
      if (keyRef == null) {
        Key key = new Key(nextKeyRef++, keyHash);
        keyRepository.save(key);
        keyRef = key.getKeyRef();
        existingKeys.put(key.getKeyHash(), key.getKeyRef());
      }

      // create and store the bytesEncryptor for the key ref
      Assert.isTrue(
          bytesEncryptorHashMap.put(
                  keyRef, Encryptors.stronger(entry.key.getPassword(), entry.key.getSalt()))
              == null,
          "Existing key found for " + entry.name);
    }

    // set currentPasswordAndSaltRef
    currentPasswordAndSaltRef = existingKeys.get(currentPasswordAndSalt.getHash());
  }

  // encrypts clearText using a generated DEK (data encryption key). The DEK is encrypted with the
  // current key. The end result is a pipe delimited string containing the encryption version (0),
  // the password and salt ref (integer), a base64 encode byte[] for the DEK and clear text.
//  public byte[] encrypt(String clearText) {
//    if (clearText == null) {
//      return null;
//    }
//    try {
//      // create a single use key
//      PasswordAndSalt dek = new PasswordAndSalt();
//      BytesEncryptor dekBytesEncryptor = Encryptors.stronger(dek.getPassword(), dek.getSalt());
//      String cipherText =
//          BaseEncoding.base64().encode(dekBytesEncryptor.encrypt(clearText.getBytes()));
//      BytesEncryptor bytesEncryptor = bytesEncryptorHashMap.get(currentPasswordAndSaltRef);
//
//      // encrypt DEK with master key
//      String cipherDek = BaseEncoding.base64().encode(bytesEncryptor.encrypt(dek.toBytes()));
//      return keyVersion + "|" + currentPasswordAndSaltRef + "|" + cipherDek + "|" + cipherText;
//    } catch (Exception e) {
//      log.error("failed to encrypt clearText ({}): {}", clearText, e.getMessage());
//      throw new RuntimeException(e);
//    }
//  }

  public byte[] encrypt(String clearText) {
    if (clearText == null || clearText.length() == 0) {
      return null;
    }

    return encrypt(clearText.getBytes());
  }

  public byte[] encrypt(byte[] clearText) {
    return clearText;
//    if (clearText == null || clearText.length == 0) {
//      return null;
//    }
//
//    // create IV
//    byte[] iv = new byte[ivLength];
//    random.nextBytes(iv);
//
//    // create key spec
//    IvParameterSpec ivSpec = new IvParameterSpec(iv);
//    SecretKeySpec skeySpec = new SecretKeySpec(currentKey, AES);
//
//    // create cipher for encryption
//    Cipher cipher;
//    try {
//      cipher = Cipher.getInstance(AES_CFB_NO_PADDING);
//      cipher.init(Cipher.ENCRYPT_MODE, skeySpec, ivSpec);
//    } catch (InvalidKeyException
//        | InvalidAlgorithmParameterException
//        | NoSuchPaddingException
//        | NoSuchAlgorithmException e) {
//      log.error("failed to encrypt bytes: {}", Arrays.toString(clearText));
//      throw new RuntimeException(e);
//    }
//
//    // determine result size
//    int outputSize = cipher.getOutputSize(clearText.length);
//
//    int keyRefSize = VarInt.varIntSize(currentKeyRef);
//    // create byte[] to hold the format (1 byte), the IV (16 bytes) and the encrypted bytes
//    ByteBuffer buffer = ByteBuffer.allocate(1 + keyRefSize + iv.length + outputSize);
//
//    // the format being used
//    buffer.put(aesFormatVersion);
//
//    // write the keyRef
//    VarInt.putVarInt(currentKeyRef, buffer);
//
//    // the IV
//    buffer.put(iv);
//
//    // finally, the encrypted bytes
//    try {
//      buffer.put(cipher.doFinal(clearText));
//    } catch (IllegalBlockSizeException | BadPaddingException e) {
//      log.error("failed to encrypt bytes: {}", Arrays.toString(clearText));
//      throw new RuntimeException(e);
//    }
//
//    return buffer.array();
  }

//  // decrypt the 4 part tuple (version, key ref, dek and text) created by encrypt
//  public byte[] decrypt(byte[] tupleString) {
//    if (tupleString == null) {
//      return null;
//    }
//    // unpack tuple
//    String[] parts = tupleString.split(keyDelimiter);
//    Assert.isTrue(parts.length == 4, "invalid number of parts in tupleString: " + tupleString);
//    String version = parts[0];
//    String keyRef = parts[1];
//    byte[] cipherDek = BaseEncoding.base64().decode(parts[2]);
//    byte[] cipherText = BaseEncoding.base64().decode(parts[3]);
//    Assert.isTrue(
//        keyVersion.equals(version),
//        "invalid encrypted version, " + version + ", expected " + keyVersion);
//
//    // fetch previously stored encryptor for the key ref
//    BytesEncryptor dekBytesEncryptor = bytesEncryptorHashMap.get(Integer.parseInt(keyRef));
//    Assert.isTrue(dekBytesEncryptor != null, "null kek for keyRef " + keyRef);
//
//    // we have two try/catch blocks so we can identify which decryption operation failed if any
//    // decrypt the DEK
//    PasswordAndSalt dek;
//    try {
//      dek = new PasswordAndSalt(dekBytesEncryptor.decrypt(cipherDek));
//    } catch (Exception e) {
//      log.error("failed to decrypt DEK (tuple: {}): {}", tupleString, e.getMessage());
//      throw new RuntimeException(e);
//    }
//
//    // decrypt the text with the decrypted DEK
//    try {
//      BytesEncryptor bytesEncryptor = Encryptors.stronger(dek.getPassword(), dek.getSalt());
//      return new String(bytesEncryptor.decrypt(cipherText));
//    } catch (Exception e) {
//      log.error(
//          "failed to decrypt cipherText (tuple: {}) with DEK: {}", tupleString, e.getMessage());
//      throw new RuntimeException(e);
//    }
//  }
  public byte[] decrypt(byte[] cipherText) {
    return cipherText;
//    if (cipherText == null) {
//      return null;
//    }
//
//    if (cipherText.length == 0) {
//      String message =
//          String.format(
//              "invalid cipherText length, expected at least %d bytes got 0", 1 + ivLength + 1);
//      log.error(message);
//      throw new RuntimeException(message);
//    }
//
//    // wrap cipherText so we can read parts of it
//    ByteBuffer buffer = ByteBuffer.wrap(cipherText);
//
//    if (buffer.get() != aesFormatVersion) {
//      String message =
//          String.format(
//              "invalid cipherText format, expected %02X got %02X",
//              aesFormatVersion, cipherText[0]);
//      log.error(message);
//      throw new RuntimeException(message);
//    }
//
//    // look up the key that was used to encrypt this data
//    int encKeyRef = VarInt.getVarInt(buffer);
//
//    Assert.isTrue(
//        config.keyRefToKeyMap.containsKey(encKeyRef),
//        String.format("key not found for keyRef %d", encKeyRef));
//
//    CryptoKey key = config.keyRefToKeyMap.get(encKeyRef);
//
//    byte[] iv = new byte[ivLength];
//    buffer.get(iv);
//
//    // create key spec
//    IvParameterSpec ivSpec = new IvParameterSpec(iv);
//    SecretKeySpec skeySpec = new SecretKeySpec(key.key(), config.secretKeySpecAlgorithm);
//
//    try {
//      // create cipher for decryption
//      Cipher cipher = Cipher.getInstance(config.algorithmProvider);
//      cipher.init(Cipher.DECRYPT_MODE, skeySpec, ivSpec);
//
//      ByteBuffer slice = buffer.slice();
//      byte[] encryptedBytes = new byte[slice.capacity()];
//      slice.get(encryptedBytes);
//
//      return cipher.doFinal(encryptedBytes);
//    } catch (Exception e) {
//      log.error("failed to decrypt bytes: {}", Arrays.toString(cipherText));
//      throw new RuntimeException(e);
//    }
  }

  @AllArgsConstructor
  @EqualsAndHashCode
  private class EnvironmentKey {
    public PasswordAndSalt key;
    public String name;
  }
}
