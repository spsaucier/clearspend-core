package com.clearspend.capital.service;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.TestHelper.CreateBusinessRecord;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessOwnerId;
import com.clearspend.capital.crypto.data.model.embedded.RequiredEncryptedString;
import com.clearspend.capital.crypto.data.model.embedded.RequiredEncryptedStringWithHash;
import com.clearspend.capital.data.model.business.BusinessProspect;
import com.clearspend.capital.data.model.business.TosAcceptance;
import com.clearspend.capital.data.model.security.DefaultRoles;
import com.clearspend.capital.data.repository.business.BusinessProspectRepository;
import com.clearspend.capital.testutils.permission.PermissionValidationHelper;
import java.time.OffsetDateTime;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.junit.function.ThrowingRunnable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@RequiredArgsConstructor(onConstructor = @__({@Autowired}))
public class BusinessProspectServiceTest extends BaseCapitalTest {
  private final TestHelper testHelper;
  private final PermissionValidationHelper permissionValidationHelper;
  private final BusinessProspectRepository businessProspectRepo;
  private final BusinessProspectService businessProspectService;

  private CreateBusinessRecord createBusinessRecord;

  @BeforeEach
  void setup() {
    createBusinessRecord = testHelper.createBusiness();
  }

  @Test
  void retrieveBusinessProspect_UserPermissions() {
    final TypedId<BusinessOwnerId> ownerId =
        new TypedId<>(createBusinessRecord.user().getUserId().toUuid());
    final BusinessProspect prospect = new BusinessProspect();
    prospect.setBusinessId(createBusinessRecord.business().getId());
    prospect.setBusinessOwnerId(ownerId);
    prospect.setFirstName(new RequiredEncryptedString("Bob"));
    prospect.setLastName(new RequiredEncryptedString("Saget"));
    prospect.setEmail(new RequiredEncryptedStringWithHash("user@gmail.com"));
    prospect.setTosAcceptance(new TosAcceptance(OffsetDateTime.now(), "", ""));
    businessProspectRepo.save(prospect);
    final ThrowingRunnable action = () -> businessProspectService.retrieveBusinessProspect(ownerId);
    permissionValidationHelper
        .buildValidator(createBusinessRecord)
        .allowUser(createBusinessRecord.user())
        .allowGlobalRoles(
            Set.of(
                DefaultRoles.GLOBAL_CUSTOMER_SERVICE, DefaultRoles.GLOBAL_CUSTOMER_SERVICE_MANAGER))
        .build()
        .validateServiceMethod(action);
  }
}
