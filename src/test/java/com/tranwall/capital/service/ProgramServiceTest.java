package com.tranwall.capital.service;

import com.tranwall.capital.CapitalTest;
import com.tranwall.capital.data.model.Bin;
import com.tranwall.capital.data.model.Program;
import com.tranwall.capital.data.model.enums.FundingType;
import com.tranwall.capital.data.repository.ProgramRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@CapitalTest
class ProgramServiceTest {

  @Autowired private ProgramService programService;
  @Autowired private ServiceHelper serviceHelper;

  @Autowired private ProgramRepository programRepository;

  private Bin bin;

  @BeforeEach
  public void setup() {
    if (bin == null) {
      bin = serviceHelper.createBin();
    }
  }

  @Test
  void createProgram() {
    Program program = programService.createProgram("", bin.getBin(), FundingType.POOLED);
    Program foundProgram = programRepository.findById(program.getId()).orElseThrow();
  }
}
