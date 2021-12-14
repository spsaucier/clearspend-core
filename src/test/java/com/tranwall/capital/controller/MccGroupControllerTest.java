package com.tranwall.capital.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.tranwall.capital.BaseCapitalTest;
import com.tranwall.capital.MockMvcHelper;
import com.tranwall.capital.TestHelper;
import com.tranwall.capital.controller.type.mcc.MccGroup;
import com.tranwall.capital.data.model.Business;
import com.tranwall.capital.data.model.enums.I2CMccGroup;
import com.tranwall.capital.service.UserService.CreateUpdateUserRecord;
import java.util.EnumSet;
import java.util.List;
import javax.servlet.http.Cookie;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;

@RequiredArgsConstructor(onConstructor = @__({@Autowired}))
@Slf4j
public class MccGroupControllerTest extends BaseCapitalTest {

  private final TestHelper testHelper;
  private final MockMvcHelper mockMvcHelper;

  private Cookie userCookie;

  @BeforeEach
  @SneakyThrows
  void init() {
    testHelper.init();

    if (userCookie == null) {
      Business business = testHelper.retrieveBusiness();
      CreateUpdateUserRecord user = testHelper.createUser(business);
      userCookie = testHelper.login(user.user().getEmail().getEncrypted(), user.password());
    }
  }

  @Test
  @SneakyThrows
  void getMccGroups() {
    List<MccGroup> mccGroups =
        mockMvcHelper.queryList(
            "/mcc-groups", HttpMethod.GET, userCookie, null, new TypeReference<>() {});

    assertThat(mccGroups)
        .extracting("i2cMccGroupRef")
        .containsExactlyInAnyOrderElementsOf(EnumSet.allOf(I2CMccGroup.class));
  }
}
