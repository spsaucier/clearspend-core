package com.clearspend.capital.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.MockMvcHelper;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.data.model.business.Business;
import com.clearspend.capital.data.model.enums.MccGroup;
import com.clearspend.capital.service.UserService.CreateUpdateUserRecord;
import com.fasterxml.jackson.core.type.TypeReference;
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

    assertThat(mccGroups).containsExactlyInAnyOrderElementsOf(EnumSet.allOf(MccGroup.class));
  }
}
