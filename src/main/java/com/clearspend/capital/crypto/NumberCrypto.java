package com.clearspend.capital.crypto;

import com.google.common.io.BaseEncoding;
import com.idealista.fpe.FormatPreservingEncryption;
import com.idealista.fpe.builder.FormatPreservingEncryptionBuilder;
import com.idealista.fpe.component.functions.prf.DefaultPseudoRandomFunction;
import com.idealista.fpe.config.Alphabet;
import com.idealista.fpe.config.GenericDomain;
import com.idealista.fpe.config.GenericTransformations;
import com.idealista.fpe.config.LengthRange;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;

public class NumberCrypto {
  // Format Preserving Encryption
  static String fpeEnvPrefix = "clearspend.encryption.fpe.";
  private final byte[] panKey;
  private final byte[] panTweak;

  private final Alphabet alphabet =
      new Alphabet() {
        private final char[] chars = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'};

        @Override
        public char[] availableCharacters() {
          return chars;
        }

        @Override
        public Integer radix() {
          return chars.length;
        }
      };
  private final JdbcTemplate jdbcTemplate;

  @Autowired
  public NumberCrypto(Environment env, JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
    // set up the credential and ticket format preserving encryption key and tweak
    if (env.getProperty(fpeEnvPrefix + "pan.key", "").isBlank()) {
      throw new RuntimeException(fpeEnvPrefix + "pan.key not set");
    }
    if (env.getProperty(fpeEnvPrefix + "pan.tweak", "").isBlank()) {
      throw new RuntimeException(fpeEnvPrefix + "pan.tweak not set");
    }
    panKey = BaseEncoding.base16().decode(env.getProperty(fpeEnvPrefix + "pan.key", ""));
    panTweak = BaseEncoding.base16().decode(env.getProperty(fpeEnvPrefix + "pan.tweak", ""));
  }

  // generates a new pan ID by getting a the next number from a database sequence and
  // encrypting it with format preserving encryption. All numbers are 9 digits and start with a 1
  public Integer generatePan() {
    Integer nextval = jdbcTemplate.queryForObject("select nextval('pan_seq')", Integer.class);

    // ensure that the number doesn't start with a leading zero
    assert nextval != null;
    return 10000000 + fpeEncrypt(panKey, panTweak, String.format("%07d", nextval));
  }

  private Integer fpeEncrypt(byte[] key, byte[] tweak, String clearText) {
    FormatPreservingEncryption formatPreservingEncryption =
        FormatPreservingEncryptionBuilder.ff1Implementation()
            .withDomain(
                new GenericDomain(
                    alphabet,
                    new GenericTransformations(alphabet.availableCharacters()),
                    new GenericTransformations(alphabet.availableCharacters())))
            .withPseudoRandomFunction(new DefaultPseudoRandomFunction(key))
            .withLengthRange(new LengthRange(6, 16))
            .build();
    return Integer.parseInt(formatPreservingEncryption.encrypt(clearText, tweak));
  }

  private Integer fpeDecrypt(byte[] key, byte[] tweak, Integer cipherText) {
    FormatPreservingEncryption formatPreservingEncryption =
        FormatPreservingEncryptionBuilder.ff1Implementation()
            .withDomain(
                new GenericDomain(
                    alphabet,
                    new GenericTransformations(alphabet.availableCharacters()),
                    new GenericTransformations(alphabet.availableCharacters())))
            .withPseudoRandomFunction(new DefaultPseudoRandomFunction(key))
            .withLengthRange(new LengthRange(6, 16))
            .build();
    return Integer.parseInt(formatPreservingEncryption.decrypt(cipherText.toString(), tweak));
  }
}
