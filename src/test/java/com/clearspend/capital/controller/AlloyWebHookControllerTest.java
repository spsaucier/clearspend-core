package com.clearspend.capital.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.controller.type.review.AlloyWebHookResponse;
import com.clearspend.capital.controller.type.review.Data;
import com.clearspend.capital.controller.type.review.GroupManualReviewOutcome;
import java.util.ArrayList;
import java.util.List;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

@RequiredArgsConstructor(onConstructor = @__({@Autowired}))
@Slf4j
@Transactional
public class AlloyWebHookControllerTest extends BaseCapitalTest {
  private final MockMvc mvc;
  private final TestHelper testHelper;

  @BeforeEach
  void init() {
    testHelper.init();
  }

  @Test
  @SneakyThrows
  void testUpdateHookFromAlloy() {
    TestHelper.CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();

    Data data =
        new Data(
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            GroupManualReviewOutcome.APPROVED.getValue(),
            "",
            List.of(""),
            "",
            "",
            "",
            "",
            null,
            null,
            new ArrayList<>());
    AlloyWebHookResponse alloyWebHookResponse = new AlloyWebHookResponse("", "", "", "", data);
    String body = objectMapper.writeValueAsString(alloyWebHookResponse);

    mvc.perform(
            patch("/alloy/webhook")
                .contentType("application/json")
                .content(body)
                .cookie(createBusinessRecord.authCookie()))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse();
  }
}
