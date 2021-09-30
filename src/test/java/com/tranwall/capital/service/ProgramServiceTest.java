package com.tranwall.capital.service;

import com.tranwall.capital.data.model.Bin;
import com.tranwall.capital.data.model.enums.FundingType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.event.annotation.BeforeTestClass;

@SpringBootTest
class ProgramServiceTest {

  @Autowired private ProgramService programService;
  @Autowired private ServiceHelper serviceHelper;

  private Bin bin;

  @BeforeTestClass
  public void setup() {
    if (bin == null) {
      bin = serviceHelper.createBin();
    }
  }

  @Test
  void createProgram() {
    programService.createProgram("", bin.getBin(), FundingType.POOLED);
  }
}
