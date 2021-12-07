package com.tranwall.capital.controller;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.dockerjava.api.command.CreateImageResponse;
import com.tranwall.capital.BaseCapitalTest;
import com.tranwall.capital.TestHelper;
import com.tranwall.capital.data.model.Business;
import com.tranwall.capital.service.UserService.CreateUpdateUserRecord;
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
    //    MockMultipartFile currency = new MockMultipartFile("currency", "USD".getBytes());
    //    MockMultipartFile amount = new MockMultipartFile("amount", "100.00".getBytes());

    MockHttpServletResponse response =
        mvc.perform(
                MockMvcRequestBuilders.multipart("/images/receipts")
                    .file(image)
                    //                    .file(currency)
                    //                    .file(amount)
                    .cookie(userCookie))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
    CreateImageResponse createImageResponse =
        objectMapper.readValue(response.getContentAsString(), CreateImageResponse.class);
    log.info(
        "\n{}",
        objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(createImageResponse));
  }
}
