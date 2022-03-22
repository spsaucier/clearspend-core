package com.clearspend.capital.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.controller.type.termsAndConditions.TermsAndConditionsResponse;
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
public class TermsAndConditionsControllerTest extends BaseCapitalTest {

  private final TestHelper testHelper;
  private final MockMvc mvc;
  private TestHelper.CreateBusinessRecord createBusinessRecord;

  @BeforeEach
  void init() {
    createBusinessRecord = testHelper.init();
  }

  @Test
  @SneakyThrows
  void testCheckingTimeStampDetails() {
    log.info(
        mvc.perform(
                get("/terms-and-conditions/timestamp-details")
                    .contentType("application/json")
                    .cookie(createBusinessRecord.authCookie()))
            .andReturn()
            .getResponse()
            .getContentAsString(),
        TermsAndConditionsResponse.class);
  }
}
