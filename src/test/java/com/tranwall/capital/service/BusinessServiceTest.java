package com.tranwall.capital.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.tranwall.capital.CapitalTest;
import com.tranwall.capital.data.model.Bin;
import com.tranwall.capital.data.model.Business;
import com.tranwall.capital.data.model.Program;
import com.tranwall.capital.data.repository.BusinessRepository;
import com.tranwall.capital.service.BusinessService.BusinessRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@CapitalTest
class BusinessServiceTest {

  @Autowired private ServiceHelper serviceHelper;

  @Autowired private BusinessRepository businessRepository;

  private Bin bin;
  private Program program;

  @BeforeEach
  public void setup() {
    if (bin == null) {
      bin = serviceHelper.createBin();
      program = serviceHelper.createProgram(bin);
    }
  }

  @Test
  void createBusiness() {
    BusinessRecord businessRecord = serviceHelper.createBusiness(program);
    Business foundBusiness =
        businessRepository.findById(businessRecord.business().getId()).orElseThrow();
    assertThat(foundBusiness).isNotNull();
  }
}
