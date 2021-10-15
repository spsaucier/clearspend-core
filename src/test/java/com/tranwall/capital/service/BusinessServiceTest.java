package com.tranwall.capital.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.tranwall.capital.BaseCapitalTest;
import com.tranwall.capital.TestHelper;
import com.tranwall.capital.data.model.Bin;
import com.tranwall.capital.data.model.Business;
import com.tranwall.capital.data.model.Program;
import com.tranwall.capital.data.repository.BusinessRepository;
import com.tranwall.capital.service.BusinessService.BusinessAndAllocationsRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class BusinessServiceTest extends BaseCapitalTest {

  @Autowired private TestHelper testHelper;

  @Autowired private BusinessRepository businessRepository;

  private Bin bin;
  private Program program;

  @BeforeEach
  public void setup() {
    if (bin == null) {
      bin = testHelper.createBin();
      program = testHelper.createProgram(bin);
    }
  }

  @Test
  void createBusiness() {
    BusinessAndAllocationsRecord businessAndAllocationsRecord = testHelper.createBusiness(program);
    Business foundBusiness =
        businessRepository.findById(businessAndAllocationsRecord.business().getId()).orElseThrow();
    assertThat(foundBusiness).isNotNull();
  }
}
