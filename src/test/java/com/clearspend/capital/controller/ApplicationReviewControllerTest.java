package com.clearspend.capital.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.common.data.model.Address;
import com.clearspend.capital.controller.type.review.ApplicationReviewRequirements;
import com.clearspend.capital.crypto.data.model.embedded.EncryptedString;
import com.clearspend.capital.data.model.business.Business;
import com.clearspend.capital.data.model.enums.Country;
import com.clearspend.capital.data.repository.business.BusinessRepository;
import com.clearspend.capital.service.BusinessOwnerService;
import com.clearspend.capital.service.BusinessOwnerService.BusinessOwnerAndStripePersonRecord;
import com.clearspend.capital.service.type.BusinessOwnerData;
import java.time.LocalDate;
import java.util.Collections;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@RequiredArgsConstructor(onConstructor = @__({@Autowired}))
@Slf4j
class ApplicationReviewControllerTest extends BaseCapitalTest {
  private final MockMvc mvc;
  private final TestHelper testHelper;
  private final BusinessOwnerService businessOwnerService;
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

    Assertions.assertEquals(
        new ApplicationReviewRequirements(
            Collections.emptyList(),
            Collections.emptyMap(),
            Collections.emptyList(),
            Collections.emptyList(),
            false,
            false,
            Collections.emptyList(),
            Collections.emptyList()),
        manualReviewResponse);
  }

  @Test
  @SneakyThrows
  void testUploadDocuments() {
    TestHelper.CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    Business business = createBusinessRecord.business();
    BusinessOwnerAndStripePersonRecord businessOwnerAndStripePersonRecord =
        businessOwnerService.updateBusinessOwnerAndStripePerson(
            business.getId(),
            new BusinessOwnerData(
                createBusinessRecord.businessOwner().getId(),
                business.getId(),
                createBusinessRecord.businessOwner().getFirstName().getEncrypted(),
                createBusinessRecord.businessOwner().getLastName().getEncrypted(),
                LocalDate.of(1998, 1, 1),
                testHelper.generateTaxIdentificationNumber(),
                createBusinessRecord.businessOwner().getRelationshipOwner(),
                createBusinessRecord.businessOwner().getRelationshipRepresentative(),
                createBusinessRecord.businessOwner().getRelationshipExecutive(),
                createBusinessRecord.businessOwner().getRelationshipDirector(),
                null,
                "Title",
                new Address(
                    new EncryptedString("13810 Shavano Wind"),
                    new EncryptedString("San Antonio, Texas(TX), 78230"),
                    "San Antonio",
                    "Texas",
                    new EncryptedString("78230"),
                    Country.USA),
                createBusinessRecord.businessOwner().getEmail().getEncrypted(),
                createBusinessRecord.businessOwner().getPhone().getEncrypted(),
                null,
                true));
    MockMultipartFile mockMultipartFile =
        new MockMultipartFile(
            "documentList",
            String.format(
                "%s|IDENTITY_DOCUMENT|passport.png",
                businessOwnerAndStripePersonRecord.businessOwner().getStripePersonReference()),
            "image/png",
            "passport.png".getBytes());
    MockMultipartHttpServletRequestBuilder multipartRequest =
        MockMvcRequestBuilders.multipart("/application-review/document");

    MockHttpServletResponse response =
        mvc.perform(
                multipartRequest.file(mockMultipartFile).cookie(createBusinessRecord.authCookie()))
            .andExpect(status().is2xxSuccessful())
            .andReturn()
            .getResponse();

    Assertions.assertEquals("Files successfully sent for review.", response.getContentAsString());
  }

  @SneakyThrows
  @Test
  void testUploadIdentityDocuments() {
    TestHelper.CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    Business business = createBusinessRecord.business();
    BusinessOwnerAndStripePersonRecord businessOwnerAndStripePersonRecord =
        businessOwnerService.updateBusinessOwnerAndStripePerson(
            business.getId(),
            new BusinessOwnerData(
                createBusinessRecord.businessOwner().getId(),
                business.getId(),
                createBusinessRecord.businessOwner().getFirstName().getEncrypted(),
                createBusinessRecord.businessOwner().getLastName().getEncrypted(),
                LocalDate.of(1998, 1, 1),
                testHelper.generateTaxIdentificationNumber(),
                createBusinessRecord.businessOwner().getRelationshipOwner(),
                createBusinessRecord.businessOwner().getRelationshipRepresentative(),
                createBusinessRecord.businessOwner().getRelationshipExecutive(),
                createBusinessRecord.businessOwner().getRelationshipDirector(),
                null,
                "Title",
                new Address(
                    new EncryptedString("13810 Shavano Wind"),
                    new EncryptedString("San Antonio, Texas(TX), 78230"),
                    "San Antonio",
                    "Texas",
                    new EncryptedString("78230"),
                    Country.USA),
                createBusinessRecord.businessOwner().getEmail().getEncrypted(),
                createBusinessRecord.businessOwner().getPhone().getEncrypted(),
                null,
                true));
    MockMultipartFile frontIdMultipartFile =
        new MockMultipartFile(
            "documentList",
            String.format(
                "%s|IDENTITY_DOCUMENT_FRONT|front.png",
                businessOwnerAndStripePersonRecord.businessOwner().getStripePersonReference()),
            "image/png",
            "front.png".getBytes());
    MockMultipartFile backIdMultipartFile =
        new MockMultipartFile(
            "documentList",
            String.format(
                "%s|IDENTITY_DOCUMENT_BACK|back.png",
                businessOwnerAndStripePersonRecord.businessOwner().getStripePersonReference()),
            "image/png",
            "back.png".getBytes());
    MockMultipartHttpServletRequestBuilder multipartRequest =
        MockMvcRequestBuilders.multipart("/application-review/document");

    MockHttpServletResponse response =
        mvc.perform(
                multipartRequest
                    .file(frontIdMultipartFile)
                    .file(backIdMultipartFile)
                    .cookie(createBusinessRecord.authCookie()))
            .andExpect(status().is2xxSuccessful())
            .andReturn()
            .getResponse();

    Assertions.assertEquals("Files successfully sent for review.", response.getContentAsString());
  }

  @SneakyThrows
  @Test
  void testUploadCompanyAdditionalDocuments() {
    TestHelper.CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    Business business = businessRepository.getById(createBusinessRecord.business().getId());
    business.getStripeData().setAccountRef("acct_StripeId");
    businessRepository.save(business);
    MockMultipartFile mockMultipartFile =
        new MockMultipartFile(
            "documentList",
            String.format(
                "%s|ACCOUNT_REQUIREMENT|documentName.png",
                business.getStripeData().getAccountRef()),
            "image/png",
            "doc.png".getBytes());
    MockMultipartHttpServletRequestBuilder multipartRequest =
        MockMvcRequestBuilders.multipart("/application-review/document");

    MockHttpServletResponse response =
        mvc.perform(
                multipartRequest.file(mockMultipartFile).cookie(createBusinessRecord.authCookie()))
            .andExpect(status().is2xxSuccessful())
            .andReturn()
            .getResponse();

    Assertions.assertEquals("Files successfully sent for review.", response.getContentAsString());
  }
}
