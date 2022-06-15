package com.clearspend.capital.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.TestHelper.CreateBusinessRecord;
import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.crypto.data.model.embedded.RequiredEncryptedNameWithHash;
import com.clearspend.capital.data.model.AccountActivity;
import com.clearspend.capital.data.model.Allocation;
import com.clearspend.capital.data.model.Receipt;
import com.clearspend.capital.data.model.TransactionSyncLog;
import com.clearspend.capital.data.model.User;
import com.clearspend.capital.data.model.business.Business;
import com.clearspend.capital.data.model.embedded.AllocationDetails;
import com.clearspend.capital.data.model.embedded.UserDetails;
import com.clearspend.capital.data.model.enums.AccountActivityIntegrationSyncStatus;
import com.clearspend.capital.data.model.enums.AccountActivityStatus;
import com.clearspend.capital.data.model.enums.AccountActivityType;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.data.model.enums.TransactionSyncStatus;
import com.clearspend.capital.data.model.security.DefaultRoles;
import com.clearspend.capital.data.repository.AccountActivityRepository;
import com.clearspend.capital.data.repository.ReceiptRepository;
import com.clearspend.capital.data.repository.TransactionSyncLogRepository;
import com.clearspend.capital.service.UserService.CreateUpdateUserRecord;
import com.clearspend.capital.testutils.data.TestDataHelper;
import com.clearspend.capital.testutils.data.TestDataHelper.AccountActivityConfig;
import com.clearspend.capital.testutils.data.TestDataHelper.ReceiptConfig;
import com.clearspend.capital.testutils.permission.PermissionValidationHelper;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.function.ThrowingRunnable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;

@Slf4j
class ReceiptServiceTest extends BaseCapitalTest {

  @Autowired private TestHelper testHelper;
  @Autowired private ReceiptImageService receiptImageService;
  @Autowired private ReceiptService receiptService;

  @Autowired private ReceiptRepository receiptRepository;
  @Autowired private AccountActivityRepository accountActivityRepository;
  @Autowired private AdjustmentService adjustmentService;
  @Autowired private TransactionSyncLogRepository transactionSyncLogRepository;
  @Autowired private PermissionValidationHelper permissionValidationHelper;
  @Autowired private TestDataHelper testDataHelper;

  private CreateUpdateUserRecord userRecord;
  private String fileContents;
  private String contentType;
  private Receipt receipt;
  private Receipt unlinkedReceipt;

  private CreateBusinessRecord createBusinessRecord;
  private Allocation allocation;

  @SneakyThrows
  @BeforeEach
  public void setup() {
    createBusinessRecord = testHelper.createBusiness();
    Business business = createBusinessRecord.business();
    allocation = createBusinessRecord.allocationRecord().allocation();
    testHelper.setCurrentUser(createBusinessRecord.user());
    userRecord =
        testHelper.createUserWithRole(
            createBusinessRecord.allocationRecord().allocation(), DefaultRoles.ALLOCATION_EMPLOYEE);
    fileContents = "Hello world " + UUID.randomUUID();
    contentType = "application/pdf";
    receipt =
        receiptService.storeReceiptImage(
            userRecord.user().getBusinessId(),
            userRecord.user().getId(),
            fileContents.getBytes(),
            contentType);
    unlinkedReceipt = new Receipt(userRecord.user().getBusinessId(), userRecord.user().getId());
    unlinkedReceipt.setPath(
        receiptImageService.getReceiptPath(
            userRecord.user().getBusinessId(), userRecord.user().getId(), receipt.getId()));
    receiptRepository.save(unlinkedReceipt);
  }

  @SneakyThrows
  @Test
  void storeReceiptImage_success() {
    Receipt receipt =
        receiptService.storeReceiptImage(
            userRecord.user().getBusinessId(),
            userRecord.user().getId(),
            fileContents.getBytes(),
            contentType);
    assertThat(receipt).isNotNull();
  }

