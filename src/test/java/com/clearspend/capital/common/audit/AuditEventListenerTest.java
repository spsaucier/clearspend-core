package com.clearspend.capital.common.audit;

import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class AuditEventListenerTest {

  private AccountingAuditProcessor processor;
  private AccountingAuditEventPublisher publisher;

  private AuditEventListener underTest;

  @BeforeEach
  public void setup() {
    processor = Mockito.mock(AccountingAuditProcessor.class);
    publisher = Mockito.mock(AccountingAuditEventPublisher.class);
    underTest = new AuditEventListener(processor, publisher);
  }

  @Test
  @SneakyThrows
  public void testOnAccountingAuditEvent() {
    AccountActivityAuditEvent event =
        new AccountActivityAuditEvent(
            this,
            "test messsage",
            AccountActivityAuditEvent.TYPE_NOTES_ADD,
            "businessID1",
            "userid1",
            "activity1");
    underTest.onAccountingAuditEvent(event);
    Mockito.verify(processor).storeAccountingActivityEventToBigTable(event);
  }
}
