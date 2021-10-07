package com.tranwall.capital.service;

import com.tranwall.capital.BaseCapitalTest;
import com.tranwall.capital.data.model.Bin;
import com.tranwall.capital.data.model.Program;
import com.tranwall.capital.data.model.User;
import com.tranwall.capital.data.repository.UserRepository;
import com.tranwall.capital.service.BusinessService.BusinessRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class UserServiceTest extends BaseCapitalTest {

  @Autowired private ServiceHelper serviceHelper;

  @Autowired private UserRepository userRepository;

  private BusinessRecord businessRecord;
  private Bin bin;
  private Program program;

  @BeforeEach
  public void setup() {
    if (bin == null) {
      bin = serviceHelper.createBin();
      program = serviceHelper.createProgram(bin);
      businessRecord = serviceHelper.createBusiness(program);
    }
  }

  @Test
  void createUser() {
    User user = serviceHelper.createUser(businessRecord.business());
    User foundUser = userRepository.findById(user.getId()).orElseThrow();
    assertThat(foundUser).isNotNull();
  }
}
