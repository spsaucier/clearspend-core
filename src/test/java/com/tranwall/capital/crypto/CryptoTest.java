package com.tranwall.capital.crypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.common.io.BaseEncoding;
import com.tranwall.capital.CapitalTest;
import com.tranwall.capital.crypto.data.model.Key;
import com.tranwall.capital.crypto.data.repository.KeyRepository;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

@CapitalTest
@Slf4j
public class CryptoTest {
  @Mock KeyRepository keyRepository;

  static MockEnvironment getMockEnvironment(byte[] key) {
    MockEnvironment env = new MockEnvironment();
    if (key == null) {
      key = HashUtil.generateKey();
    }
    env.setProperty(Crypto.envPrefix + "current", Base64.encodeBase64String(key));
    String next = HashUtil.generateKeyString();
    env.setProperty(Crypto.envPrefix + "next", next);
    env.setProperty(Crypto.envPrefix + "0", Base64.encodeBase64String(key));

    return env;
  }

  @Test
  public void testCrypto_noEnvironment() {
    assertThrows(Throwable.class, () -> new Crypto(new MockEnvironment(), keyRepository));
  }

  @Test
  public void testCrypto_current() {
    MockEnvironment env = new MockEnvironment();
    env.setProperty(Crypto.envPrefix + "current", HashUtil.generateKeyString());

    assertThrows(Throwable.class, () -> new Crypto(env, keyRepository));
  }

  @Test
  public void testCrypto_currentAndNext() {
    MockEnvironment env = new MockEnvironment();
    env.setProperty(Crypto.envPrefix + "current", HashUtil.generateKeyString());
    env.setProperty(Crypto.envPrefix + "next", HashUtil.generateKeyString());

    assertThrows(Throwable.class, () -> new Crypto(env, keyRepository));
  }

  @Test
  public void testCrypto_invalidZeroKey() {
    MockEnvironment env = new MockEnvironment();
    env.setProperty(Crypto.envPrefix + "current", HashUtil.generateKeyString());
    env.setProperty(Crypto.envPrefix + "next", HashUtil.generateKeyString());
    env.setProperty(Crypto.envPrefix + "0", HashUtil.generateKeyString());

    assertThrows(Throwable.class, () -> new Crypto(env, keyRepository));
  }

  @Test
  public void testCrypto_duplicateCurrentNext() {
    MockEnvironment env = new MockEnvironment();
    String key = HashUtil.generateKeyString();
    env.setProperty(Crypto.envPrefix + "current", key);
    env.setProperty(Crypto.envPrefix + "next", key);
    env.setProperty(Crypto.envPrefix + "0", key);

    assertThrows(Throwable.class, () -> new Crypto(env, keyRepository));
  }

  @Test
  public void testCrypto_duplicateKey() {
    MockEnvironment env = new MockEnvironment();
    String current = HashUtil.generateKeyString();
    String next = HashUtil.generateKeyString();
    String randomKey = HashUtil.generateKeyString();
    env.setProperty(Crypto.envPrefix + "current", current);
    env.setProperty(Crypto.envPrefix + "next", next);
    env.setProperty(Crypto.envPrefix + "0", randomKey);
    env.setProperty(Crypto.envPrefix + "1", randomKey);
    env.setProperty(Crypto.envPrefix + "2", current);

    assertThrows(Throwable.class, () -> new Crypto(env, keyRepository));
  }

  @Test
  public void testCrypto_duplicateReplacementKey() {
    MockEnvironment env = new MockEnvironment();
    String current = HashUtil.generateKeyString();
    String next = HashUtil.generateKeyString();
    String randomKey = HashUtil.generateKeyString();
    env.setProperty(Crypto.envPrefix + "current", current);
    env.setProperty(Crypto.envPrefix + "next", next);
    env.setProperty(Crypto.envPrefix + "0", randomKey + "|" + current);
    env.setProperty(Crypto.envPrefix + "1", current);

    assertThrows(Throwable.class, () -> new Crypto(env, keyRepository));
  }

  @Test
  public void testCrypto_valid() {
    assertDoesNotThrow(() -> new Crypto(getMockEnvironment(null), keyRepository));
  }

  @Test
  public void testCrypto_validKeyReplacement() {
    MockEnvironment env = new MockEnvironment();
    String current = HashUtil.generateKeyString();
    String next = HashUtil.generateKeyString();
    String randomKey = HashUtil.generateKeyString();
    String replacement = HashUtil.generateKeyString();
    env.setProperty(Crypto.envPrefix + "current", current);
    env.setProperty(Crypto.envPrefix + "next", next);
    env.setProperty(Crypto.envPrefix + "0", randomKey + "|" + replacement);
    env.setProperty(Crypto.envPrefix + "1", current);

    assertDoesNotThrow(() -> new Crypto(env, keyRepository));
  }

