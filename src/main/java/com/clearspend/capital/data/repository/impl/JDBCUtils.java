package com.clearspend.capital.data.repository.impl;

import com.clearspend.capital.common.typedid.data.TypedId;
import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import lombok.NonNull;
import lombok.SneakyThrows;
import org.hibernate.Session;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

public class JDBCUtils {

  static final UUID NULL_UUID = new UUID(0L, 0L);

  /**
   * Run a query in the current context.
   *
   * @param entityManager from Hibernate
   * @param sql the query, with named parameters
   * @param paramSource specifications of values of parameters
   * @param rowMapper how to interpret the results
   * @param <R> the type of each row
   * @return the results, as a list
   */
  public static <R> List<R> query(
      EntityManager entityManager,
      String sql,
      SqlParameterSource paramSource,
      RowMapper<R> rowMapper) {
    return entityManager
        .unwrap(Session.class)
        .doReturningWork(
            connection ->
                new NamedParameterJdbcTemplate(new SingleConnectionDataSource(connection, true))
                    .query(sql, paramSource, rowMapper));
  }

  /**
   * Read a TypedId off a ResultSet
   *
   * @param resultSet The result set from which to read a result
   * @param field the name of the field in the result set
   * @param <T> The sentinel interface for TypedId
   * @return null if the DB had a null or the special value {@link #NULL_UUID}, the TypedId
   *     otherwise.
   * @throws SQLException
   */
  public static <T> TypedId<T> getTypedId(@NonNull ResultSet resultSet, @NonNull final String field)
      throws SQLException {
    UUID uuid = resultSet.getObject(field, UUID.class);
    return uuid == null || uuid.equals(NULL_UUID) ? null : new TypedId<T>(uuid);
  }

  /**
   * @param resultSet The result set from which to read a result
   * @param fieldName the name of the field containing an array of enum values
   * @param enumClass The class being enumerated
   * @param <T> The class being enumerated
   * @return an EnumSet, or null if null was in the DB.
   */
  @SneakyThrows
  public static <T extends Enum<T>> EnumSet<T> getEnumSet(
      ResultSet resultSet, String fieldName, Class<T> enumClass) {
    Array elements = resultSet.getArray(fieldName);
    if (elements == null) {
      return null;
    }
    List<T> list =
        Arrays.stream((String[]) elements.getArray())
            .map(str -> Enum.valueOf(enumClass, str))
            .collect(Collectors.toList());

    return list.isEmpty() ? EnumSet.noneOf(enumClass) : EnumSet.copyOf(list);
  }
}
