package com.clearspend.capital.data.audit;

import com.clearspend.capital.common.audit.AccountingAuditEventPublisher;
import com.clearspend.capital.data.model.AccountActivity;
import com.clearspend.capital.data.model.embedded.ExpenseDetails;
import com.clearspend.capital.data.model.embedded.ReceiptDetails;
import java.util.HashSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class AccountActivityEntityEventListenerTest {

  private AccountingAuditEventPublisher publisher;

  private AccountActivityEntityEventListener underTest = new AccountActivityEntityEventListener();

  @BeforeEach
  void setUp() {
    publisher = Mockito.mock(AccountingAuditEventPublisher.class);
    underTest.init(publisher);
  }

  @Test
  void testNoEventPublishedWithEmptyStrting() {
    AccountActivity activity = new AccountActivity();
    activity.setNotes("");
    underTest.postUpdateAudit(activity);
    Mockito.verify(publisher, Mockito.times(0))
        .publishAccountActivityAuditEvent(Mockito.anyMap(), Mockito.anyString());
  }

  @Test
  void testNoEventPulishedWithEmptyReceipt() {
    AccountActivity activity = new AccountActivity();
    activity.setNotes("");
    activity.setReceipt(new ReceiptDetails());
    activity.getReceipt().setReceiptIds(new HashSet<>());
    underTest.postUpdateAudit(activity);
    Mockito.verify(publisher, Mockito.times(0))
        .publishAccountActivityAuditEvent(Mockito.anyMap(), Mockito.anyString());
  }

  @Test
  void testEventPublished() {
    AccountActivity activity = new AccountActivity();
    activity.setExpenseDetails(new ExpenseDetails());
    activity.getExpenseDetails().setCategoryName("new category name");
    underTest.postUpdateAudit(activity);
    Mockito.verify(publisher, Mockito.times(1))
        .publishAccountActivityAuditEvent(Mockito.anyMap(), Mockito.anyString());
  }
}
