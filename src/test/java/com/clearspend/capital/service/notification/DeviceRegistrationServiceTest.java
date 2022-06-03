package com.clearspend.capital.service.notification;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.TestHelper.CreateBusinessRecord;
import com.clearspend.capital.data.model.DeviceRegistration;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
class DeviceRegistrationServiceTest extends BaseCapitalTest {

  @Autowired private TestHelper testHelper;

  @Autowired private DeviceRegistrationService deviceRegistrationService;

  @Test
  void findDeviceRegistrationByUserId() {
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    testHelper.setCurrentUser(createBusinessRecord.user());
    deviceRegistrationService.saveUpdateDeviceTokens(
        createBusinessRecord.user().getId(), List.of("test"));
    Assertions.assertArrayEquals(
        new String[] {"test"},
        deviceRegistrationService
            .findAllByUserId(createBusinessRecord.user().getId())
            .orElse(new DeviceRegistration())
            .getDeviceIds());
  }
}
