package com.clearspend.capital.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.TestHelper.CreateBusinessRecord;
import com.clearspend.capital.data.model.Bin;
import com.clearspend.capital.data.model.Business;
import com.clearspend.capital.data.model.Program;
import com.clearspend.capital.data.repository.BusinessRepository;
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
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    Business foundBusiness =
        businessRepository.findById(createBusinessRecord.business().getId()).orElseThrow();
    assertThat(foundBusiness).isNotNull();
  }
}
