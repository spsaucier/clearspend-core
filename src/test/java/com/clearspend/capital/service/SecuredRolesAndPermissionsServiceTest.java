package com.clearspend.capital.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.TestHelper.CreateBusinessRecord;
import com.clearspend.capital.common.data.dao.UserRolesAndPermissions;
import com.clearspend.capital.data.model.User;
import com.clearspend.capital.data.model.enums.AllocationPermission;
import com.clearspend.capital.data.model.enums.GlobalUserPermission;
import com.clearspend.capital.data.model.enums.UserType;
import com.clearspend.capital.data.model.security.AllocationRolePermissions;
import com.clearspend.capital.data.model.security.DefaultRoles;
import com.clearspend.capital.data.repository.security.AllocationRolePermissionsRepository;
import com.clearspend.capital.service.FusionAuthService.RoleChange;
import com.clearspend.capital.testutils.permission.PermissionValidationHelper;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.ThrowingSupplier;
import org.springframework.beans.factory.annotation.Autowired;

@RequiredArgsConstructor(onConstructor = @__({@Autowired}))
public class SecuredRolesAndPermissionsServiceTest extends BaseCapitalTest {
  private final TestHelper testHelper;
  private final PermissionValidationHelper permissionValidationHelper;
  private final FusionAuthService fusionAuthService;
  private final EntityManager entityManager;
  private final AllocationRolePermissionsRepository allocationRolePermissionsRepo;
  private final RolesAndPermissionsService rolesAndPermissionsService;
  private final SecuredRolesAndPermissionsService secureRolesAndPermissionsService;
  private CreateBusinessRecord createBusinessRecord;

  @BeforeEach
  void setup() {
    createBusinessRecord = testHelper.createBusiness();
  }

  @Test
  void getAllPermissionsForUserInBusiness() {
    testHelper.setCurrentUser(createBusinessRecord.user());
    final EnumSet<AllocationPermission> adminPermissions =
        allocationRolePermissionsRepo
            .findByRoleNameAndBusinessId(DefaultRoles.ALLOCATION_ADMIN, null)
            .map(AllocationRolePermissions::getPermissions)
            .stream()
            .flatMap(Stream::of)
            .collect(Collectors.toCollection(() -> EnumSet.noneOf(AllocationPermission.class)));
    entityManager.flush();
    final List<UserRolesAndPermissions> permissions =
        secureRolesAndPermissionsService.getAllPermissionsForUserInBusiness(
            createBusinessRecord.user().getId(), createBusinessRecord.business().getId());
    final UserRolesAndPermissions expected =
        new UserRolesAndPermissions(
            createBusinessRecord.user().getFirstName().getEncrypted(),
            createBusinessRecord.user().getLastName().getEncrypted(),
            UserType.BUSINESS_OWNER,
            createBusinessRecord.user().getId(),
            createBusinessRecord.allocationRecord().allocation().getId(),
            null,
            createBusinessRecord.business().getId(),
            false,
            DefaultRoles.ALLOCATION_ADMIN,
            adminPermissions,
            EnumSet.noneOf(GlobalUserPermission.class));
    assertThat(permissions).hasSize(1).contains(expected);
  }

  @Test
  @SneakyThrows
  void getAllPermissionsForUserInBusiness_UserPermissions() {
    testHelper.setCurrentUser(createBusinessRecord.user());
    final User user =
        testHelper
            .createUserWithRole(
                createBusinessRecord.allocationRecord().allocation(),
                DefaultRoles.ALLOCATION_EMPLOYEE)
            .user();
    entityManager.flush();

    final ThrowingSupplier<List<UserRolesAndPermissions>> action =
        () ->
            secureRolesAndPermissionsService.getAllPermissionsForUserInBusiness(
                user.getId(), createBusinessRecord.business().getId());

    permissionValidationHelper
        .buildValidator(createBusinessRecord)
        .build()
        .validateServiceMethod(action::get);

    final User employee =
        testHelper
            .createUserWithRole(
                createBusinessRecord.allocationRecord().allocation(),
                DefaultRoles.ALLOCATION_EMPLOYEE)
            .user();
    testHelper.setCurrentUser(employee);
    final List<UserRolesAndPermissions> employeeResults = action.get();
    assertThat(employeeResults).isEmpty();

    final User admin =
        testHelper
            .createUserWithRole(
                createBusinessRecord.allocationRecord().allocation(), DefaultRoles.ALLOCATION_ADMIN)
            .user();
    testHelper.setCurrentUser(admin);
    final List<UserRolesAndPermissions> adminResults = action.get();
    assertThat(adminResults).hasSize(1);

    // Owner should still see their permissions
    testHelper.setCurrentUser(user);
    final List<UserRolesAndPermissions> ownerResults = action.get();
    assertThat(ownerResults).hasSize(1);

    testHelper.setUserAsMaster(createBusinessRecord.user());
    final User customerServiceUser = testHelper.createUser(createBusinessRecord.business()).user();
    fusionAuthService.changeUserRole(
        RoleChange.GRANT,
        customerServiceUser.getSubjectRef(),
        DefaultRoles.GLOBAL_CUSTOMER_SERVICE);
    testHelper.setCurrentUser(customerServiceUser);
    final List<UserRolesAndPermissions> customerServiceResults = action.get();
    assertThat(customerServiceResults).hasSize(1);
  }
}
