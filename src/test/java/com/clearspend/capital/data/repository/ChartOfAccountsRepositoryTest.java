package com.clearspend.capital.data.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.client.codat.types.CodatAccountNested;
import com.clearspend.capital.data.model.Allocation;
import com.clearspend.capital.data.model.Card;
import com.clearspend.capital.data.model.ChartOfAccounts;
import com.clearspend.capital.data.model.User;
import com.clearspend.capital.data.model.business.Business;
import com.github.javafaker.Faker;
import java.util.List;
import javax.servlet.http.Cookie;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
public class ChartOfAccountsRepositoryTest extends BaseCapitalTest {
  @Autowired private ChartOfAccountsRepository chartOfAccountsRepository;
  @Autowired private TestHelper testHelper;

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
  public void canPersistNestedAccounts() {
    CodatAccountNested chartOfAccounts = new CodatAccountNested("1", "Parent");
    CodatAccountNested childAccount = new CodatAccountNested("2", "Child");
    childAccount.setCategory("Expense");
    childAccount.setStatus("Active");
    childAccount.setQualifiedName("Parent.Child");
    chartOfAccounts.setChildren(List.of(childAccount));

    chartOfAccountsRepository.save(new ChartOfAccounts(business.getId(), List.of(chartOfAccounts)));
    List<ChartOfAccounts> results = chartOfAccountsRepository.findAll();
    assertThat(results.size()).isEqualTo(1);
    assertThat(results.get(0).getNestedAccounts().get(0).getChildren().size()).isEqualTo(1);
    assertThat(results.get(0).getNestedAccounts().get(0).getChildren().get(0).getName())
        .isEqualTo("Child");
  }
}
