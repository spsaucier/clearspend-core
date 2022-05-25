package com.clearspend.capital.service;

import com.clearspend.capital.client.google.BigTableClient;
import com.clearspend.capital.common.audit.AccountingCodatSyncAuditEvent;
import com.clearspend.capital.data.audit.AccountActivityAuditEvent;
import com.clearspend.capital.data.audit.AccountActivityAuditLog;
import com.clearspend.capital.data.audit.AccountingAuditResponse;
import com.clearspend.capital.data.audit.CodatSyncLogValue;
import java.util.ArrayList;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class AccountingAuditLogServiceTest {

  private BigTableClient bigTableClient;

  private UserService userService;

  private AccountingAuditLogService underTest;

  @BeforeEach
  public void setup() {
    bigTableClient = Mockito.mock(BigTableClient.class);
    userService = Mockito.mock(UserService.class);
    underTest = new AccountingAuditLogService(bigTableClient, userService);
  }

  @Test
  @SneakyThrows
  public void testSearchSupplierCodatSyncByBusiness() {
    AccountingAuditResponse expected = new AccountingAuditResponse();
    List<CodatSyncLogValue> codatSyncLogList = new ArrayList<>();
    expected.setCodatSyncLogList(codatSyncLogList);
    Mockito.when(
            bigTableClient.readCodatSupplierSyncLogs(
                AccountingCodatSyncAuditEvent.ROW_KEY_PREFIX + "#" + "business01#.*$",
                AccountingCodatSyncAuditEvent.COLUMN_FAMILY,
                10))
        .thenReturn(expected);
    AccountingAuditResponse actual = underTest.searchSupplierCodatSyncByBusiness("business01", 10);
    Assertions.assertEquals(expected, actual);
  }

  @Test
  @SneakyThrows
  public void testSearchAccountActivityByBusiness() {
    AccountingAuditResponse expected = new AccountingAuditResponse();
    List<AccountActivityAuditLog> accountActivityAuditLogs = new ArrayList<>();
    expected.setAccountActivityAuditLogs(accountActivityAuditLogs);
    Mockito.when(
            bigTableClient.readAccountingTransactionActivityLog(
                AccountActivityAuditEvent.ROW_KEY_PREFIX + "#business01#.*$",
                AccountActivityAuditEvent.COLUMN_FAMILY,
                10))
        .thenReturn(expected);
    AccountingAuditResponse actual = underTest.searchAccountActivityByBusiness("business01", 10);
    Assertions.assertEquals(expected, actual);
  }
}
