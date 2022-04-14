package com.clearspend.capital.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.client.codat.types.CodatAccount;
import com.clearspend.capital.client.codat.types.CodatAccountNested;
import com.clearspend.capital.client.codat.types.CodatAccountStatus;
import com.clearspend.capital.client.codat.types.CodatAccountType;
import com.clearspend.capital.data.model.Allocation;
import com.clearspend.capital.data.model.Card;
import com.clearspend.capital.data.model.ChartOfAccounts;
import com.clearspend.capital.data.model.User;
import com.clearspend.capital.data.model.business.Business;
import com.clearspend.capital.data.repository.BusinessNotificationRepository;
import com.clearspend.capital.data.repository.ChartOfAccountsRepository;
import com.github.javafaker.Faker;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class ChartOfAccountsServiceTest extends BaseCapitalTest {
  @Autowired private ChartOfAccountsRepository chartOfAccountsRepository;
  @Autowired private TestHelper testHelper;
  @Autowired private ChartOfAccountsService chartOfAccountsService;
  @Autowired private CodatService codatService;
  @Autowired private BusinessNotificationRepository businessNotificationRepository;
  @Autowired private BusinessNotificationService businessNotificationService;

  private TestHelper.CreateBusinessRecord createBusinessRecord;
  private Faker faker = new Faker();
  private Allocation allocation;
  private Business business;
  private Card card;
  private User user;
  private Cookie userCookie;

  @BeforeEach
  public void setup() {
    if (createBusinessRecord == null) {
      createBusinessRecord = testHelper.createBusiness();
      business = createBusinessRecord.business();
      business.setCodatCompanyRef("test-codat-ref");
      allocation = createBusinessRecord.allocationRecord().allocation();
      user = createBusinessRecord.user();
      userCookie = testHelper.login(user);
      testHelper.setCurrentUser(user);
    }
  }

  @Test
  public void canUpdateChartOfAccounts() {
    List<CodatAccount> accounts =
        CodatServiceTest.getQualifiedNames().stream()
            .map(
                walker ->
                    CodatServiceTest.CodatAccountBuilder.builder()
                        .withId(UUID.randomUUID().toString())
                        .withName(faker.name().firstName())
                        .withStatus(CodatAccountStatus.ACTIVE)
                        .withCategory("Testing")
                        .withQualifiedName(walker)
                        .withType(CodatAccountType.EXPENSE)
                        .build())
            .collect(Collectors.toList());

    List<CodatAccountNested> result = codatService.nestCodatAccounts(accounts);

    chartOfAccountsService.updateChartOfAccountsForBusiness(business.getId(), result);

    ChartOfAccounts chartOfAccounts =
        chartOfAccountsService.getChartOfAccountsForBusiness(business.getId());
    assertThat(chartOfAccounts.getNestedAccounts()).isNotNull();
    assertThat(chartOfAccounts.getNestedAccounts().size()).isEqualTo(30);
  }

  @Test
  public void canGenerateChartOfAccountsDiff() {
    businessNotificationService.acceptChartOfAccountChangesForUser(
        business.getId(), user.getUserId());
    List<CodatAccount> accounts =
        CodatServiceTest.getQualifiedNames().stream()
            .map(
                walker ->
                    CodatServiceTest.CodatAccountBuilder.builder()
                        .withId(UUID.randomUUID().toString())
                        .withName(faker.name().firstName())
                        .withStatus(CodatAccountStatus.ACTIVE)
                        .withCategory("Testing")
                        .withQualifiedName(walker)
                        .withType(CodatAccountType.EXPENSE)
                        .build())
            .collect(Collectors.toList());

    List<CodatAccountNested> oldNestedAccount = codatService.nestCodatAccounts(accounts);

    List<CodatAccount> newAccounts =
        CodatServiceTest.getModifiedQualifiedNames().stream()
            .map(
                walker ->
                    CodatServiceTest.CodatAccountBuilder.builder()
                        .withId(UUID.randomUUID().toString())
                        .withName(faker.name().firstName())
                        .withStatus(CodatAccountStatus.ACTIVE)
                        .withCategory("Testing")
                        .withQualifiedName(walker)
                        .withType(CodatAccountType.EXPENSE)
                        .build())
            .collect(Collectors.toList());

    List<CodatAccountNested> newNestedAccount = codatService.nestCodatAccounts(newAccounts);

    chartOfAccountsService.updateStatusesForChartOfAccounts(
        new ChartOfAccounts(business.getId(), oldNestedAccount),
        new ChartOfAccounts(business.getId(), newNestedAccount),
        business.getId());
    assertThat(newNestedAccount.size()).isGreaterThan(0);
    chartOfAccountsService.updateChartOfAccountsForBusiness(business.getId(), newNestedAccount);
    assertThat(chartOfAccountsService.getTotalChangesForBusiness(business.getId())).isEqualTo(6);
    assertThat(businessNotificationRepository.findAllByBusinessId(business.getId()).size())
        .isEqualTo(7);
    assertThat(
            businessNotificationService
                .getUnseenNotificationsForUser(business.getId(), user.getUserId())
                .size())
        .isEqualTo(6);
    businessNotificationService.acceptChartOfAccountChangesForUser(
        business.getId(), user.getUserId());
    assertThat(
            businessNotificationService
                .getUnseenNotificationsForUser(business.getId(), user.getUserId())
                .size())
        .isEqualTo(0);
  }
}
