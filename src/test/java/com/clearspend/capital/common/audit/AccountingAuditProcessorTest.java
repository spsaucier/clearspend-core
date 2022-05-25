package com.clearspend.capital.common.audit;

import com.clearspend.capital.client.google.BigTableClient;
import com.clearspend.capital.data.audit.AccountActivityAuditEvent;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class AccountingAuditProcessorTest {

  private BigTableClient bigTableClient;

  private AccountingAuditProcessor underTest;

  @BeforeEach
  public void setup() {
    bigTableClient = Mockito.mock(BigTableClient.class);
    underTest = new AccountingAuditProcessor(bigTableClient);
  }

  @Test
  @SneakyThrows
  public void testStoreAccountingEventToBigTable() {
    Map<String, String> columnMap = new HashMap<>();
    columnMap.put("testKye", "testValue");
    columnMap.put("userid", "userid1");
    AccountActivityAuditEvent event =
        new AccountActivityAuditEvent(this, columnMap, "businessID1", "userid1", "activityId1");
    String rowKey = underTest.storeAccountingActivityEventToBigTable(event);
    Mockito.verify(bigTableClient)
        .saveOneRow("audit-table", rowKey, AccountActivityAuditEvent.COLUMN_FAMILY, columnMap);
  }

  @Test
  public void testReverseDateCalc() {
    String actual = AccountingAuditProcessor.getReversedDateString();
    LocalDateTime currentTime = LocalDateTime.now(ZoneOffset.systemDefault());
    String futureDate = currentTime.plusDays(1).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    String reversedFuture =
        String.valueOf(AccountingAuditProcessor.MAX_DATE - Integer.valueOf(futureDate));
    Assertions.assertTrue(actual.compareTo(reversedFuture) > 0);
  }

  @Test
  public void testCodatSyncEventSent() {
    Map<String, String> data = new HashMap<>();
    data.put(CodatSyncEventType.SUPPLIER_SYNC_TO_CODAT.toString(), "my new supplier");
    AccountingCodatSyncAuditEvent event =
        new AccountingCodatSyncAuditEvent(this, data, "bussinessId", "userId");
    String rowKey = underTest.storeAccountingCodatSyncAuditEventToBigTable(event);
    Mockito.verify(bigTableClient)
        .saveOneRow("audit-table", rowKey, AccountingCodatSyncAuditEvent.COLUMN_FAMILY, data);
  }

  @Test
  public void testGetActualDate() {
    OffsetDateTime actual =
        AccountingAuditProcessor.getActualDate(AccountingAuditProcessor.getReversedDateString());
    OffsetDateTime expected =
        OffsetDateTime.of(LocalDate.from(LocalDateTime.now()), LocalTime.MIDNIGHT, ZoneOffset.UTC);
    Assertions.assertEquals(actual, expected);
  }
}
