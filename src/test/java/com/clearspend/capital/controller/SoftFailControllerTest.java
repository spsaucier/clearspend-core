package com.clearspend.capital.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.common.typedid.data.BusinessProspectId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.controller.type.business.prospect.SetBusinessProspectPasswordRequest;
import com.clearspend.capital.controller.type.review.SoftFailRequiredDocumentsResponse;
import com.clearspend.capital.crypto.PasswordUtil;
import com.clearspend.capital.data.repository.BusinessProspectRepository;
import com.clearspend.capital.data.repository.BusinessRepository;
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
public class SoftFailControllerTest extends BaseCapitalTest {
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
                get("/manual-review")
                    .contentType("application/json")
                    .cookie(createBusinessRecord.authCookie()))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

    SoftFailRequiredDocumentsResponse manualReviewResponse =
        objectMapper.readValue(
            response.getContentAsString(), SoftFailRequiredDocumentsResponse.class);
  }

  String setBusinessProspectPassword(TypedId<BusinessProspectId> businessProspectId)
      throws Exception {
    String password = PasswordUtil.generatePassword();
    SetBusinessProspectPasswordRequest request = new SetBusinessProspectPasswordRequest(password);
    String body = objectMapper.writeValueAsString(request);

    MockHttpServletResponse response =
        mvc.perform(
                post(String.format("/business-prospects/%s/password", businessProspectId))
                    .contentType("application/json")
                    .content(body))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
    return password;
  }
  //
  //  @Test
  //  @SneakyThrows
  //  void testGetRequiredDocumentsForManualReview_whenThereIsOneEntryToReview() {
  //    mockServerHelper.expectOtpViaEmail();
  //    mockServerHelper.expectOtpViaSms();
  //    mockServerHelper.expectEmailVerification("777888999");
  //    mockServerHelper.expectPhoneVerification("766255906");
  //    BusinessProspect businessProspect = testHelper.createBusinessProspect();
  //    testHelper.validateBusinessProspectIdentifier(
  //        IdentifierType.EMAIL, businessProspect.getId(), "777888999");
  //    testHelper.setBusinessProspectPhone(businessProspect.getId());
  //    testHelper.validateBusinessProspectIdentifier(
  //        IdentifierType.PHONE, businessProspect.getId(), "766255906");
  //    String password = setBusinessProspectPassword(businessProspect.getId());
  //    Cookie cookie = testHelper.login(businessProspect.getEmail().getEncrypted(), password);
  //    ConvertBusinessProspectResponse businessReview =
  //        testHelper.convertBusinessProspect("BusinessReview", businessProspect.getId());
  //
  //    MockHttpServletResponse response =
  //        mvc.perform(get("/manual-review").contentType("application/json").cookie(cookie))
  //            .andExpect(status().isOk())
  //            .andReturn()
  //            .getResponse();
  //
  //    SoftFailRequiredDocumentsResponse manualReviewResponse =
  //        objectMapper.readValue(
  //            response.getContentAsString(), SoftFailRequiredDocumentsResponse.class);
  //  }
  //
  //  @Test
  //  @SneakyThrows
  //  void testGetRequiredDocumentsForManualReview_whenThereAreDocumentsForBusinessAndTwoOwners() {
  //    TestHelper.CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
  //    MockHttpServletResponse response =
  //        mvc.perform(
  //                get("/manual-review")
  //                    .contentType("application/json")
  //                    .cookie(createBusinessRecord.authCookie()))
  //            .andExpect(status().isOk())
  //            .andReturn()
  //            .getResponse();
  //
  //    SoftFailRequiredDocumentsResponse manualReviewResponse =
  //        objectMapper.readValue(
  //            response.getContentAsString(), SoftFailRequiredDocumentsResponse.class);
  //  }

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
}
