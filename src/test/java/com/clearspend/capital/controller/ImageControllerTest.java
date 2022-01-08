package com.clearspend.capital.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.controller.type.receipt.CreateReceiptResponse;
import com.clearspend.capital.data.model.business.Business;
import com.clearspend.capital.service.UserService.CreateUpdateUserRecord;
import javax.servlet.http.Cookie;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@RequiredArgsConstructor(onConstructor = @__({@Autowired}))
@Slf4j
class ImageControllerTest extends BaseCapitalTest {

  private final MockMvc mvc;
  private final TestHelper testHelper;

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
  void storeReceiptImage() {
    MockMultipartFile image =
        new MockMultipartFile(
            "receipt", "hello.txt", MediaType.TEXT_PLAIN_VALUE, "Hello, World!".getBytes());

    MockHttpServletResponse response =
        mvc.perform(
                MockMvcRequestBuilders.multipart("/images/receipts").file(image).cookie(userCookie))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
    CreateReceiptResponse createImageResponse =
        objectMapper.readValue(response.getContentAsString(), CreateReceiptResponse.class);
    log.info(
        "\n{}",
        objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(createImageResponse));

    // retrieve the image
    response =
        mvc.perform(
                get("/images/receipts/{receiptId}", createImageResponse.getReceiptId())
                    .cookie(userCookie))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

    log.info("response: {}", response.getContentAsString());
  }
}
