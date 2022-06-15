package com.clearspend.capital.flyway.migration;

import com.clearspend.capital.util.function.ThrowableFunctions.ThrowingFunction;
import java.sql.Connection;
import java.util.Optional;
import javax.sql.DataSource;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class CapitalFlywayMigration extends BaseJavaMigration {
  private final DataSource dataSource;

  private Optional<DataSource> getDataSource() {
    return Optional.ofNullable(dataSource);
  }

  /** Perform the migration. When doing JUnit tests, feel free to pass null to this method */
  @Override
  @SneakyThrows
  public final void migrate(final Context context) throws Exception {
    final NamedParameterJdbcTemplate jdbcTemplate = createJdbcTemplate(context);
    performMigration(jdbcTemplate);
    if (TransactionSynchronizationManager.isActualTransactionActive()
        && getDataSource()
            .map(DataSourceUtils::getConnection)
            .map(ThrowingFunction.sneakyThrows(Connection::isClosed))
            .orElse(false)) {
      throw new IllegalStateException(
          "Must not explicitly close DataSource connection in Migration implementation, this will break JUnit tests");
    }
  }

  /**
   * SUPER IMPORTANT:
   *
   * <p>In order for implementations of this class to be test-able, it must re-use the same
   * connection used in our @Transactional tests. Within NamedParameterJdbcTemplate, it will use the
   * DataSource as a key to retrieve the existing Connection if there is one.
   */
  private NamedParameterJdbcTemplate createJdbcTemplate(final Context context) {
    final DataSource templateDataSource =
        getDataSource()
            .orElseGet(() -> new SingleConnectionDataSource(context.getConnection(), true));
    return new NamedParameterJdbcTemplate(templateDataSource);
  }

  protected abstract void performMigration(final NamedParameterJdbcTemplate jdbcTemplate)
      throws Exception;
}
