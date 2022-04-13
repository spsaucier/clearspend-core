package com.clearspend.capital.data.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.data.model.Allocation;
import com.clearspend.capital.data.model.BusinessNotification;
import com.clearspend.capital.data.model.Card;
import com.clearspend.capital.data.model.User;
import com.clearspend.capital.data.model.business.Business;
import com.clearspend.capital.data.model.enums.BusinessNotificationType;
import com.clearspend.capital.data.model.notifications.BusinessNotificationData;
import com.github.javafaker.Faker;
import javax.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class BusinessNotificationRepositoryTest extends BaseCapitalTest {
  @Autowired private BusinessNotificationRepository businessNotificationRepository;
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
  public void canSaveBusinessNotification() {
    BusinessNotification businessNotification = new BusinessNotification();
    businessNotification.setBusinessId(business.getBusinessId());
    businessNotification.setType(BusinessNotificationType.CHART_OF_ACCOUNTS_CREATED);
    BusinessNotificationData businessNotificationData = new BusinessNotificationData();
    businessNotificationData.setOldValue("oldName");
    businessNotificationData.setNewValue("newName");
    businessNotification.setData(businessNotificationData);
    businessNotificationRepository.save(businessNotification);

    assertThat(businessNotificationRepository.findAll().size()).isEqualTo(1);
  }
}
