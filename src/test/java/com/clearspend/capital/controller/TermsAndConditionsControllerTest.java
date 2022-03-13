package com.clearspend.capital.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.TestHelper;
import javax.servlet.http.Cookie;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;
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
    Cookie authCookie = createBusinessRecord.authCookie();
    MockHttpServletResponse response =
        mvc.perform(
                get("/terms-and-conditions/timestamp-details")
                    .contentType("application/json")
                    .cookie(authCookie))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
    log.info(response.getContentAsString());
  }
}
