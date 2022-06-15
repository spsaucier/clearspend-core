package com.clearspend.capital.flyway.migration;

import com.clearspend.capital.crypto.Crypto;
import com.clearspend.capital.crypto.data.converter.EncryptionConverter;
import com.clearspend.capital.crypto.data.converter.NullableEncryptionConverter;
import com.clearspend.capital.crypto.data.model.embedded.NullableEncryptedPhoneWithHash;
import com.clearspend.capital.crypto.data.model.embedded.RequiredEncryptedEmailWithHash;
import com.clearspend.capital.crypto.data.model.embedded.RequiredEncryptedNameWithHash;
import com.clearspend.capital.crypto.data.repository.KeyRepositoryImpl;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import javax.annotation.Nullable;
import javax.sql.DataSource;
import lombok.NonNull;
import org.bouncycastle.util.Arrays;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Component;

@Component
public class V1_20220614_01__Canonicalize_Encrypted_Data extends CapitalFlywayMigration {

  private static final String GET_ALL_USERS =
      """
          SELECT id,
                  first_name_encrypted,
                  last_name_encrypted,
                  email_encrypted,
                  phone_encrypted
          FROM users
          """;
  private static final String UPDATE_USERS =
      """
          UPDATE users
          SET first_name_encrypted = :firstNameEncrypted,
            first_name_hash = :firstNameHash,
            last_name_encrypted = :lastNameEncrypted,
            last_name_hash = :lastNameHash,
            email_encrypted = :emailEncrypted,
            email_hash = :emailHash,
            phone_encrypted = :phoneEncrypted,
            phone_hash = :phoneHash
          WHERE id = :userId
      """;

  private final Environment env;

  @Autowired
  public V1_20220614_01__Canonicalize_Encrypted_Data(
      final Environment env, final DataSource dataSource) {
    super(dataSource);
    this.env = env;
  }

  @Override
  protected void performMigration(final NamedParameterJdbcTemplate jdbcTemplate) throws Exception {
    final Crypto crypto = new Crypto(env, new KeyRepositoryImpl(jdbcTemplate));
    final EncryptionConverter encryptionConverter = new EncryptionConverter(crypto);
    final NullableEncryptionConverter nullableEncryptionConverter =
        new NullableEncryptionConverter(crypto);

    final SqlParameterSource[] updateParams =
        jdbcTemplate
            .queryForStream(GET_ALL_USERS, Map.of(), createUserValuesRowMapper(crypto))
            .map(createUpdateUserValues(encryptionConverter, nullableEncryptionConverter))
            .toArray(SqlParameterSource[]::new);

    if (!Arrays.isNullOrEmpty(updateParams)) {
      jdbcTemplate.batchUpdate(UPDATE_USERS, updateParams);
    }
  }

  private record UserValues(
      UUID id, String firstName, String lastName, String email, String phone) {}

  private static RowMapper<UserValues> createUserValuesRowMapper(@NonNull final Crypto crypto) {
    return (resultSet, rowNum) ->
        new UserValues(
            resultSet.getObject("id", UUID.class),
            decrypt(crypto, resultSet.getBytes("first_name_encrypted")),
            decrypt(crypto, resultSet.getBytes("last_name_encrypted")),
            decrypt(crypto, resultSet.getBytes("email_encrypted")),
            decrypt(crypto, resultSet.getBytes("phone_encrypted")));
  }

  @Nullable
  private static String decrypt(@NonNull final Crypto crypto, @Nullable byte[] cipher) {
    return Optional.ofNullable(cipher)
        .map(crypto::decrypt)
        .map(decrypted -> new String(decrypted, StandardCharsets.UTF_8))
        .orElse(null);
  }

  private static Function<UserValues, SqlParameterSource> createUpdateUserValues(
      @NonNull final EncryptionConverter encryptionConverter,
      @NonNull final NullableEncryptionConverter nullableEncryptionConverter) {
    return userValues -> {
      final RequiredEncryptedNameWithHash firstName =
          new RequiredEncryptedNameWithHash(userValues.firstName());
      final RequiredEncryptedNameWithHash lastName =
          new RequiredEncryptedNameWithHash(userValues.lastName());
      final RequiredEncryptedEmailWithHash email =
          new RequiredEncryptedEmailWithHash(userValues.email());
      final NullableEncryptedPhoneWithHash phone =
          new NullableEncryptedPhoneWithHash(userValues.phone());
      return new MapSqlParameterSource()
          .addValue("userId", userValues.id())
          .addValue(
              "firstNameEncrypted",
              encryptionConverter.convertToDatabaseColumn(firstName.getEncrypted()))
          .addValue("firstNameHash", firstName.getHash())
          .addValue(
              "lastNameEncrypted",
              encryptionConverter.convertToDatabaseColumn(lastName.getEncrypted()))
          .addValue("lastNameHash", lastName.getHash())
          .addValue(
              "emailEncrypted", encryptionConverter.convertToDatabaseColumn(email.getEncrypted()))
          .addValue("emailHash", email.getHash())
          .addValue(
              "phoneEncrypted",
              nullableEncryptionConverter.convertToDatabaseColumn(phone.getEncrypted()))
          .addValue("phoneHash", phone.getHash());
    };
  }
}
