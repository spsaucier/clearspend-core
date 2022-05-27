package com.clearspend.capital.data.repository.impl;

import com.clearspend.capital.common.data.util.MustacheSqlBeanPropertyContext;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.data.repository.impl.JDBCUtils.MustacheQueryConfig.QueryType;
import com.samskivert.mustache.Template;
import java.io.StringWriter;
import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import org.hibernate.Session;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

public class JDBCUtils {

  static final UUID NULL_UUID = new UUID(0L, 0L);

  public record CountObjectForSqlQuery(Boolean count, OffsetDateTime javaNow) {
    public CountObjectForSqlQuery(Boolean count) {
      this(count, OffsetDateTime.now());
    }
  }

  public static final Map<String, Boolean> JMUSTACHE_COUNT_CONTEXT = Map.of("count", true);
  public static final Map<String, Boolean> JMUSTACHE_ENTITY_CONTEXT = Map.of("count", false);

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

  public static <R> R queryForObject(
      EntityManager entityManager,
      String sql,
      SqlParameterSource paramSource,
      RowMapper<R> rowMapper) {
    return entityManager
        .unwrap(Session.class)
        .doReturningWork(
            connection ->
                new NamedParameterJdbcTemplate(new SingleConnectionDataSource(connection, true))
                    .queryForObject(sql, paramSource, rowMapper));
  }

  public static <R> R query(
      EntityManager entityManager,
      String sql,
      SqlParameterSource paramSource,
      ResultSetExtractor<R> resultSetExtractor) {
    return entityManager
        .unwrap(Session.class)
        .doReturningWork(
            connection ->
                new NamedParameterJdbcTemplate(new SingleConnectionDataSource(connection, true))
                    .query(sql, paramSource, resultSetExtractor));
  }

  public static <R> R execute(
      EntityManager entityManager,
      String sql,
      SqlParameterSource paramSource,
      PreparedStatementCallback<R> action) {
    return entityManager
        .unwrap(Session.class)
        .doReturningWork(
            connection ->
                new NamedParameterJdbcTemplate(new SingleConnectionDataSource(connection, true))
                    .execute(sql, paramSource, action));
  }

