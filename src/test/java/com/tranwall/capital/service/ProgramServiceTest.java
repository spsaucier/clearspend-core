package com.tranwall.capital.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.tranwall.capital.BaseCapitalTest;
import com.tranwall.capital.TestHelper;
import com.tranwall.capital.data.model.Bin;
import com.tranwall.capital.data.model.Program;
import com.tranwall.capital.data.repository.ProgramRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ProgramServiceTest extends BaseCapitalTest {

  @Autowired private ProgramService programService;
  @Autowired private TestHelper testHelper;

  @Autowired private ProgramRepository programRepository;

  private Bin bin;

  @BeforeEach
  public void setup() {
    if (bin == null) {
      bin = testHelper.createBin();
    }
  }

  @Test
  void createProgram() {
    Program program = testHelper.createProgram(bin);
    Program foundProgram = programRepository.findById(program.getId()).orElseThrow();
    assertThat(foundProgram).isNotNull();
    assertThat(foundProgram.getBin()).isEqualTo(program.getBin());
    assertThat(foundProgram.getName()).isEqualTo(program.getName());
  }
}
