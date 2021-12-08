package com.tranwall.capital.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.tranwall.capital.BaseCapitalTest;
import com.tranwall.capital.TestHelper;
import com.tranwall.capital.data.model.Bin;
import com.tranwall.capital.data.model.Program;
import com.tranwall.capital.data.model.enums.CardType;
import com.tranwall.capital.data.model.enums.FundingType;
import com.tranwall.capital.data.repository.ProgramRepository;
import javax.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@Transactional
class ProgramServiceTest extends BaseCapitalTest {

  @Autowired private ProgramService programService;
  @Autowired private TestHelper testHelper;

  @Autowired private ProgramRepository programRepository;

  private Bin bin;

  @BeforeEach
  public void setup() {
    if (bin == null) {
      bin = testHelper.retrieveBin();
    }
    if (bin == null) {
      bin = testHelper.createBin();
    }
  }

  @Test
  void createProgram() {
    Program program =
        programService.createProgram(
            "CreateProgramTest",
            testHelper.createBin().getBin(),
            FundingType.POOLED,
            CardType.VIRTUAL,
            "CPT");
    Program foundProgram = programRepository.findById(program.getId()).orElseThrow();
    assertThat(foundProgram).isNotNull();
    assertThat(foundProgram.getBin()).isEqualTo(program.getBin());
    assertThat(foundProgram.getName()).isEqualTo(program.getName());
  }
}
