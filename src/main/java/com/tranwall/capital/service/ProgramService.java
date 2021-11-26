package com.tranwall.capital.service;

import com.tranwall.capital.common.error.RecordNotFoundException;
import com.tranwall.capital.common.error.RecordNotFoundException.Table;
import com.tranwall.capital.common.typedid.data.ProgramId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.data.model.Program;
import com.tranwall.capital.data.model.enums.CardType;
import com.tranwall.capital.data.model.enums.FundingType;
import com.tranwall.capital.data.repository.ProgramRepository;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProgramService {

  private final ProgramRepository programRepository;

  public Program createProgram(
      String name, String bin, FundingType fundingType, CardType cardType, String i2cProgramRef) {
    final Program program = new Program(name, bin, fundingType, cardType, i2cProgramRef);
    return programRepository.save(program);
  }

  public List<Program> findAllPrograms() {
    return programRepository.findAll();
  }

  public List<Program> findProgramsByIds(Set<TypedId<ProgramId>> programIds) {
    return programRepository.findProgramsByIdIn(programIds);
  }

  public Program retrieveProgram(TypedId<ProgramId> programId) {
    return programRepository
        .findById(programId)
        .orElseThrow(() -> new RecordNotFoundException(Table.PROGRAM, programId));
  }
}
