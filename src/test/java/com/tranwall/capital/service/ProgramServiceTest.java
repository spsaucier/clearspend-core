package com.tranwall.capital.service;

import com.tranwall.capital.CapitalTest;
import com.tranwall.capital.data.model.Bin;
import com.tranwall.capital.data.model.enums.FundingType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@CapitalTest
class ProgramServiceTest {

  @Autowired private ProgramService programService;
  @Autowired private ServiceHelper serviceHelper;

  private Bin bin;

  @BeforeEach
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
