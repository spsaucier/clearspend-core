package com.clearspend.capital.service.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.MockMvcHelper;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.client.google.MockBigTableClient;
import com.clearspend.capital.data.audit.AuditLogDisplayValue;
import com.clearspend.capital.data.model.Allocation;
import com.clearspend.capital.data.model.Card;
import com.clearspend.capital.data.model.User;
import com.clearspend.capital.data.model.business.Business;
import com.clearspend.capital.data.model.enums.FundingType;
import com.clearspend.capital.data.model.enums.card.CardType;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.List;
import javax.persistence.EntityManager;
import javax.servlet.http.Cookie;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.MockMvc;

@Slf4j
public class AccountingAuditLogEndPointTest extends BaseCapitalTest {

  @Autowired private TestHelper testHelper;
  private TestHelper.CreateBusinessRecord createBusinessRecord;

  @Autowired MockMvc mvc;
  @Autowired MockMvcHelper mockMvcHelper;

  private Allocation allocation;
  private Business business;
  private Card card;
  private User user;
  private Cookie userCookie;

  @Autowired EntityManager entityManager;
  @Autowired CacheManager cacheManager;
  @Autowired MockBigTableClient bigTableClient;

  @BeforeEach
  public void setup() {
    createBusinessRecord = testHelper.createBusiness();
    business = createBusinessRecord.business();
    allocation = createBusinessRecord.allocationRecord().allocation();
    user = createBusinessRecord.user();
    userCookie = testHelper.login(user);
    testHelper.setCurrentUser(user);
    card =
        testHelper.issueCard(
            business,
            allocation,
            user,
            business.getCurrency(),
            FundingType.POOLED,
            CardType.VIRTUAL,
            false);
    entityManager.flush();
    cacheManager.getCacheNames().stream()
        .map(it -> cacheManager.getCache(it))
        .forEach(it -> it.clear());
  }

  @AfterEach
  void cleanUp() {
    bigTableClient.getMockBigTable().clear();
  }

  @Test
  @SneakyThrows
  void testSearchAllAccountingAuditLogByBusiness() {
    testHelper.setCurrentUser(createBusinessRecord.user());
    bigTableClient.setTestUserId(createBusinessRecord.user().getId().toString());
    bigTableClient.prepareEmptyAuditData(createBusinessRecord.business().getId().toString());

    List<AuditLogDisplayValue> actual =
        mockMvcHelper.queryList(
            "/codat/audit-log?limit=1", HttpMethod.GET, userCookie, null, new TypeReference<>() {});

    assertThat(actual.isEmpty()).isTrue();

    bigTableClient.prepareFullAuditData(createBusinessRecord.business().getId().toString());

    List<AuditLogDisplayValue> fullResult =
        mockMvcHelper.queryList(
            "/codat/audit-log?limit=1", HttpMethod.GET, userCookie, null, new TypeReference<>() {});

    assertThat(fullResult.size() == 5).isTrue();
  }
}
