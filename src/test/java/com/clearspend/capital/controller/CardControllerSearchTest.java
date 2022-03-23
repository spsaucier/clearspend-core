package com.clearspend.capital.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.TestHelper.CreateBusinessRecord;
import com.clearspend.capital.controller.type.PagedData;
import com.clearspend.capital.controller.type.card.SearchCardData;
import com.clearspend.capital.controller.type.card.SearchCardRequest;
import com.clearspend.capital.controller.type.common.PageRequest;
import com.clearspend.capital.data.model.Card;
import com.clearspend.capital.data.model.business.Business;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.data.model.enums.FundingType;
import com.clearspend.capital.data.model.enums.card.CardStatus;
import com.clearspend.capital.data.model.enums.card.CardType;
import com.clearspend.capital.data.model.security.DefaultRoles;
import com.clearspend.capital.data.repository.CardRepositoryCustom.FilteredCardRecord;
import com.clearspend.capital.data.repository.HoldRepository;
import com.clearspend.capital.service.AccountService;
import com.clearspend.capital.service.AllocationService.AllocationRecord;
import com.clearspend.capital.service.UserService.CreateUpdateUserRecord;
import com.clearspend.capital.service.type.PageToken.OrderBy;
import com.fasterxml.jackson.core.type.TypeReference;
import java.math.BigDecimal;
import java.util.List;
import javax.servlet.http.Cookie;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

@RequiredArgsConstructor(onConstructor = @__({@Autowired}))
@Slf4j
public class CardControllerSearchTest extends BaseCapitalTest {

  private final MockMvc mvc;
  private final TestHelper testHelper;
  private final HoldRepository holdRepository;
  private final AccountService accountService;

  private CreateBusinessRecord createBusinessRecord;
  private Business business;

  private Cookie authCookie;

  private AllocationRecord rootAllocation;
  private AllocationRecord childAllocation;

  private CreateUpdateUserRecord userA;
  private CreateUpdateUserRecord userB;
  private CreateUpdateUserRecord userC;

  private Card rootCardA;
  private Card rootCardB;
  private Card childCardA;

  private SearchCardData rootCardASearchResult;
  private SearchCardData rootCardBSearchResult;
  private SearchCardData childCardASearchResult;

  @SneakyThrows
  @BeforeEach
  public void setup() {
    if (createBusinessRecord == null) {
      createBusinessRecord = testHelper.createBusiness(1000L);
      business = createBusinessRecord.business();
      rootAllocation = createBusinessRecord.allocationRecord();
      testHelper.setCurrentUser(createBusinessRecord.user());
      childAllocation =
          testHelper.createAllocation(
              business.getId(),
              "Child Allocation",
              rootAllocation.allocation().getId(),
              createBusinessRecord.user());
      userA =
          testHelper.createUserWithRole(
              createBusinessRecord.allocationRecord().allocation(),
              DefaultRoles.ALLOCATION_EMPLOYEE);
      userB =
          testHelper.createUserWithRole(
              createBusinessRecord.allocationRecord().allocation(),
              DefaultRoles.ALLOCATION_EMPLOYEE);
      userC =
          testHelper.createUserWithRole(
              childAllocation.allocation(), DefaultRoles.ALLOCATION_MANAGER);
      authCookie = createBusinessRecord.authCookie();

      // root card A
      rootCardA =
          testHelper.issueCard(
              business,
              rootAllocation.allocation(),
              userA.user(),
              Currency.USD,
              FundingType.POOLED,
              CardType.VIRTUAL,
              false);
      rootCardASearchResult =
          SearchCardData.of(
              new FilteredCardRecord(
                  rootCardA, rootAllocation.allocation(), rootAllocation.account(), userA.user()));
      rootCardASearchResult.setBalance(
          new com.clearspend.capital.controller.type.Amount(
              Currency.USD, new BigDecimal("1000.00")));

      // root card B
      rootCardB =
          testHelper.issueCard(
              business,
              rootAllocation.allocation(),
              userB.user(),
              Currency.USD,
              FundingType.POOLED,
              CardType.VIRTUAL,
              false);
      rootCardBSearchResult =
          SearchCardData.of(
              new FilteredCardRecord(
                  rootCardB, rootAllocation.allocation(), rootAllocation.account(), userB.user()));
      rootCardBSearchResult.setBalance(
          new com.clearspend.capital.controller.type.Amount(
              Currency.USD, new BigDecimal("1000.00")));

      // child card A
      childCardA =
          testHelper.issueCard(
              business,
              childAllocation.allocation(),
              userA.user(),
              Currency.USD,
              FundingType.POOLED,
              CardType.PHYSICAL,
              false);
      childCardASearchResult =
          SearchCardData.of(
              new FilteredCardRecord(
                  childCardA,
                  childAllocation.allocation(),
                  childAllocation.account(),
                  userA.user()));

      // placing hold to make sure that available balance is returned for root cards A and B
      // THIS DOES NOT WORK!!! The balance does not get updated by the time I need to run a query
      //      accountService.depositFunds(
      //          business.getId(), rootAllocation.account(), Amount.of(Currency.USD, 777), true,
      // true);

    }
  }

