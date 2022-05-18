package com.clearspend.capital.client.google;

import com.google.cloud.bigtable.data.v2.models.Row;
import com.google.cloud.bigtable.data.v2.models.RowCell;
import com.google.protobuf.ByteString;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Profile("test")
@Component
@Slf4j
public class MockBigTableClient extends BigTableClient {

  Map<String, String> mockBigTable = new HashMap<>();

  public MockBigTableClient(@NonNull BigTableProperties bigTableProperties) {
    super(bigTableProperties);
  }

  @Override
  public void saveOneRow(
      @NonNull String tableName,
      @NonNull String rowKey,
      @NonNull String columFamily,
      @NonNull Map<String, String> columnData) {
    log.info(
        "On accounting audit application event: tableId is {}, rowKey is {}, columnFamily is {}",
        tableName,
        rowKey,
        columFamily);
    for (Map.Entry<String, String> set : columnData.entrySet()) {
      mockBigTable.put(rowKey, set.getValue());
      log.info(
          "the column name/qualifier is: {}, the value is : {} ", set.getKey(), set.getValue());
    }
  }

  @Override
  public Row readOneRow(@NonNull String tableName, String rowKey) {
    if (mockBigTable.containsKey(rowKey)) {
      return new Row() {
        @NotNull
        @Override
        public ByteString getKey() {
          return null;
        }

        @Override
        public List<RowCell> getCells() {
          return null;
        }
      };
    }
    return null;
  }

  @Override
  public void appendToExistingRow(
      @NonNull String tableName,
      @NonNull String rowKey,
      @NonNull String columFamily,
      @NonNull Map<String, String> columnData) {
    log.info(
        "On accounting audit application event: tableId is {}, rowKey is {}, columnFamily is {}",
        tableName,
        rowKey,
        columFamily);
    for (Map.Entry<String, String> set : columnData.entrySet()) {
      String existingValue = mockBigTable.get(rowKey);
      mockBigTable.put(rowKey, existingValue + set.getValue());
      log.info(
          "the column name/qualifier is: {}, the value is : {} ",
          set.getKey(),
          existingValue + set.getValue());
    }
  }
}
