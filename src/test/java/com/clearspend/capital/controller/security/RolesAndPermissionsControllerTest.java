package com.clearspend.capital.controller.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.TestHelper.CreateBusinessRecord;
import com.clearspend.capital.controller.type.security.UserRolesAndPermissionsRecord;
import com.clearspend.capital.data.model.enums.AllocationPermission;
import java.util.EnumSet;
import javax.servlet.http.Cookie;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

@RequiredArgsConstructor(onConstructor = @__({@Autowired}))
@Slf4j
class RolesAndPermissionsControllerTest extends BaseCapitalTest {

  private final MockMvc mvc;
  private final TestHelper testHelper;

  @Test
  @SneakyThrows
  public void testGettingUserRolesAndPermissions() {
    CreateBusinessRecord createBusinessRecord = testHelper.init();
    final Cookie cookie = createBusinessRecord.authCookie();
    UserRolesAndPermissionsRecord userRolesAndPermissionsRecord =
        getRoles(
            "/roles-and-permissions/allocation/%s"
                .formatted(
                    createBusinessRecord
                        .allocationRecord()
                        .allocation()
                        .getId()
                        .toUuid()
                        .toString()),
            cookie);

    assertEquals(
        createBusinessRecord.user().getId(), userRolesAndPermissionsRecord.getUser().getUserId());
    assertEquals(
        EnumSet.allOf(AllocationPermission.class),
        EnumSet.copyOf(userRolesAndPermissionsRecord.getAllocationPermissions()));
    assertTrue(userRolesAndPermissionsRecord.getGlobalUserPermissions().isEmpty());

    userRolesAndPermissionsRecord =
        getRoles(
            "/roles-and-permissions/business/%s".formatted(createBusinessRecord.business().getId()),
            cookie);

    assertEquals(
        createBusinessRecord.allocationRecord().allocation().getId(),
        userRolesAndPermissionsRecord.getAllocationId());

    userRolesAndPermissionsRecord = getRoles("/roles-and-permissions/", cookie);

    assertEquals(
        createBusinessRecord.allocationRecord().allocation().getId(),
        userRolesAndPermissionsRecord.getAllocationId());
    assertEquals(
        EnumSet.allOf(AllocationPermission.class),
        EnumSet.copyOf(userRolesAndPermissionsRecord.getAllocationPermissions()));
    assertTrue(userRolesAndPermissionsRecord.getGlobalUserPermissions().isEmpty());
  }

  @Test
  @SneakyThrows
  public void testGettingEmptyPermissionsRecords() {
    CreateBusinessRecord createBusinessRecord = testHelper.init();

    CreateBusinessRecord secondBusinessRecord = testHelper.createBusiness();
    final Cookie secondCookie = secondBusinessRecord.authCookie();

    UserRolesAndPermissionsRecord userRolesAndPermissionsRecord =
        getRoles(
            "/roles-and-permissions/allocation/%s"
                .formatted(
                    createBusinessRecord
                        .allocationRecord()
                        .allocation()
                        .getId()
                        .toUuid()
                        .toString()),
            secondCookie);

    assertEquals(
        secondBusinessRecord.user().getId(), userRolesAndPermissionsRecord.getUser().getUserId());
    assertTrue(userRolesAndPermissionsRecord.getAllocationPermissions().isEmpty());
    assertTrue(userRolesAndPermissionsRecord.getGlobalUserPermissions().isEmpty());
  }

  private UserRolesAndPermissionsRecord getRoles(String url, Cookie cookie) throws Exception {
    MockHttpServletResponse response =
        mvc.perform(get(url).contentType("application/json").cookie(cookie))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

    return objectMapper.readValue(
        response.getContentAsString(), UserRolesAndPermissionsRecord.class);
  }
}
