package com.clearspend.capital.common.audit;

import com.clearspend.capital.client.google.BigTableClient;
import java.util.HashMap;
import java.util.Map;
import lombok.SneakyThrows;
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
    AccountActivityAuditEvent event =
        new AccountActivityAuditEvent(
            this,
            "test messsage",
            AccountActivityAuditEvent.TYPE_NOTES_ADD,
            "businessID1",
            "userid1",
            "activityId1");
    Map<String, String> columnMap = new HashMap<>();
    columnMap.put(event.getEventType(), event.getMessage());
    String rowKey = underTest.storeAccountingActivityEventToBigTable(event);
    Mockito.verify(bigTableClient).saveOneRow("audit-table", rowKey, "cfaa", columnMap);
  }
}
