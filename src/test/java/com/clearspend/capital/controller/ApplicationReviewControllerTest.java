package com.clearspend.capital.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.controller.type.review.ApplicationReviewRequirements;
import com.clearspend.capital.data.repository.business.BusinessProspectRepository;
import com.clearspend.capital.data.repository.business.BusinessRepository;
import java.nio.charset.StandardCharsets;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
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
public class ApplicationReviewControllerTest extends BaseCapitalTest {
  private final MockMvc mvc;
  private final TestHelper testHelper;

  private final BusinessProspectRepository businessProspectRepository;
  private final BusinessRepository businessRepository;

  @Test
  @SneakyThrows
  void testGetRequiredDocumentsForManualReview_whenThereAreNoEntitiesToReview() {
    TestHelper.CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    MockHttpServletResponse response =
        mvc.perform(
                get("/application-review/requirement")
                    .contentType("application/json")
                    .cookie(createBusinessRecord.authCookie()))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

    ApplicationReviewRequirements manualReviewResponse =
        objectMapper.readValue(response.getContentAsString(), ApplicationReviewRequirements.class);
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
        MockMvcRequestBuilders.multipart("/application-review/document");

    MockHttpServletResponse response =
        mvc.perform(
                multipartRequest.file(mockMultipartFile).cookie(createBusinessRecord.authCookie()))
            .andExpect(status().is5xxServerError())
            .andReturn()
            .getResponse();
  }
}
