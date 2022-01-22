package com.clearspend.capital.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.TestHelper.CreateBusinessRecord;
import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.crypto.utils.CurrentUserSwitcher;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.service.UserService.CreateUpdateUserRecord;
import java.math.BigDecimal;
import java.util.Collections;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;

@Slf4j
@Transactional
public class AllocationServiceTest extends BaseCapitalTest {

  @Autowired private TestHelper testHelper;
  @Autowired private RolesAndPermissionsService rolesAndPermissionsService;
  @Autowired private AllocationService allocationService;
  @Autowired private EntityManager entityManager;

  @Test
  void createAllocationCheckPermissions() {
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    CreateUpdateUserRecord peon = testHelper.createUser(createBusinessRecord.business());
    CurrentUserSwitcher.setCurrentUser(createBusinessRecord.user());
    assertDoesNotThrow(
        () ->
            allocationService.createAllocation(
                createBusinessRecord.business().getId(),
                createBusinessRecord.allocationRecord().allocation().getId(),
                "not root",
                createBusinessRecord.user(),
                new Amount(Currency.USD, new BigDecimal(0)),
                Collections.emptyMap(),
                Collections.emptyList(),
                Collections.emptySet()));

    CurrentUserSwitcher.setCurrentUser(createBusinessRecord.user());
    rolesAndPermissionsService.setUserAllocationRole(
        createBusinessRecord.allocationRecord().allocation().getId(),
        peon.user().getId(),
        "View only");
    entityManager.flush();

    CurrentUserSwitcher.setCurrentUser(peon.user());
    assertThrows(
        AccessDeniedException.class,
        () ->
            allocationService.createAllocation(
                createBusinessRecord.business().getId(),
                createBusinessRecord.allocationRecord().allocation().getId(),
                "also not root",
                peon.user(),
                new Amount(Currency.USD, new BigDecimal(0)),
                Collections.emptyMap(),
                Collections.emptyList(),
                Collections.emptySet()));
  }
}
