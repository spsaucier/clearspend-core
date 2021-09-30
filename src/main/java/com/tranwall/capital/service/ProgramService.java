package com.tranwall.capital.service;

import com.tranwall.capital.data.model.Program;
import com.tranwall.capital.data.model.enums.FundingType;
import com.tranwall.capital.data.repository.ProgramRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProgramService {

  @NonNull private final ProgramRepository programRepository;

  Program createProgram(String name, String bin, FundingType fundingType) {
    return programRepository.save(new Program(name, bin, fundingType));
  }
}
