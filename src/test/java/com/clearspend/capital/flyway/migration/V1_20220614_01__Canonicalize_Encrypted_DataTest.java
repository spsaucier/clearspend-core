package com.clearspend.capital.flyway.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.TestHelper.CreateBusinessRecord;
import com.clearspend.capital.crypto.Crypto;
import com.clearspend.capital.crypto.HashUtil;
import com.clearspend.capital.crypto.data.converter.canonicalization.Canonicalizer;
import com.clearspend.capital.data.model.User;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import javax.persistence.EntityManager;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

@RequiredArgsConstructor(onConstructor = @__({@Autowired}))
public class V1_20220614_01__Canonicalize_Encrypted_DataTest extends BaseCapitalTest {
  private static final String UPDATE_USER_VALUES =
      """
            UPDATE users
            SET first_name_encrypted = :firstNameEncrypted,
                first_name_hash = :firstNameHashed,
                last_name_encrypted = :lastNameEncrypted,
                last_name_hash = :lastNameHashed,
                email_encrypted = :emailEncrypted,
                email_hash = :emailHashed,
                phone_encrypted = :phoneEncrypted,
                phone_hash = :phoneHashed
            WHERE id = :userId;
            """;
  private static final String GET_USER_VALUES =
      """
          SELECT first_name_encrypted, first_name_hash,
            last_name_encrypted, last_name_hash,
            email_encrypted, email_hash,
            phone_encrypted, phone_hash
          FROM users
          WHERE id = :userId;
          """;
  private static final String RAW_FIRST_NAME = "JÓhn ";
  private static final String RAW_LAST_NAME = "DÓe ";
  private static final String RAW_EMAIL = "A bocd@gmail.com";
  private static final String RAW_PHONE = "234-567-8901";
  private final DataSource dataSource;
  private final V1_20220614_01__Canonicalize_Encrypted_Data migration;
  private final TestHelper testHelper;
  private final Crypto crypto;
  private final EntityManager entityManager;

  private CreateBusinessRecord createBusinessRecord;
  private User user;
  private NamedParameterJdbcTemplate jdbcTemplate;

  @BeforeEach
  void setup() {
    jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
    createBusinessRecord = testHelper.createBusiness();
    user = testHelper.createUser(createBusinessRecord.business()).user();
    entityManager.flush();
    prepareUserValuesToMigrate(user.getId().toUuid());
  }

  @SneakyThrows
  private void prepareUserValuesToMigrate(final UUID userId) {
    final SqlParameterSource params =
        new MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("firstNameEncrypted", crypto.encrypt(RAW_FIRST_NAME))
            .addValue("firstNameHashed", HashUtil.calculateHash(RAW_FIRST_NAME))
            .addValue("lastNameEncrypted", crypto.encrypt(RAW_LAST_NAME))
            .addValue("lastNameHashed", HashUtil.calculateHash(RAW_LAST_NAME))
            .addValue("emailEncrypted", crypto.encrypt(RAW_EMAIL))
            .addValue("emailHashed", HashUtil.calculateHash(RAW_EMAIL))
            .addValue("phoneEncrypted", crypto.encrypt(RAW_PHONE))
            .addValue("phoneHashed", HashUtil.calculateHash(RAW_PHONE));
    final int rows = jdbcTemplate.update(UPDATE_USER_VALUES, params);
    assertEquals(1, rows);
  }

  @Test
  @SneakyThrows
  void performMigration() {
    migration.migrate(null);

    final SqlParameterSource params =
        new MapSqlParameterSource().addValue("userId", user.getId().toUuid());
    final List<UserValues> userValuesList =
        jdbcTemplate.query(
            GET_USER_VALUES,
            params,
            (rs, num) ->
                new UserValues(
                    rs.getBytes("first_name_encrypted"),
                    rs.getBytes("first_name_hash"),
                    rs.getBytes("last_name_encrypted"),
                    rs.getBytes("last_name_hash"),
                    rs.getBytes("email_encrypted"),
                    rs.getBytes("email_hash"),
                    rs.getBytes("phone_encrypted"),
                    rs.getBytes("phone_hash")));

    assertThat(userValuesList).hasSize(1);
    assertEquals(
        Canonicalizer.NAME.forEncryption(RAW_FIRST_NAME),
        decrypt(userValuesList.get(0).firstNameEncrypted()));
    assertArrayEquals(
        toExpectedHash(RAW_FIRST_NAME, Canonicalizer.NAME),
        userValuesList.get(0).firstNameHashed());
    assertEquals(
        Canonicalizer.NAME.forEncryption(RAW_LAST_NAME),
        decrypt(userValuesList.get(0).lastNameEncrypted()));
    assertArrayEquals(
        toExpectedHash(RAW_LAST_NAME, Canonicalizer.NAME), userValuesList.get(0).lastNameHashed());
    assertEquals(
        Canonicalizer.EMAIL.forEncryption(RAW_EMAIL),
        decrypt(userValuesList.get(0).emailEncrypted()));
    assertArrayEquals(
        toExpectedHash(RAW_EMAIL, Canonicalizer.EMAIL), userValuesList.get(0).emailHashed());
    assertEquals(
        Canonicalizer.PHONE.forEncryption(RAW_PHONE),
        decrypt(userValuesList.get(0).phoneEncrypted()));
    assertArrayEquals(
        toExpectedHash(RAW_PHONE, Canonicalizer.PHONE), userValuesList.get(0).phoneHashed());
  }

  private byte[] toExpectedHash(final String raw, final Canonicalizer canonicalizer) {
    return HashUtil.calculateHash(canonicalizer.forHash(raw));
  }

  private String decrypt(final byte[] cipher) {
    return new String(crypto.decrypt(cipher), StandardCharsets.UTF_8);
  }

  private record UserValues(
      byte[] firstNameEncrypted,
      byte[] firstNameHashed,
      byte[] lastNameEncrypted,
      byte[] lastNameHashed,
      byte[] emailEncrypted,
      byte[] emailHashed,
      byte[] phoneEncrypted,
      byte[] phoneHashed) {}
}