  @SneakyThrows
  @Test
  void getReceiptImage_success() {
    testHelper.setCurrentUser(userRecord.user());
    byte[] bytes = receiptService.getReceiptImage(receipt);
    assertThat(bytes).isEqualTo(fileContents.getBytes());
  }

  @Test
  void getReceipt_success() {
    Receipt foundReceipt = receiptService.getReceipt(receipt.getId());
    assertThat(foundReceipt.getPath()).isEqualTo(receipt.getPath());
  }

  @Test
  void getReceipt_UserPermissions() {
    final User uploadOwnerEmployee =
        testHelper
            .createUserWithRole(
                createBusinessRecord.allocationRecord().allocation(),
                DefaultRoles.ALLOCATION_EMPLOYEE)
            .user();
    final User activityOwnerEmployee =
        testHelper
            .createUserWithRole(
                createBusinessRecord.allocationRecord().allocation(),
                DefaultRoles.ALLOCATION_EMPLOYEE)
            .user();

    final Receipt receipt =
        testDataHelper.createReceipt(
            ReceiptConfig.fromCreateBusinessRecord(createBusinessRecord).build());
    receipt.setUploadUserId(uploadOwnerEmployee.getId());

    final AccountActivity accountActivity =
        testDataHelper.createAccountActivity(
            AccountActivityConfig.fromCreateBusinessRecord(createBusinessRecord)
                .owner(activityOwnerEmployee)
                .build());
    accountActivity.getReceipt().getReceiptIds().add(receipt.getId());
    accountActivityRepository.save(accountActivity);
    receipt.addLinkUserId(activityOwnerEmployee.getId());
    receiptRepository.save(receipt);
    final ThrowingRunnable action = () -> receiptService.getReceipt(receipt.getId());

    permissionValidationHelper
        .buildValidator(createBusinessRecord)
        .allowRolesOnAllocation(
            Set.of(
                DefaultRoles.ALLOCATION_ADMIN,
                DefaultRoles.ALLOCATION_MANAGER,
                DefaultRoles.ALLOCATION_VIEW_ONLY))
        .allowUser(uploadOwnerEmployee)
        .allowUser(activityOwnerEmployee)
        .build()
        .validateServiceMethod(action);
  }

  @Test
  void getReceipts_success() {
    testHelper.setCurrentUser(userRecord.user());
    List<Receipt> foundReceipts = receiptService.getReceiptsForCurrentUser();
    assertThat(foundReceipts).hasSize(2);
    assertThat(foundReceipts).hasSameElementsAs(List.of(receipt, unlinkedReceipt));
  }

  @Test
  void deleteReceipt_success() {
    Receipt receipt = new Receipt(userRecord.user().getBusinessId(), userRecord.user().getId());
    receipt.setAllocationId(createBusinessRecord.allocationRecord().allocation().getId());
    receipt.setPath(
        receiptImageService.getReceiptPath(
            userRecord.user().getBusinessId(), userRecord.user().getId(), this.receipt.getId()));
    receiptRepository.save(receipt);
    testHelper.setCurrentUser(userRecord.user());
    receiptService.deleteReceipt(receipt);
  }

  @SneakyThrows
  @Test
  void linkReceipts_whenAccountActivityAlreadySynced_thenReceiptIsUploaded() {
    testHelper.setCurrentUser(createBusinessRecord.user());
    receipt =
        receiptService.storeReceiptImage(
            userRecord.user().getBusinessId(),
            createBusinessRecord.user().getId(),
            fileContents.getBytes(),
            contentType);

    // Create an Account Activity object to 'link'
    AccountActivity accountActivity = createAccountActivity(createBusinessRecord.user());

    TransactionSyncLog log = new TransactionSyncLog();
    log.setBusinessId(createBusinessRecord.user().getBusinessId());
    log.setStatus(TransactionSyncStatus.COMPLETED);
    log.setAccountActivityId(accountActivity.getId());
    log.setDirectCostPushOperationKey("whatever");
    log.setFirstName(new RequiredEncryptedNameWithHash("first"));
    log.setLastName(new RequiredEncryptedNameWithHash("last"));
    log = transactionSyncLogRepository.save(log);

    receiptService.linkReceipt(receipt, accountActivity);

    assertThat(transactionSyncLogRepository.findById(log.getId()))
        .isPresent()
        .get()
        .extracting(TransactionSyncLog::getStatus)
        .isEqualTo(TransactionSyncStatus.UPLOADED_RECEIPTS);
  }