  @Test
  public void testCrypto() {
    // start with an empty repo
    Mockito.when(keyRepository.findAll()).thenReturn(Collections.emptyList());

    // set up environment
    byte[] key = HashUtil.generateKey();
    MockEnvironment env = getMockEnvironment(key);

    // create instance of crypto
    Crypto crypto = new Crypto(env, keyRepository);

    // validate that we correctly loaded the keys
    assertThat(ReflectionTestUtils.getField(crypto, "currentKey")).isEqualTo(key);

    // test away
    for (int i = 1; i < 20; i++) {
      for (int j = 1; j < 40; j++) {
        String clearText = RandomStringUtils.randomAlphanumeric(i + j);
        byte[] cipherText = crypto.encrypt(clearText);
        byte[] hash = HashUtil.calculateHash(clearText);
//        log.info("clearText: {}, hash: {}, cipherText: {}", clearText, hash, cipherText);
        assertThat(cipherText).isNotEqualTo(clearText);
        assertThat(hash).isNotNull();
        assertThat(hash).isNotEqualTo("");

        String clearText2 = new String(crypto.decrypt(cipherText));
        byte[] hash2 = HashUtil.calculateHash(clearText);
        assertThat(clearText2).isEqualTo(clearText);
        assertThat(hash2).isEqualTo(hash);
      }
    }

    Map<byte[], Boolean> hashes = new HashMap<>();
    for (int i = 1; i < 5000; i++) {
      String clearText = UUID.randomUUID().toString();
      byte[] hash = HashUtil.calculateHash(clearText);
      assertThat(hashes.containsKey(hash)).isFalse();
      hashes.put(hash, true);
    }
  }

  @Test
  public void testCrypto_null() {
    // start with an empty repo
    Mockito.when(keyRepository.findAll()).thenReturn(Collections.emptyList());

    // set up environment
    byte[] key = HashUtil.generateKey();
    MockEnvironment env = getMockEnvironment(key);

    // create instance of crypto
    Crypto crypto = new Crypto(env, keyRepository);

    // validate that we correctly loaded the keys
    assertThat(ReflectionTestUtils.getField(crypto, "currentKey")).isEqualTo(key);

    // test away
    String clearText = null;
    byte[] cipherText = crypto.encrypt(clearText);
    byte[] hash = HashUtil.calculateHash(clearText);
    assertThat(clearText).isNull();
    assertThat(cipherText).isNull();
    assertThat(hash).isNull();
  }

  @Test
  public void testCrypto_oldKey() {
    // start with an empty repo
    Mockito.when(keyRepository.findAll()).thenReturn(Collections.emptyList());

    // set up environment
    byte[] key = HashUtil.generateKey();
    MockEnvironment env = getMockEnvironment(key);

    // create instance of crypto
    Crypto crypto = new Crypto(env, keyRepository);

    // validate that we correctly loaded the keys
    assertThat(ReflectionTestUtils.getField(crypto, "currentKey")).isEqualTo(key);

    // encrypt with old key
    String clearText = "hello world";
    byte[] cipherText = crypto.encrypt(clearText);
    assertThat(cipherText).isNotEqualTo(clearText.getBytes());

    // update the environment
    env = new MockEnvironment();
    String newKey = HashUtil.generateKeyString();
    env.setProperty(Crypto.envPrefix + "current", newKey);
    String next = HashUtil.generateKeyString();
    env.setProperty(Crypto.envPrefix + "next", next);
    env.setProperty(Crypto.envPrefix + "0", BaseEncoding.base64().encode(key));
    env.setProperty(Crypto.envPrefix + "1", newKey);
    Crypto cryptoNew = new Crypto(env, keyRepository);

    // decrypt with old key in new environment
    byte[] clearText2 = cryptoNew.decrypt(cipherText);
    assertThat(clearText2).isEqualTo(clearText.getBytes());
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testCrypto_previousData() {
    byte[] key = HashUtil.generateKey();

    // start with data in the database
    Mockito.when(keyRepository.findAll())
        .thenReturn(Collections.singletonList(new Key(10, HashUtil.calculateHash(key))));

    // set up environment
    MockEnvironment env = new MockEnvironment();
    String newKey = HashUtil.generateKeyString();
    env.setProperty(Crypto.envPrefix + "current", newKey);
    String next = HashUtil.generateKeyString();
    env.setProperty(Crypto.envPrefix + "next", next);
    env.setProperty(Crypto.envPrefix + "0", BaseEncoding.base64().encode(key));
    env.setProperty(Crypto.envPrefix + "1", newKey);

    // create instance of crypto
    Crypto crypto = new Crypto(env, keyRepository);

    // validate that it's been set up correctly
    HashMap<Integer, byte[]> keyMap =
        (HashMap<Integer, byte[]>) ReflectionTestUtils.getField(crypto, "keyMap");
    assertThat(keyMap).isNotNull();
    assertThat(keyMap.size()).isEqualTo(3);
    assertThat(ReflectionTestUtils.getField(crypto, "currentPasswordAndSaltRef")).isEqualTo(11);
    assertThat(keyMap.get(10)).isNotNull();
    assertThat(keyMap.get(11)).isNotNull();
    assertThat(keyMap.get(12)).isNotNull();
  }
}
