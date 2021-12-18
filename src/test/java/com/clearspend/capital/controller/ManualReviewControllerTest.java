package com.clearspend.capital.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.controller.type.review.AlloyWebHookResponse;
import com.clearspend.capital.controller.type.review.Data;
import com.clearspend.capital.controller.type.review.ManualReviewResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@RequiredArgsConstructor(onConstructor = @__({@Autowired}))
@Slf4j
@Transactional
public class ManualReviewControllerTest extends BaseCapitalTest {
  private final MockMvc mvc;
  private final TestHelper testHelper;

  @BeforeEach
  void init() {
    testHelper.init();
  }

  @Test
  @SneakyThrows
  void testGetRequiredDocumentsForManualReview() {
    TestHelper.CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    MockHttpServletResponse response =
        mvc.perform(
                get("/manual-review")
                    .contentType("application/json")
                    .cookie(createBusinessRecord.authCookie()))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

    ManualReviewResponse manualReviewResponse =
        objectMapper.readValue(response.getContentAsString(), ManualReviewResponse.class);
  }

  @Test
  @SneakyThrows
  void testUploadDocuments() {
    TestHelper.CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    MockMultipartFile mockMultipartFile =
        new MockMultipartFile(
            "documentList",
            "P-token|license|documentName.png",
            "image/png",
            "test.txt".getBytes(StandardCharsets.UTF_8));
    MockMultipartHttpServletRequestBuilder multipartRequest =
        MockMvcRequestBuilders.multipart("/manual-review");

    MockHttpServletResponse response =
        mvc.perform(
                multipartRequest.file(mockMultipartFile).cookie(createBusinessRecord.authCookie()))
            .andExpect(status().is5xxServerError())
            .andReturn()
            .getResponse();
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
            "",
            "",
            List.of(""),
            "",
            "",
            "",
            "",
            null,
            List.of(""),
            null);
    AlloyWebHookResponse alloyWebHookResponse = new AlloyWebHookResponse("", "", "", "", data);
    String body = objectMapper.writeValueAsString(alloyWebHookResponse);

    MockHttpServletResponse response =
        mvc.perform(
                patch("/manual-review/alloy/web-hook")
                    .contentType("application/json")
                    .content(body)
                    .cookie(createBusinessRecord.authCookie()))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
  }
}
