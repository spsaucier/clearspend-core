package com.tranwall.capital.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
import com.tranwall.capital.BaseCapitalTest;
import com.tranwall.capital.TestHelper;
import com.tranwall.capital.TestHelper.CreateBusinessRecord;
import com.tranwall.capital.controller.type.PagedData;
import com.tranwall.capital.controller.type.card.SearchCardData;
import com.tranwall.capital.controller.type.card.SearchCardRequest;
import com.tranwall.capital.controller.type.common.PageRequest;
import com.tranwall.capital.data.model.Bin;
import com.tranwall.capital.data.model.Business;
import com.tranwall.capital.data.model.Card;
import com.tranwall.capital.data.model.Program;
import com.tranwall.capital.data.model.enums.Currency;
import com.tranwall.capital.data.repository.CardRepositoryCustom.FilteredCardRecord;
import com.tranwall.capital.service.AllocationService.AllocationRecord;
import com.tranwall.capital.service.UserService.CreateUpdateUserRecord;
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
public class CardControllerSearchTest extends BaseCapitalTest {

  private final MockMvc mvc;
  private final TestHelper testHelper;

  private CreateBusinessRecord createBusinessRecord;
  private Bin bin;
  private Business business;
  private Program program;

  private Cookie authCookie;

  private AllocationRecord rootAllocation;
  private AllocationRecord childAllocation;

  private CreateUpdateUserRecord userA;
  private CreateUpdateUserRecord userB;

  private Card rootCardA;
  private Card rootCardB;
  private Card childCardA;

  @SneakyThrows
  @BeforeEach
  public void setup() {
    if (bin == null) {
      bin = testHelper.createBin();
      program = testHelper.createProgram(bin);
      createBusinessRecord = testHelper.createBusiness();
      business = createBusinessRecord.business();
      rootAllocation = createBusinessRecord.allocationRecord();
      childAllocation =
          testHelper.createAllocation(
              business.getId(), "Child Allocation", rootAllocation.allocation().getId());
      userA = testHelper.createUser(createBusinessRecord.business());
      userB = testHelper.createUser(createBusinessRecord.business());
      authCookie = createBusinessRecord.authCookie();

      rootCardA =
          testHelper.issueCard(
              business, rootAllocation.allocation(), userA.user(), program, Currency.USD);
      rootCardB =
          testHelper.issueCard(
              business, rootAllocation.allocation(), userB.user(), program, Currency.USD);
      childCardA =
          testHelper.issueCard(
              business, childAllocation.allocation(), userA.user(), program, Currency.USD);
    }
  }

  @SneakyThrows
  @Test
  void searchAllCards() {
    SearchCardRequest request = new SearchCardRequest(new PageRequest(0, 10));

    PagedData<SearchCardData> result = callSearcCards(request, 3);

    assertThat(result.getContent())
        .containsExactlyInAnyOrder(
            SearchCardData.of(
                new FilteredCardRecord(
                    rootCardA,
                    rootAllocation.allocation(),
                    rootAllocation.account(),
                    userA.user())),
            SearchCardData.of(
                new FilteredCardRecord(
                    rootCardB,
                    rootAllocation.allocation(),
                    rootAllocation.account(),
                    userB.user())),
            SearchCardData.of(
                new FilteredCardRecord(
                    childCardA,
                    childAllocation.allocation(),
                    childAllocation.account(),
                    userA.user())));
  }

  @SneakyThrows
  @Test
  void searchByAllocationId() {
    SearchCardRequest request = new SearchCardRequest(new PageRequest(0, 10));
    request.setAllocationId(childAllocation.allocation().getId());

    PagedData<SearchCardData> result = callSearcCards(request, 1);

    assertThat(result.getContent())
        .containsExactlyInAnyOrder(
            SearchCardData.of(
                new FilteredCardRecord(
                    childCardA,
                    childAllocation.allocation(),
                    childAllocation.account(),
                    userA.user())));
  }

  @SneakyThrows
  @Test
  void searchByUserId() {
    SearchCardRequest request = new SearchCardRequest(new PageRequest(0, 20));
    request.setUserId(userA.user().getId());

    PagedData<SearchCardData> result = callSearcCards(request, 2);

    assertThat(result.getContent())
        .containsExactlyInAnyOrder(
            SearchCardData.of(
                new FilteredCardRecord(
                    rootCardA,
                    rootAllocation.allocation(),
                    rootAllocation.account(),
                    userA.user())),
            SearchCardData.of(
                new FilteredCardRecord(
                    childCardA,
                    childAllocation.allocation(),
                    childAllocation.account(),
                    userA.user())));
  }

  @SneakyThrows
  @Test
  void searchByCardNumber() {
    SearchCardRequest request = new SearchCardRequest(new PageRequest(0, 10));
    request.setSearchText(childCardA.getCardNumber().getEncrypted());

    PagedData<SearchCardData> result = callSearcCards(request, 1);

    assertThat(result.getContent())
        .containsExactlyInAnyOrder(
            SearchCardData.of(
                new FilteredCardRecord(
                    childCardA,
                    childAllocation.allocation(),
                    childAllocation.account(),
                    userA.user())));
  }

  @SneakyThrows
  @Test
  void searchByAllocationName() {
    SearchCardRequest request = new SearchCardRequest(new PageRequest(0, 10));
    request.setSearchText("ROOT");

    PagedData<SearchCardData> result = callSearcCards(request, 2);

    assertThat(result.getContent())
        .containsExactlyInAnyOrder(
            SearchCardData.of(
                new FilteredCardRecord(
                    rootCardA,
                    rootAllocation.allocation(),
                    rootAllocation.account(),
                    userA.user())),
            SearchCardData.of(
                new FilteredCardRecord(
                    rootCardB,
                    rootAllocation.allocation(),
                    rootAllocation.account(),
                    userB.user())));
  }

  @SneakyThrows
  @Test
  void searchByUserLastName() {
    SearchCardRequest request = new SearchCardRequest(new PageRequest(0, 10));
    request.setSearchText(userB.user().getLastName().getEncrypted());

    PagedData<SearchCardData> result = callSearcCards(request, 1);

    assertThat(result.getContent())
        .containsExactlyInAnyOrder(
            SearchCardData.of(
                new FilteredCardRecord(
                    rootCardB,
                    rootAllocation.allocation(),
                    rootAllocation.account(),
                    userB.user())));
  }

  @SneakyThrows
  @Test
  void searchTotalElementsShouldBeCalculated() {
    SearchCardRequest request = new SearchCardRequest(new PageRequest(0, 1));

    PagedData<SearchCardData> result = callSearcCards(request, 1);

    assertThat(result.getTotalElements()).isEqualTo(3);
  }

  private PagedData<SearchCardData> callSearcCards(SearchCardRequest request, long expectedSize)
      throws Exception {
    String body = objectMapper.writeValueAsString(request);

    MockHttpServletResponse response =
        mvc.perform(
                post("/cards/search")
                    .contentType("application/json")
                    .content(body)
                    .cookie(authCookie))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

    PagedData<SearchCardData> result =
        objectMapper.readValue(response.getContentAsString(), new TypeReference<>() {});

    assertThat(result.getPageNumber()).isEqualTo(request.getPageRequest().getPageNumber());
    assertThat(result.getPageSize()).isEqualTo(request.getPageRequest().getPageSize());
    assertThat(result.getContent().size()).isEqualTo(expectedSize);

    return result;
  }
}
