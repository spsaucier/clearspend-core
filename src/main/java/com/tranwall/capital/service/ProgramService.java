package com.tranwall.capital.service;

import com.tranwall.capital.common.error.RecordNotFoundException;
import com.tranwall.capital.common.error.RecordNotFoundException.Table;
import com.tranwall.capital.data.model.Program;
import com.tranwall.capital.data.model.enums.FundingType;
import com.tranwall.capital.data.repository.ProgramRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProgramService {

  private final ProgramRepository programRepository;

  Program createProgram(String name, String bin, FundingType fundingType) {
    return programRepository.save(new Program(name, bin, fundingType));
  }

  public List<Program> findAllPrograms() {
    return programRepository.findAll();
  }

  public Program retrieveProgram(UUID programId) {
    return programRepository
        .findById(programId)
        .orElseThrow(() -> new RecordNotFoundException(Table.PROGRAM, programId));
  }
}
