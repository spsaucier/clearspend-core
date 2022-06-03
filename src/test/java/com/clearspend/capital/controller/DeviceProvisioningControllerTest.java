package com.clearspend.capital.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.TestHelper.CreateBusinessRecord;
import com.clearspend.capital.controller.type.notification.firebase.DeviceTokenRegistrationRequest;
import com.clearspend.capital.data.model.DeviceRegistration;
import com.clearspend.capital.data.repository.DeviceRepository;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

@RequiredArgsConstructor(onConstructor = @__({@Autowired}))
@Slf4j
class DeviceProvisioningControllerTest extends BaseCapitalTest {

  private final MockMvc mvc;
  private final TestHelper testHelper;
  private final DeviceRepository deviceRepository;

  @SneakyThrows
  @Test
  void saveDeviceTokens() {
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    testHelper.setCurrentUser(createBusinessRecord.user());

    DeviceTokenRegistrationRequest deviceTokenRegistrationRequest =
        new DeviceTokenRegistrationRequest();
    deviceTokenRegistrationRequest.setDeviceToken("testToken");

    String body = objectMapper.writeValueAsString(deviceTokenRegistrationRequest);

    mvc.perform(
            post("/device/provision/device-tokens")
                .contentType("application/json")
                .content(body)
                .cookie(createBusinessRecord.authCookie()))
        .andExpect(status().isOk());

    Assertions.assertArrayEquals(
        new String[] {"testToken"},
        deviceRepository
            .findAllByUserId(createBusinessRecord.user().getId())
            .orElse(new DeviceRegistration())
            .getDeviceIds());
  }

  @SneakyThrows
  @Test
  void updateDeviceTokens() {
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    testHelper.setCurrentUser(createBusinessRecord.user());

    DeviceTokenRegistrationRequest deviceTokenRegistrationRequest =
        new DeviceTokenRegistrationRequest();
    deviceTokenRegistrationRequest.setDeviceToken("testToken");

    String body = objectMapper.writeValueAsString(deviceTokenRegistrationRequest);

    mvc.perform(
            post("/device/provision/device-tokens")
                .contentType("application/json")
                .content(body)
                .cookie(createBusinessRecord.authCookie()))
        .andExpect(status().isOk());

    deviceTokenRegistrationRequest.setDeviceToken("testToken");
    deviceTokenRegistrationRequest.setDeviceToken("deviceToken2");

    body = objectMapper.writeValueAsString(deviceTokenRegistrationRequest);
    mvc.perform(
            post("/device/provision/device-tokens")
                .contentType("application/json")
                .content(body)
                .cookie(createBusinessRecord.authCookie()))
        .andExpect(status().isOk());

    Assertions.assertArrayEquals(
        new String[] {"deviceToken2", "testToken"},
        deviceRepository
            .findAllByUserId(createBusinessRecord.user().getId())
            .orElse(new DeviceRegistration())
            .getDeviceIds());
  }

  @SneakyThrows
  @Test
  void deleteDeviceTokens() {
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    testHelper.setCurrentUser(createBusinessRecord.user());

    DeviceTokenRegistrationRequest deviceTokenRegistrationRequest =
        new DeviceTokenRegistrationRequest();
    deviceTokenRegistrationRequest.setDeviceToken("testToken");

    String body = objectMapper.writeValueAsString(deviceTokenRegistrationRequest);
    // save first device token for user
    mvc.perform(
            post("/device/provision/device-tokens")
                .contentType("application/json")
                .content(body)
                .cookie(createBusinessRecord.authCookie()))
        .andExpect(status().isOk());

    deviceTokenRegistrationRequest.setDeviceToken("deviceToken2");
    body = objectMapper.writeValueAsString(deviceTokenRegistrationRequest);
    // save second device token for user
    mvc.perform(
            post("/device/provision/device-tokens")
                .contentType("application/json")
                .content(body)
                .cookie(createBusinessRecord.authCookie()))
        .andExpect(status().isOk());

    deviceTokenRegistrationRequest.setDeviceToken("deviceToken2");
    body = objectMapper.writeValueAsString(deviceTokenRegistrationRequest);
    // unregister one device token for user
    mvc.perform(
            delete("/device/provision/device-tokens")
                .contentType("application/json")
                .content(body)
                .cookie(createBusinessRecord.authCookie()))
        .andExpect(status().isOk());

    Assertions.assertArrayEquals(
        new String[] {"testToken"},
        deviceRepository
            .findAllByUserId(createBusinessRecord.user().getId())
            .orElse(new DeviceRegistration())
            .getDeviceIds());
  }

  @SneakyThrows
  @Test
  void deleteUserDevices() {
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    testHelper.setCurrentUser(createBusinessRecord.user());

    DeviceTokenRegistrationRequest deviceTokenRegistrationRequest =
        new DeviceTokenRegistrationRequest();
    deviceTokenRegistrationRequest.setDeviceToken("testToken");

    String body = objectMapper.writeValueAsString(deviceTokenRegistrationRequest);

    mvc.perform(
            post("/device/provision/device-tokens")
                .contentType("application/json")
                .content(body)
                .cookie(createBusinessRecord.authCookie()))
        .andExpect(status().isOk());

    deviceTokenRegistrationRequest.setDeviceToken("testToken");

    body = objectMapper.writeValueAsString(deviceTokenRegistrationRequest);
    mvc.perform(
            delete("/device/provision/device-tokens")
                .contentType("application/json")
                .content(body)
                .cookie(createBusinessRecord.authCookie()))
        .andExpect(status().isOk());

    Assertions.assertArrayEquals(
        null,
        deviceRepository
            .findAllByUserId(createBusinessRecord.user().getId())
            .orElse(new DeviceRegistration())
            .getDeviceIds());
  }
}