  @SneakyThrows
  @Tag("PERMISSIONS")
  @Test
  void getReceiptImage_requiresOwnershipOrReadAllocationPermissions() {
    testHelper.setCurrentUser(createBusinessRecord.user());
    // Create three users to test.
    User manager =
        testHelper.createUserWithRole(allocation, DefaultRoles.ALLOCATION_MANAGER).user();
    User employee =
        testHelper.createUserWithRole(allocation, DefaultRoles.ALLOCATION_EMPLOYEE).user();
    User snooper =
        testHelper.createUserWithRole(allocation, DefaultRoles.ALLOCATION_EMPLOYEE).user();

    // Create a Receipt for the Employee user
    receipt =
        receiptService.storeReceiptImage(
            userRecord.user().getBusinessId(),
            employee.getId(),
            fileContents.getBytes(),
            contentType);

    // Ensure that the Manager can access the Receipt Image by virtue of the READ permission
    testHelper.setCurrentUser(manager);
    assertThat(receiptService.getReceiptImage(receipt)).isEqualTo(fileContents.getBytes());

    // The Employee that owns the Receipt should also be able to call the method
    testHelper.setCurrentUser(employee);
    assertThat(receiptService.getReceiptImage(receipt)).isEqualTo(fileContents.getBytes());

    // The sibling employee (Snooper) should NOT be allowed to see the Receipt Image
    testHelper.setCurrentUser(snooper);
    assertThrows(AccessDeniedException.class, () -> receiptService.getReceiptImage(receipt));
  }

  @SneakyThrows
  @Tag("PERMISSIONS")
  @Test
  void getReceipt_requiresOwnershipOrReadAllocationPermissions() {
    testHelper.setCurrentUser(createBusinessRecord.user());
    // Create three users to test.
    User manager =
        testHelper.createUserWithRole(allocation, DefaultRoles.ALLOCATION_MANAGER).user();
    User employee =
        testHelper.createUserWithRole(allocation, DefaultRoles.ALLOCATION_EMPLOYEE).user();
    User snooper =
        testHelper.createUserWithRole(allocation, DefaultRoles.ALLOCATION_EMPLOYEE).user();

    // Create a Receipt for the Employee user
    receipt =
        receiptService.storeReceiptImage(
            userRecord.user().getBusinessId(),
            employee.getId(),
            fileContents.getBytes(),
            contentType);

    // Ensure that the Manager can access the Receipt by virtue of the READ permission
    testHelper.setCurrentUser(manager);
    assertThat(receiptService.getReceipt(receipt.getId())).isEqualTo(receipt);

    // The Employee that owns the Receipt should also be able to call the method
    testHelper.setCurrentUser(employee);
    assertThat(receiptService.getReceipt(receipt.getId())).isEqualTo(receipt);

    // The sibling employee (Snooper) should NOT be allowed to see the Receipt
    testHelper.setCurrentUser(snooper);
    assertThrows(AccessDeniedException.class, () -> receiptService.getReceipt(receipt.getId()));
  }

