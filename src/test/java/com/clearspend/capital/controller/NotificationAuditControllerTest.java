package com.clearspend.capital.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.TestHelper.CreateBusinessRecord;
import com.clearspend.capital.controller.type.notification.NotificationHistoryResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

@RequiredArgsConstructor(onConstructor = @__({@Autowired}))
@Slf4j
public class NotificationAuditControllerTest extends BaseCapitalTest {

  private final MockMvc mvc;
  private final TestHelper testHelper;

  @Test
  void testRetrieveNotificationHistory() throws Exception {
    CreateBusinessRecord createBusinessRecord = testHelper.init();
    MockHttpServletResponse response =
        mvc.perform(
                get("/notifications?limit=1")
                    .contentType("application/json")
                    .cookie(createBusinessRecord.authCookie()))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

    List<NotificationHistoryResponse> notificationHistory =
        objectMapper.readValue(response.getContentAsString(), new TypeReference<>() {});

    Assertions.assertEquals(2, notificationHistory.size());
    Assertions.assertEquals(notificationHistory.get(0).getMessage(), "This is the message");
  }
}
