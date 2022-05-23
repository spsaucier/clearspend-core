package com.clearspend.capital.client.google;

import static com.google.cloud.bigtable.data.v2.models.Filters.FILTERS;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.api.gax.rpc.ServerStream;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.bigtable.data.v2.BigtableDataClient;
import com.google.cloud.bigtable.data.v2.BigtableDataSettings;
import com.google.cloud.bigtable.data.v2.models.Filters;
import com.google.cloud.bigtable.data.v2.models.Query;
import com.google.cloud.bigtable.data.v2.models.ReadModifyWriteRow;
import com.google.cloud.bigtable.data.v2.models.Row;
import com.google.cloud.bigtable.data.v2.models.RowMutation;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Profile("!test")
@Component
public class BigTableClient {

  private final BigTableProperties bigTableProperties;
  private BigtableDataSettings settings;

  public BigTableClient(@NonNull BigTableProperties bigTableProperties) {
    this.bigTableProperties = bigTableProperties;
    FixedCredentialsProvider provider = null;
    try {
      provider =
          FixedCredentialsProvider.create(
              GoogleCredentials.fromStream(
                  new ByteArrayInputStream(bigTableProperties.getCredentials().getBytes())));
      this.settings =
          BigtableDataSettings.newBuilder()
              .setCredentialsProvider(provider)
              .setProjectId(bigTableProperties.getProjectId())
              .setInstanceId(bigTableProperties.getInstanceId())
              .build();
    } catch (IOException e) {
      log.error("BigTableClient: SA file path is not configured correctly", e);
    } catch (Exception e) {
      log.error("BigTableClient: bigtable env is not configured properly", e);
    }
  }

  public void saveOneRow(
      @NonNull String tableName,
      @NonNull String rowKey,
      @NonNull String columFamily,
      @NonNull Map<String, String> columnData) {
    long timestamp = System.currentTimeMillis() * 1000;
    try (BigtableDataClient dataClient = BigtableDataClient.create(settings)) {
      RowMutation newRow = RowMutation.create(tableName, rowKey);
      for (Map.Entry<String, String> set : columnData.entrySet()) {
        newRow.setCell(columFamily, set.getKey(), timestamp, set.getValue());
      }
      dataClient.mutateRow(newRow);
    } catch (Exception e) {
      log.error("BigTableClient insertRow failed", e);
    }
  }

  public Row readOneRow(@NonNull String tableName, String rowKey) {
    try (BigtableDataClient dataClient = BigtableDataClient.create(settings)) {
      return dataClient.readRow(tableName, rowKey);
    } catch (Exception e) {
      log.error("BigTableClient readOneRow failed", e);
    }
    return null;
  }

  public Row readOneRowByFamily(@NonNull String tableName, String rowKey, String familyName) {
    try (BigtableDataClient dataClient = BigtableDataClient.create(settings)) {
      Filters.Filter filter = FILTERS.chain().filter(FILTERS.family().exactMatch(familyName));
      return dataClient.readRow(tableName, rowKey, filter);
    } catch (Exception e) {
      log.error("BigTableClient readOneRowByFamily failed", e);
    }
    return null;
  }

  public ServerStream<Row> readMultipleRow(
      @NonNull String tableName, @NonNull List<String> rowKeys) {
    try (BigtableDataClient dataClient = BigtableDataClient.create(settings)) {
      Query query = Query.create(tableName);
      rowKeys.stream().forEach(key -> query.rowKey(key));
      return dataClient.readRows(query);
    } catch (Exception e) {
      log.error("BigTableClient readMultipleRow failed", e);
    }
    return null;
  }

  public ServerStream<Row> readMultipleRowWithFilter(
      @NonNull String tableName, @NonNull String regex, String familyName) {
    try (BigtableDataClient dataClient = BigtableDataClient.create(settings)) {

      Filters.Filter filter = null;
      if (StringUtils.isNoneBlank(familyName)) {
        filter =
            FILTERS
                .chain()
                .filter(FILTERS.key().regex(regex))
                .filter(FILTERS.family().exactMatch(familyName));
      } else {
        filter = FILTERS.chain().filter(FILTERS.key().regex(regex));
      }
      Query query = Query.create(tableName).filter(filter);
      return dataClient.readRows(query);
    } catch (Exception e) {
      log.error("BigTableClient readMultipleRowWithFilter failed", e);
    }
    return null;
  }

  public ServerStream<Row> readRangedRow(
      @NonNull String tableName, @NonNull String start, @NonNull String end) {
    try (BigtableDataClient dataClient = BigtableDataClient.create(settings)) {
      Query query = Query.create(tableName).range(start, end);
      return dataClient.readRows(query);
    } catch (Exception e) {
      log.error("BigTableClient readOneRow failed", e);
    }
    return null;
  }

  public ServerStream<Row> readRowsByCustomFilter(
      @NonNull String tableName, @NonNull Filters.Filter filter) {
    try (BigtableDataClient dataClient = BigtableDataClient.create(settings)) {
      Query query = Query.create(tableName).filter(filter);
      return dataClient.readRows(query);
    } catch (Exception e) {
      log.error("BigTableClient readOneRow failed", e);
    }
    return null;
  }

  public void appendToExistingRow(
      @NonNull String tableName,
      @NonNull String rowKey,
      @NonNull String columFamily,
      @NonNull Map<String, String> columnData) {
    try (BigtableDataClient dataClient = BigtableDataClient.create(settings)) {
      ReadModifyWriteRow mutation = ReadModifyWriteRow.create(tableName, rowKey);
      for (Map.Entry<String, String> set : columnData.entrySet()) {
        mutation.append(columFamily, set.getKey(), set.getValue());
      }
      dataClient.readModifyWriteRow(mutation);
    } catch (Exception e) {
      log.error("BigTableClient appendToExistingRow failed", e);
    }
  }
}
