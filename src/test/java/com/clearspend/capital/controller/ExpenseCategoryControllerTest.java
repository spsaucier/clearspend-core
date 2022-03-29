package com.clearspend.capital.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.data.model.ExpenseCategory;
import com.clearspend.capital.data.model.enums.ExpenseCategoryStatus;
import com.clearspend.capital.data.repository.ExpenseCategoryRepository;
import com.clearspend.capital.service.ExpenseCategoryService;
import java.util.List;
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
  private final ExpenseCategoryService expenseCategoryService;
  private final ExpenseCategoryRepository expenseCategoryRepository;
  private TestHelper.CreateBusinessRecord createBusinessRecord;

  @BeforeEach
  void init() {
    createBusinessRecord = testHelper.init();
  }

  @Test
  @SneakyThrows
  void getCategories() {
    Cookie authCookie = createBusinessRecord.authCookie();
    MockHttpServletResponse response =
        mvc.perform(
                get("/expense-categories/list").contentType("application/json").cookie(authCookie))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
    log.info(response.getContentAsString());
  }

  @Test
  @SneakyThrows
  void disableCategories() {
    Cookie authCookie = createBusinessRecord.authCookie();
    List<ExpenseCategory> foundCategories =
        expenseCategoryService.retrieveExpenseCategoriesForBusiness(
            createBusinessRecord.business().getId());
    MockHttpServletResponse response =
        mvc.perform(
                post("/expense-categories/disable")
                    .contentType("application/json")
                    .content("[\"%s\"]".formatted(foundCategories.get(0).getId()))
                    .cookie(authCookie))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
    log.info(response.getContentAsString());
    assertThat(
            expenseCategoryRepository
                .findByBusinessIdAndStatus(
                    createBusinessRecord.business().getId(), ExpenseCategoryStatus.DISABLED)
                .size())
        .isEqualTo(1);
  }
}