  @SneakyThrows
  @Tag("PERMISSIONS")
  @Test
  void getReceiptsForCurrentUser_requiresOwnership() {
    testHelper.setCurrentUser(createBusinessRecord.user());
    // Create three users to test.
    User manager =
        testHelper.createUserWithRole(allocation, DefaultRoles.ALLOCATION_MANAGER).user();
    User employee =
        testHelper.createUserWithRole(allocation, DefaultRoles.ALLOCATION_EMPLOYEE).user();
    User snooper =
        testHelper.createUserWithRole(allocation, DefaultRoles.ALLOCATION_EMPLOYEE).user();

    // Create a Receipt for the Employee user
    receipt =
        receiptService.storeReceiptImage(
            userRecord.user().getBusinessId(),
            employee.getId(),
            fileContents.getBytes(),
            contentType);

    // Ensure that the Manager can NOT access the Receipt
    testHelper.setCurrentUser(manager);
    assertThat(receiptService.getReceiptsForCurrentUser()).doesNotContain(receipt);

    // The Employee that owns the Receipt should get the Receipt back
    testHelper.setCurrentUser(employee);
    assertThat(receiptService.getReceiptsForCurrentUser()).contains(receipt);

    // The sibling employee (Snooper) should NOT be allowed to see the Receipt
    testHelper.setCurrentUser(snooper);
    assertThat(receiptService.getReceiptsForCurrentUser()).doesNotContain(receipt);
  }

  @SneakyThrows
  @Tag("PERMISSIONS")
  @Test
  void linkReceipt_UserPermissions() {
    testHelper.setCurrentUser(createBusinessRecord.user());
    final User employee =
        testHelper.createUserWithRole(allocation, DefaultRoles.ALLOCATION_EMPLOYEE).user();
    final AccountActivity accountActivity = createAccountActivity(employee);
    receipt =
        receiptService.storeReceiptImage(
            userRecord.user().getBusinessId(),
            employee.getId(),
            fileContents.getBytes(),
            contentType);

    final ThrowingRunnable action = () -> receiptService.linkReceipt(receipt, accountActivity);

    permissionValidationHelper
        .buildValidator(createBusinessRecord)
        .allowRolesOnAllocation(
            Set.of(
                DefaultRoles.ALLOCATION_ADMIN,
                DefaultRoles.ALLOCATION_MANAGER,
                DefaultRoles.ALLOCATION_VIEW_ONLY))
        .allowUser(employee)
        .build()
        .validateServiceMethod(action);
  }

  @SneakyThrows
  @Tag("PERMISSIONS")
  @Test
  void unlinkReceipt_requiresOwnershipOrLinkReceiptsAndReadPermission() {
    testHelper.setCurrentUser(createBusinessRecord.user());
    // Create three users to test.
    User manager =
        testHelper.createUserWithRole(allocation, DefaultRoles.ALLOCATION_MANAGER).user();
    User employee =
        testHelper.createUserWithRole(allocation, DefaultRoles.ALLOCATION_EMPLOYEE).user();
    User snooper =
        testHelper.createUserWithRole(allocation, DefaultRoles.ALLOCATION_EMPLOYEE).user();

    // Create a Receipt for the Employee user
    receipt =
        receiptService.storeReceiptImage(
            userRecord.user().getBusinessId(),
            employee.getId(),
            fileContents.getBytes(),
            contentType);

    // Create an Account Activity object to 'link'
    AccountActivity accountActivity = createAccountActivity(employee);

    // Ensure that the Manager can link a receipt
    testHelper.setCurrentUser(employee);
    receiptService.linkReceipt(receipt, accountActivity);
    Receipt managerReceipt = receiptRepository.getById(receipt.getId());
    testHelper.setCurrentUser(manager);
    assertThat(
            accountActivityRepository.getById(accountActivity.getId()).getReceipt().getReceiptIds())
        .contains(managerReceipt.getId());
    assertDoesNotThrow(() -> receiptService.unlinkReceipt(managerReceipt, accountActivity));
    assertThat(
            accountActivityRepository.getById(accountActivity.getId()).getReceipt().getReceiptIds())
        .doesNotContain(managerReceipt.getId());

    // The Employee that owns the Receipt and the Activity should be able to link the two
    testHelper.setCurrentUser(employee);
    receiptService.linkReceipt(receipt, accountActivity);
    Receipt employeeReceipt = receiptRepository.getById(receipt.getId());
    assertThat(
            accountActivityRepository.getById(accountActivity.getId()).getReceipt().getReceiptIds())
        .contains(employeeReceipt.getId());
    testHelper.setCurrentUser(employee);
    assertDoesNotThrow(() -> receiptService.unlinkReceipt(employeeReceipt, accountActivity));
    assertThat(
            accountActivityRepository.getById(accountActivity.getId()).getReceipt().getReceiptIds())
        .doesNotContain(employeeReceipt.getId());

    // The sibling employee (Snooper) should NOT be allowed to link the Receipt
    testHelper.setCurrentUser(employee);
    receiptService.linkReceipt(receipt, accountActivity);
    Receipt snooperReceipt = receiptRepository.getById(receipt.getId());
    assertThat(
            accountActivityRepository.getById(accountActivity.getId()).getReceipt().getReceiptIds())
        .contains(snooperReceipt.getId());
    testHelper.setCurrentUser(snooper);
    assertThrows(
        AccessDeniedException.class,
        () -> receiptService.unlinkReceipt(snooperReceipt, accountActivity));
    assertThat(
            accountActivityRepository.getById(accountActivity.getId()).getReceipt().getReceiptIds())
        .contains(snooperReceipt.getId());
  }

