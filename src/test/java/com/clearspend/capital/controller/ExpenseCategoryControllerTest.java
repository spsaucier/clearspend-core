package com.clearspend.capital.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.controller.type.expense.ExpenseCategoryRequest;
import com.clearspend.capital.data.model.business.Business;
import com.clearspend.capital.data.repository.ExpenseCategoryRepository;
import com.clearspend.capital.service.UserService;
import javax.servlet.http.Cookie;
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
class ExpenseCategoryControllerTest extends BaseCapitalTest {

  private final TestHelper testHelper;
  private final MockMvc mvc;
  private Cookie userCookie;
  private final ExpenseCategoryRepository expenseCategoryRepository;

  @BeforeEach
  @SneakyThrows
  void init() {
    testHelper.init();

    if (userCookie == null) {
      Business business = testHelper.retrieveBusiness();
      UserService.CreateUpdateUserRecord user = testHelper.createUser(business);
      userCookie = testHelper.login(user.user().getEmail().getEncrypted(), user.password());
    }
  }

  @Test
  @SneakyThrows
  void getCategories() {
    MockHttpServletResponse response =
        mvc.perform(
                get("/expense-categories/list").contentType("application/json").cookie(userCookie))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
    log.info(response.getContentAsString());
  }

  @Test
  @SneakyThrows
  void updateExpenseCategory_success() {

    ExpenseCategoryRequest expenseCategoryRequest = new ExpenseCategoryRequest();
    expenseCategoryRequest.setExpenseCategoryName("TEST");

    String body = objectMapper.writeValueAsString(expenseCategoryRequest);
    Integer expenseCategoryId = 5;
    mvc.perform(
            patch("/expense-categories/" + expenseCategoryId)
                .contentType("application/json")
                .content(body)
                .cookie(userCookie))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse();

    log.info(String.valueOf(expenseCategoryRepository.findByIconRef(expenseCategoryId)));
  }
}