  @SneakyThrows
  @Test
  void searchAllCards() {
    SearchCardRequest request = new SearchCardRequest(new PageRequest(0, 10));

    PagedData<SearchCardData> result = callSearchCards(request, authCookie, 3);

    assertThat(result.getContent())
        .containsExactlyInAnyOrder(
            rootCardASearchResult, rootCardBSearchResult, childCardASearchResult);
  }

  @SneakyThrows
  @Test
  void orderByCreatedDesc() {
    PageRequest pageRequest = new PageRequest(0, 10);
    pageRequest.setOrderBy(
        List.of(OrderBy.builder().item("created").direction(Direction.DESC).build()));
    SearchCardRequest request = new SearchCardRequest(pageRequest);

    PagedData<SearchCardData> result =
        callSearchCards(request, testHelper.login(createBusinessRecord.user()), 3);

    assertThat(result.getContent())
        .containsExactlyInAnyOrder(
            childCardASearchResult, rootCardBSearchResult, rootCardASearchResult);
  }

  @SneakyThrows
  @Test
  void searchByAllocationId() {
    SearchCardRequest request = new SearchCardRequest(new PageRequest(0, 10));
    request.addAllocation(childAllocation.allocation().getId());

    PagedData<SearchCardData> result = callSearchCards(request, authCookie, 1);

    assertThat(result.getContent()).containsExactlyInAnyOrder(childCardASearchResult);
  }

  @SneakyThrows
  @Test
  void searchByUserId() {
    SearchCardRequest request = new SearchCardRequest(new PageRequest(0, 20));
    request.addUser(userA.user().getId());

    PagedData<SearchCardData> result = callSearchCards(request, authCookie, 2);

    assertThat(result.getContent())
        .containsExactlyInAnyOrder(rootCardASearchResult, childCardASearchResult);
  }

  @SneakyThrows
  @Test
  void searchByLast4CardNumber() {
    SearchCardRequest request = new SearchCardRequest(new PageRequest(0, 10));
    request.setSearchText(childCardA.getLastFour());

    PagedData<SearchCardData> result = callSearchCards(request, authCookie, 1);

    assertThat(result.getContent()).containsExactlyInAnyOrder(childCardASearchResult);
  }

  @SneakyThrows
  @Test
  void searchByAllocationName() {
    String name = rootAllocation.allocation().getName();
    SearchCardRequest request = new SearchCardRequest(new PageRequest(0, 10));
    request.setSearchText(name);

    PagedData<SearchCardData> result = callSearchCards(request, authCookie, 2);

    assertThat(result.getContent())
        .containsExactlyInAnyOrder(rootCardASearchResult, rootCardBSearchResult);
  }

  @SneakyThrows
  @Test
  void searchByUserLastName() {
    SearchCardRequest request = new SearchCardRequest(new PageRequest(0, 10));
    request.setSearchText(userB.user().getLastName().getEncrypted());

    PagedData<SearchCardData> result = callSearchCards(request, authCookie, 1);

    assertThat(result.getContent()).containsExactlyInAnyOrder(rootCardBSearchResult);
  }

  @SneakyThrows
  @Test
  void searchTotalElementsShouldBeCalculated() {
    SearchCardRequest request = new SearchCardRequest(new PageRequest(0, 1));

    PagedData<SearchCardData> result = callSearchCards(request, authCookie, 1);

    assertThat(result.getTotalElements()).isEqualTo(3);
  }

  @SneakyThrows
  @Test
  void search_cardTypeFiltersAreHonoredIfSupplied() {
    SearchCardRequest request = new SearchCardRequest(new PageRequest(0, 10));
    request.setTypes(List.of(CardType.VIRTUAL));

    PagedData<SearchCardData> result = callSearchCards(request, authCookie, 2);

    assertThat(result.getContent())
        .containsExactlyInAnyOrder(rootCardBSearchResult, rootCardASearchResult);
  }

  @SneakyThrows
  @Test
  void search_cardStatusFiltersAreHonoredIfSupplied() {
    SearchCardRequest request = new SearchCardRequest(new PageRequest(0, 10));
    request.setStatuses(List.of(CardStatus.INACTIVE));

    PagedData<SearchCardData> result = callSearchCards(request, authCookie, 1);

    assertThat(result.getContent()).containsExactlyInAnyOrder(childCardASearchResult);
  }

  @SneakyThrows
  @Test
  void search_cardManagerPermissionAppliesDownstream() {
    SearchCardRequest request = new SearchCardRequest(new PageRequest(0, 10));

    PagedData<SearchCardData> result = callSearchCards(request, testHelper.login(userC.user()), 1);

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
  void search_cardOwnershipPermissionsIsTransitive() {
    SearchCardRequest request = new SearchCardRequest(new PageRequest(0, 10));

    PagedData<SearchCardData> result = callSearchCards(request, testHelper.login(userA.user()), 2);

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

  private PagedData<SearchCardData> callSearchCards(
      SearchCardRequest request, Cookie authCookie, long expectedSize) throws Exception {
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