  @SneakyThrows
  @Tag("PERMISSIONS")
  @Test
  void deleteReceipt_requiresOwnership() {
    testHelper.setCurrentUser(createBusinessRecord.user());
    // Create three users to test.
    User manager =
        testHelper.createUserWithRole(allocation, DefaultRoles.ALLOCATION_MANAGER).user();
    User employee =
        testHelper.createUserWithRole(allocation, DefaultRoles.ALLOCATION_EMPLOYEE).user();
    User snooper =
        testHelper.createUserWithRole(allocation, DefaultRoles.ALLOCATION_EMPLOYEE).user();

    // Create a Receipt for the Employee user
    receipt =
        receiptService.storeReceiptImage(
            userRecord.user().getBusinessId(),
            employee.getId(),
            fileContents.getBytes(),
            contentType);

    // Create an Account Activity object to 'link'
    AccountActivity accountActivity = createAccountActivity(employee);

    // Ensure that the Manager can NOT delete a receipt
    testHelper.setCurrentUser(manager);
    assertThrows(AccessDeniedException.class, () -> receiptService.deleteReceipt(receipt));

    // The Employee that owns the Receipt and the Activity should be able to link the two
    testHelper.setCurrentUser(employee);
    assertDoesNotThrow(() -> receiptService.linkReceipt(receipt, accountActivity));

    // The sibling employee (Snooper) should NOT be allowed to link the Receipt
    testHelper.setCurrentUser(snooper);
    assertThrows(
        AccessDeniedException.class, () -> receiptService.linkReceipt(receipt, accountActivity));
  }

  private AccountActivity createAccountActivity(User user) {
    AccountActivity accountActivity =
        new AccountActivity(
            createBusinessRecord.business().getId(),
            AccountActivityType.BANK_DEPOSIT_STRIPE,
            AccountActivityStatus.APPROVED,
            AllocationDetails.of(createBusinessRecord.allocationRecord().allocation()),
            OffsetDateTime.now(),
            Amount.of(createBusinessRecord.business().getCurrency(), BigDecimal.ONE),
            Amount.of(createBusinessRecord.business().getCurrency(), BigDecimal.ONE),
            AccountActivityIntegrationSyncStatus.NOT_READY);
    accountActivity.setAccountId(createBusinessRecord.allocationRecord().account().getId());
    accountActivity.setNotes("");
    accountActivity.setUser(UserDetails.of(user));
    accountActivity.setAdjustmentId(
        adjustmentService
            .recordManualAdjustment(
                createBusinessRecord.allocationRecord().account(),
                Amount.of(Currency.USD, BigDecimal.valueOf(42.00)))
            .adjustment()
            .getId());
    accountActivityRepository.save(accountActivity);
    return accountActivity;
  }
}
