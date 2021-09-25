package com.tranwall.crypto;

import static com.tranwall.crypto.PasswordAndSalt.digest;

import com.google.common.io.BaseEncoding;
import com.tranwall.crypto.data.model.Key;
import com.tranwall.crypto.data.repository.KeyRepository;
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

  @Autowired
  public Crypto(Environment env, KeyRepository keyRepository) {
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
  public byte[] encrypt(String clearText) {
    if (clearText == null) {
      return null;
    }
    try {
      // create a single use key
      PasswordAndSalt dek = new PasswordAndSalt();
      BytesEncryptor dekBytesEncryptor = Encryptors.stronger(dek.getPassword(), dek.getSalt());
      String cipherText =
          BaseEncoding.base64().encode(dekBytesEncryptor.encrypt(clearText.getBytes()));
      BytesEncryptor bytesEncryptor = bytesEncryptorHashMap.get(currentPasswordAndSaltRef);

      // encrypt DEK with master key
      String cipherDek = BaseEncoding.base64().encode(bytesEncryptor.encrypt(dek.toBytes()));
      return keyVersion + "|" + currentPasswordAndSaltRef + "|" + cipherDek + "|" + cipherText;
    } catch (Exception e) {
      log.error("failed to encrypt clearText ({}): {}", clearText, e.getMessage());
      throw new RuntimeException(e);
    }
  }

  public byte[] calculateHash(byte[] clearText) {
    if (clearText == null) {
      return null;
    }

    if (clearText.startsWith("hash:")) {
      return clearText;
    }

    return "hash:"
        + BaseEncoding.base64().encode(digest.digest(clearText.toLowerCase().getBytes()));
  }

  // decrypt the 4 part tuple (version, key ref, dek and text) created by encrypt
  public byte[] decrypt(byte[] tupleString) {
    if (tupleString == null) {
      return null;
    }
    // unpack tuple
    String[] parts = tupleString.split(keyDelimiter);
    Assert.isTrue(parts.length == 4, "invalid number of parts in tupleString: " + tupleString);
    String version = parts[0];
    String keyRef = parts[1];
    byte[] cipherDek = BaseEncoding.base64().decode(parts[2]);
    byte[] cipherText = BaseEncoding.base64().decode(parts[3]);
    Assert.isTrue(
        keyVersion.equals(version),
        "invalid encrypted version, " + version + ", expected " + keyVersion);

    // fetch previously stored encryptor for the key ref
    BytesEncryptor dekBytesEncryptor = bytesEncryptorHashMap.get(Integer.parseInt(keyRef));
    Assert.isTrue(dekBytesEncryptor != null, "null kek for keyRef " + keyRef);

    // we have two try/catch blocks so we can identify which decryption operation failed if any
    // decrypt the DEK
    PasswordAndSalt dek;
    try {
      dek = new PasswordAndSalt(dekBytesEncryptor.decrypt(cipherDek));
    } catch (Exception e) {
      log.error("failed to decrypt DEK (tuple: {}): {}", tupleString, e.getMessage());
      throw new RuntimeException(e);
    }

    // decrypt the text with the decrypted DEK
    try {
      BytesEncryptor bytesEncryptor = Encryptors.stronger(dek.getPassword(), dek.getSalt());
      return new String(bytesEncryptor.decrypt(cipherText));
    } catch (Exception e) {
      log.error(
          "failed to decrypt cipherText (tuple: {}) with DEK: {}", tupleString, e.getMessage());
      throw new RuntimeException(e);
    }
  }

  @AllArgsConstructor
  @EqualsAndHashCode
  private class EnvironmentKey {
    public PasswordAndSalt key;
    public String name;
  }
}
