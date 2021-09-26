package com.tranwall.crypto;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import com.github.javafaker.Faker;
import com.google.common.io.BaseEncoding;
import com.tranwall.crypto.data.repository.KeyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.env.MockEnvironment;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CryptoTest {
  private final byte[] encodedKey0 = HashUtil.generateKey();
  private final byte[] encodedKey1 = HashUtil.generateKey();

  @Mock private KeyRepository keyRepository;

  record TestKey(byte[] key, byte[] hash, String name) {}

  private TestKey cryptoTestKey0() {
    return new TestKey(encodedKey0, HashUtil.calculateHash(encodedKey0), "key 0");
  }

  private TestKey cryptoTestKey1() {
    return new TestKey(encodedKey1, HashUtil.calculateHash(encodedKey1), "key 1");
  }

  private MockEnvironment cryptoTestEnv() {
    return new MockEnvironment()
        .withProperty(Crypto.envPrefix + ".0", BaseEncoding.base16().encode(cryptoTestKey0().key()))
        .withProperty(Crypto.envPrefix + ".1", BaseEncoding.base16().encode(cryptoTestKey1().key()))
        .withProperty("test.key.base", "test.key.aes");
  }

  private Crypto crytptoTestCrypto() {
    return new Crypto(cryptoTestEnv(), keyRepository);
  }

  private final Crypto crypto = crytptoTestCrypto();

  private final Faker faker = new Faker();

  @Test
  void test_encryptAndDecrypt() {
    int tests = faker.random().nextInt(1000, 5000);
    while (tests-- > 0) {

      int length = faker.random().nextInt(1, 50);
      StringBuilder pattern = new StringBuilder();
      while (length-- > 0) {
        pattern.append(faker.random().nextBoolean() ? "#" : "?");
      }

      String secret = faker.bothify(pattern.toString());
      byte[] secretBytes = secret.getBytes();
      byte[] encryptedSecret = crypto.encrypt(secret);
      byte[] encryptedSecretFromBytes = crypto.encrypt(secretBytes);
      assertArrayEquals(
          secretBytes,
          crypto.decrypt(encryptedSecret),
          "Encrypted value decrypts to unknown value");
      assertArrayEquals(
          secretBytes,
          crypto.decrypt(encryptedSecretFromBytes),
          "EncryptedFromBytes value decrypts to unknown value");
    }
  }

  @Test
  void test_encryptAndDecryptNull() {
    byte[] encryptedSecret = crypto.encrypt((String) null);
    byte[] encryptedSecretFromBytes = crypto.encrypt((byte[]) null);
    assertArrayEquals(
        null, crypto.decrypt(encryptedSecret), "Encrypted value decrypts to unknown value");
    assertArrayEquals(
        null,
        crypto.decrypt(encryptedSecretFromBytes),
        "EncryptedFromBytes value decrypts to unknown value");
  }
}