  /**
   * Read a TypedId off a ResultSet
   *
   * @param resultSet The result set from which to read a result
   * @param field the name of the field in the result set
   * @param <T> The sentinel interface for TypedId
   * @return null if the DB had a null or the special value {@link #NULL_UUID}, the TypedId
   *     otherwise.
   * @throws SQLException if the DB goes sideways reading from the resultset
   */
  public static <T> TypedId<T> getTypedId(@NonNull ResultSet resultSet, @NonNull final String field)
      throws SQLException {
    UUID uuid = resultSet.getObject(field, UUID.class);
    return uuid == null || uuid.equals(NULL_UUID) ? null : new TypedId<>(uuid);
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

  /**
   * Convert the given TypedId into a UUID using the NULL_UUID constant
   *
   * @param id some TypedId
   * @return the UUID from the given TypedId or NULL_UUID
   */
  public static UUID safeUUID(TypedId<?> id) {
    return id == null ? NULL_UUID : id.toUuid();
  }

  /**
   * Generates a sql query from a JMustache template
   *
   * @param sqlTemplate JMustache query
   * @param criteria a context object could be a pojo or a map
   * @param forCount flag to indicate if count mode should be enabled for the template. Template
   *     should have this implemented otherwise it will have no effect
   * @return generated sql query
   */
  public static String generateQuery(Template sqlTemplate, Object criteria, boolean forCount) {
    StringWriter writer = new StringWriter();
    sqlTemplate.execute(
        criteria,
        forCount ? JDBCUtils.JMUSTACHE_COUNT_CONTEXT : JDBCUtils.JMUSTACHE_ENTITY_CONTEXT,
        writer);

    return writer.toString();
  }

  record QueryAndResult<T>(String query, List<T> result) {}

  public static <T> QueryAndResult<T> executeMustacheQuery(
      @NonNull final EntityManager entityManager,
      @NonNull final Template sqlTemplate,
      @NonNull final MustacheQueryConfig<T> config) {
    final Map<String, Object> context = getMustacheContext(config.getParameterSource());
    final Map<String, Boolean> parentContext =
        config.forCount ? JMUSTACHE_COUNT_CONTEXT : JMUSTACHE_ENTITY_CONTEXT;

    final StringWriter writer = new StringWriter();
    sqlTemplate.execute(context, parentContext, writer);
    final String query = writer.toString();
    final List<T> result =
        switch (config.getQueryType()) {
          case NATIVE_QUERY -> executeNativeQuery(
              entityManager, query, config.getEntityClass(), config.getParameterSource());
          case JDBC_TEMPLATE -> executeJdbcTemplate(
              entityManager, query, config.getRowMapper(), config.getParameterSource());
          case UNKNOWN -> throw new IllegalStateException(
              "Mustache query config incomplete, cannot determine type of query to execute");
        };
    return new QueryAndResult<>(query, result);
  }

  private static <T> List<T> executeJdbcTemplate(
      @NonNull final EntityManager entityManager,
      @NonNull final String query,
      @NonNull final RowMapper<T> rowMapping,
      @Nullable final SqlParameterSource parameterSource) {
    final SqlParameterSource source =
        Optional.ofNullable(parameterSource).orElse(new MapSqlParameterSource());
    return entityManager
        .unwrap(Session.class)
        .doReturningWork(
            connection ->
                new NamedParameterJdbcTemplate(new SingleConnectionDataSource(connection, true))
                    .query(query, source, rowMapping));
  }

  public static <T> List<T> executeNativeQuery(
      @NonNull final EntityManager entityManager,
      @NonNull final String query,
      @NonNull final Class<T> entity,
      @Nullable final SqlParameterSource parameterSource) {
    final Query nativeQuery = entityManager.createNativeQuery(query, entity);
    final SqlParameterSource source =
        Optional.ofNullable(parameterSource).orElse(new MapSqlParameterSource());
    final String[] paramNames =
        Optional.ofNullable(source.getParameterNames()).orElse(new String[0]);
    Arrays.stream(paramNames)
        .forEach(name -> safelySetParameter(nativeQuery, name, source.getValue(name)));
    return nativeQuery.getResultList();
  }

  private static void safelySetParameter(final Query query, final String name, final Object value) {
    try {
      query.setParameter(name, value);
    } catch (IllegalArgumentException ex) {
      if (!ex.getMessage().contains("Could not locate named parameter")) {
        throw ex;
      }
      // Otherwise suppress because NativeQueries don't like it when a parameter does not exist in
      // the SQL
    }
  }

  private static Map<String, Object> getMustacheContext(
      @Nullable final SqlParameterSource parameterSource) {
    if (parameterSource == null) {
      return Map.of();
    } else if (parameterSource instanceof MapSqlParameterSource mParam) {
      return mParam.getValues();
    } else if (parameterSource instanceof BeanPropertySqlParameterSource bParam) {
      return new MustacheSqlBeanPropertyContext(bParam);
    }
    throw new IllegalArgumentException(
        "Invalid parameter source type: %s".formatted(parameterSource.getClass().getName()));
  }

  @Getter
  @Builder
  public static class MustacheQueryConfig<T> {
    private final SqlParameterSource parameterSource;
    @Builder.Default private final boolean forCount = false;
    /** Using an Entity class allows for automatic mapping if a full entity is being retrieved. */
    private final Class<T> entityClass;
    /** Using a RowMapper allows for manual mapping of custom results. */
    private final RowMapper<T> rowMapper;

    public QueryType getQueryType() {
      if (entityClass != null) {
        return QueryType.NATIVE_QUERY;
      } else if (rowMapper != null) {
        return QueryType.JDBC_TEMPLATE;
      } else {
        return QueryType.UNKNOWN;
      }
    }

    public enum QueryType {
      NATIVE_QUERY,
      JDBC_TEMPLATE,
      UNKNOWN
    }
  }
}
