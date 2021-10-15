package com.tranwall.capital.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.tranwall.capital.BaseCapitalTest;
import com.tranwall.capital.TestHelper;
import com.tranwall.capital.data.model.Bin;
import com.tranwall.capital.data.model.Program;
import com.tranwall.capital.data.model.User;
import com.tranwall.capital.data.repository.UserRepository;
import com.tranwall.capital.service.BusinessService.BusinessAndAllocationsRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class UserServiceTest extends BaseCapitalTest {

  @Autowired private TestHelper testHelper;

  @Autowired private UserRepository userRepository;

  private BusinessAndAllocationsRecord businessAndAllocationsRecord;
  private Bin bin;
  private Program program;

  @BeforeEach
  public void setup() {
    if (bin == null) {
      bin = testHelper.createBin();
      program = testHelper.createProgram(bin);
      businessAndAllocationsRecord = testHelper.createBusiness(program);
    }
  }

  @Test
  void createUser() {
    User user = testHelper.createUser(businessAndAllocationsRecord.business());
    User foundUser = userRepository.findById(user.getId()).orElseThrow();
    assertThat(foundUser).isNotNull();
  }
}
