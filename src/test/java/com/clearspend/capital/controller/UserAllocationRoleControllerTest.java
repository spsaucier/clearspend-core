package com.clearspend.capital.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.controller.type.userAllocationRole.UserAllocationRoleRecord;
import com.clearspend.capital.controller.type.userAllocationRole.UserAllocationRolesResponse;
import com.clearspend.capital.data.model.Allocation;
import com.clearspend.capital.data.model.User;
import com.clearspend.capital.service.AllocationService;
import java.util.Map;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

@RequiredArgsConstructor(onConstructor = @__({@Autowired}))
@Slf4j
@Transactional
public class UserAllocationRoleControllerTest extends BaseCapitalTest {

  private final MockMvc mvc;
  private final TestHelper testHelper;
  private final AllocationService allocationService;
  private final EntityManager entityManager;

  @SneakyThrows
  @Test
  void createAllocationPostPermissionPutChangeDelete() {
    testHelper.init();
    final Allocation rootAllocation =
        allocationService.getRootAllocation(testHelper.retrieveBusiness().getId()).allocation();
    final User rootAllocationOwner = rootAllocation.getOwner();
    User manager = testHelper.createUser(testHelper.retrieveBusiness()).user();

    entityManager.flush();
    testHelper.login(rootAllocationOwner); // Has Admin permissions by default
    // Set a permission on the manager
    mvc.perform(
            post("/user-allocation-roles/%s/%s"
                    .formatted(
                        rootAllocation.getId().toUuid().toString(),
                        manager.getId().toUuid().toString()))
                .contentType(MediaType.TEXT_PLAIN)
                .content("Manager")
                .cookie(testHelper.getDefaultAuthCookie()))
        .andExpect(status().is2xxSuccessful())
        .andReturn()
        .getResponse();

    // Get back 2 permissions, one default for the owner and the other for the manager
    entityManager.flush();
    Map<TypedId<UserId>, UserAllocationRoleRecord> permissions = getPermissions(rootAllocation);

    assertEquals("Manager", permissions.get(manager.getId()).getRole());
    assertEquals("Admin", permissions.get(rootAllocation.getOwnerId()).getRole());
    assertEquals(2, permissions.size());

    // Now edit the permission - give the second user only read permission
    mvc.perform(
            put("/user-allocation-roles/%s/%s"
                    .formatted(
                        rootAllocation.getId().toUuid().toString(),
                        manager.getId().toUuid().toString()))
                .contentType(MediaType.TEXT_PLAIN)
                .content("View only")
                .cookie(testHelper.getDefaultAuthCookie()))
        .andExpect(status().is2xxSuccessful())
        .andReturn()
        .getResponse();
    entityManager.flush();

    // And check it
    permissions = getPermissions(rootAllocation);
    assertEquals("View only", permissions.get(manager.getId()).getRole());
    assertEquals("Admin", permissions.get(rootAllocation.getOwnerId()).getRole());
    assertEquals(2, permissions.size());

    // Then delete the manager user's permission
    mvc.perform(
            delete(
                    "/user-allocation-roles/%s/%s"
                        .formatted(
                            rootAllocation.getId().toUuid().toString(),
                            manager.getId().toUuid().toString()))
                .contentType(MediaType.TEXT_PLAIN)
                .cookie(testHelper.getDefaultAuthCookie()))
        .andExpect(status().is2xxSuccessful())
        .andReturn()
        .getResponse();
    entityManager.flush();

    // And check it
    permissions = getPermissions(rootAllocation);
    assertEquals("Admin", permissions.get(rootAllocation.getOwner().getId()).getRole());
    assertEquals(1, permissions.size());
  }

  @NotNull
  private Map<TypedId<UserId>, UserAllocationRoleRecord> getPermissions(Allocation rootAllocation)
      throws Exception {
    MockHttpServletResponse response =
        mvc.perform(
                get("/user-allocation-roles/%s"
                        .formatted(rootAllocation.getId().toUuid().toString()))
                    .contentType("application/json")
                    .cookie(testHelper.getDefaultAuthCookie()))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
    log.info(response.getContentAsString());

    UserAllocationRolesResponse allocationRolesResponse =
        objectMapper.readValue(response.getContentAsString(), UserAllocationRolesResponse.class);

    return allocationRolesResponse.getUserAllocationRoleRecordList().stream()
        .collect(Collectors.toMap(e -> e.getUser().getUserId(), e -> e));
  }
}
