package com.clearspend.capital.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.TestHelper.CreateBusinessRecord;
import com.clearspend.capital.controller.type.common.PageRequest;
import com.clearspend.capital.data.model.User;
import com.clearspend.capital.data.model.security.DefaultRoles;
import com.clearspend.capital.service.AccountActivityServiceTest.FindPermissionUsers;
import com.clearspend.capital.testutils.data.TestDataHelper;
import com.clearspend.capital.testutils.data.TestDataHelper.AccountActivityConfig;
import java.time.OffsetDateTime;
import javax.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class ExportCSVServiceTest extends BaseCapitalTest {

  @Autowired TestHelper testHelper;
  @Autowired TestDataHelper testDataHelper;
  @Autowired EntityManager entityManager;
  @Autowired ExportCSVService exportCSVService;
  @Autowired AccountActivityService accountActivityService;

  @Test
  void createCSVFile_UserFilterPermissions() {
    final CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    final FindPermissionUsers users =
        testHelper.runWithCurrentUser(
            createBusinessRecord.user(),
            () -> {
              final User employeeOwner =
                  testHelper
                      .createUserWithRole(
                          createBusinessRecord.allocationRecord().allocation(),
                          DefaultRoles.ALLOCATION_EMPLOYEE)
                      .user();
              final User otherEmployee =
                  testHelper
                      .createUserWithRole(
                          createBusinessRecord.allocationRecord().allocation(),
                          DefaultRoles.ALLOCATION_EMPLOYEE)
                      .user();
              final User admin =
                  testHelper
                      .createUserWithRole(
                          createBusinessRecord.allocationRecord().allocation(),
                          DefaultRoles.ALLOCATION_ADMIN)
                      .user();
              final User manager =
                  testHelper
                      .createUserWithRole(
                          createBusinessRecord.allocationRecord().allocation(),
                          DefaultRoles.ALLOCATION_MANAGER)
                      .user();

              return new FindPermissionUsers(employeeOwner, otherEmployee, admin, manager);
            });
    testDataHelper.createAccountActivity(
        AccountActivityConfig.fromCreateBusinessRecord(createBusinessRecord)
            .owner(users.employeeOwner())
            .build());
    testDataHelper.createAccountActivity(
        AccountActivityConfig.fromCreateBusinessRecord(createBusinessRecord).build());
    testDataHelper.createAccountActivity(
        AccountActivityConfig.fromCreateBusinessRecord(createBusinessRecord).build());

    entityManager.flush();

    final AccountActivityFilterCriteria criteria = new AccountActivityFilterCriteria();
    criteria.setFrom(OffsetDateTime.now().minusYears(1));
    criteria.setTo(OffsetDateTime.now().plusYears(1));
    final PageRequest pageRequest = new PageRequest(0, 10);
    criteria.setPageToken(PageRequest.toPageToken(pageRequest));

    // Admin user should see all
    testHelper.setCurrentUser(users.admin());
    final byte[] adminBytes =
        exportCSVService.fromAccountActivity(
            accountActivityService
                .find(createBusinessRecord.business().getId(), criteria)
                .toList());
    assertEquals(4, new String(adminBytes).split("\n").length);

    // Manager user should see all
    testHelper.setCurrentUser(users.manager());
    final byte[] managerBytes =
        exportCSVService.fromAccountActivity(
            accountActivityService
                .find(createBusinessRecord.business().getId(), criteria)
                .toList());
    assertEquals(4, new String(managerBytes).split("\n").length);

    // Other Employee user should see none
    testHelper.setCurrentUser(users.otherEmployee());
    final byte[] otherEmployeeBytes =
        exportCSVService.fromAccountActivity(
            accountActivityService
                .find(createBusinessRecord.business().getId(), criteria)
                .toList());
    assertEquals(1, new String(otherEmployeeBytes).split("\n").length);

    // Employee owner should see self owned
    testHelper.setCurrentUser(users.employeeOwner());
    final byte[] employeeOwnerBytes =
        exportCSVService.fromAccountActivity(
            accountActivityService
                .find(createBusinessRecord.business().getId(), criteria)
                .toList());
    assertEquals(2, new String(employeeOwnerBytes).split("\n").length);
  }
}
