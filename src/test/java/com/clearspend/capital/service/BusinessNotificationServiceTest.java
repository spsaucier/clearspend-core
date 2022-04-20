package com.clearspend.capital.service;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.data.model.Allocation;
import com.clearspend.capital.data.model.BusinessNotification;
import com.clearspend.capital.data.model.User;
import com.clearspend.capital.data.model.business.Business;
import com.clearspend.capital.data.model.enums.BusinessNotificationType;
import com.clearspend.capital.data.model.notifications.BusinessNotificationData;
import com.clearspend.capital.data.repository.BusinessNotificationRepository;
import com.github.javafaker.Faker;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class BusinessNotificationServiceTest extends BaseCapitalTest {
  @Autowired private TestHelper testHelper;
  @Autowired private BusinessNotificationRepository businessNotificationRepository;
  @Autowired private BusinessNotificationService businessNotificationService;
  private TestHelper.CreateBusinessRecord createBusinessRecord;
  private Faker faker = new Faker();
  private Allocation allocation;
  private Business business;
  private User user;

  @BeforeEach
  public void setup() {
    createBusinessRecord = testHelper.createBusiness();
    business = createBusinessRecord.business();
    business.setCodatCompanyRef("test-codat-ref");
    allocation = createBusinessRecord.allocationRecord().allocation();
    user = createBusinessRecord.user();
  }

  private BusinessNotification buildBusinessNotification(
      BusinessNotificationType businessNotificationType, String oldValue, String newValue) {
    BusinessNotification businessNotification = new BusinessNotification();
    businessNotification.setBusinessId(business.getBusinessId());
    businessNotification.setType(businessNotificationType);
    BusinessNotificationData businessNotificationData = new BusinessNotificationData();
    businessNotificationData.setOldValue(oldValue);
    businessNotificationData.setNewValue(newValue);
    businessNotification.setData(businessNotificationData);
    return businessNotification;
  }

  @Test
  public void canGetRecentNotifications() {
    businessNotificationRepository.save(
        buildBusinessNotification(
            BusinessNotificationType.CHART_OF_ACCOUNTS_CREATED, null, "New Category"));
    BusinessNotification oldNotification =
        buildBusinessNotification(
            BusinessNotificationType.CHART_OF_ACCOUNTS_CREATED, null, "New Category");
    OffsetDateTime oldCreationDate = OffsetDateTime.now(ZoneOffset.UTC);
    oldCreationDate = oldCreationDate.minus(5, ChronoUnit.DAYS);
    oldNotification.setCreated(oldCreationDate);
    businessNotificationRepository.save(oldNotification);

    assertThat(
            businessNotificationService
                .getRecentChartOfAccountsNotifications(business.getId())
                .size())
        .isEqualTo(1);
  }
}
