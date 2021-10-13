package com.tranwall.capital.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tranwall.capital.BaseCapitalTest;
import com.tranwall.capital.controller.ControllerHelper.OnboardBusinessRecord;
import com.tranwall.capital.controller.type.business.owner.UpdateBusinessOwnerRequest;
import com.tranwall.capital.data.model.BusinessProspect;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

@RequiredArgsConstructor(onConstructor = @__({@Autowired}))
class BusinessOwnerControllerTest extends BaseCapitalTest {

  private final MockMvc mvc;
  private final ControllerHelper controllerHelper;

  OnboardBusinessRecord onboardBusinessRecord;

  @BeforeEach
  void init() throws Exception {
    if (onboardBusinessRecord == null) {
      onboardBusinessRecord = controllerHelper.onboardBusiness();
    }
  }

  @SneakyThrows
  @Test
  void updateBusinessOwner_success() {
    BusinessProspect businessProspect = onboardBusinessRecord.businessProspect();
    UpdateBusinessOwnerRequest request =
        new UpdateBusinessOwnerRequest(
            businessProspect.getFirstName().getEncrypted(),
            businessProspect.getLastName().getEncrypted(),
            controllerHelper.generateDateOfBirth(),
            controllerHelper.generateTaxIdentificationNumber(),
            businessProspect.getEmail().getEncrypted(),
            controllerHelper.generateAddress());

    String body = objectMapper.writeValueAsString(request);

    MockHttpServletResponse response =
        mvc.perform(
                patch(
                        String.format(
                            "/business-owners/%s", onboardBusinessRecord.businessOwner().getId()))
                    .contentType("application/json")
                    .content(body))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
  }
}
