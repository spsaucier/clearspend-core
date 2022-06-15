package com.clearspend.capital.crypto.data.repository;

import com.clearspend.capital.crypto.data.model.Key;
import com.clearspend.capital.util.function.NullableFunctions;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

@Repository
public class KeyRepositoryImpl implements KeyRepository {
  private final NamedParameterJdbcTemplate jdbcTemplate;

  public KeyRepositoryImpl(final NamedParameterJdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Autowired
  public KeyRepositoryImpl(final DataSource dataSource) {
    this.jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
  }

  @Override
  @SneakyThrows
  public List<Key> findAll() {
    return jdbcTemplate.query(
        "select k.id, k.created, k.key_ref, k.key_hash from Key k",
        (rs, rn) -> {
          final Key key = new Key(rs.getInt(3), rs.getBytes(4));
          key.setId(UUID.fromString(rs.getString(1)));
          key.setCreated(
              OffsetDateTime.ofInstant(
                  Instant.ofEpochMilli(rs.getTimestamp(2).getTime()), ZoneOffset.UTC));
          return key;
        });
  }

  @Override
  @SneakyThrows
  public <S extends Key> S save(S key) {
    NullableFunctions.doIfNull(key.getId(), () -> key.setId(UUID.randomUUID()));
    NullableFunctions.doIfNull(key.getCreated(), () -> key.setCreated(OffsetDateTime.now()));
    final SqlParameterSource params =
        new MapSqlParameterSource()
            .addValue("id", key.getId())
            .addValue(
                "created",
                Timestamp.valueOf(
                    key.getCreated().atZoneSameInstant(ZoneOffset.UTC).toLocalDateTime()))
            .addValue("keyRef", key.getKeyRef())
            .addValue("keyHash", key.getKeyHash());
    jdbcTemplate.update(
        "insert into key (id, created, key_ref, key_hash) values(:id,:created,:keyRef,:keyHash)",
        params);
    return key;
  }
}
