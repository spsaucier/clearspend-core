package com.tranwall.service;

import com.tranwall.data.model.Program;
import com.tranwall.data.repository.ProgramRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProgramService {

  @NonNull private final ProgramRepository programRepository;

  Program createProgram(String name, String bin) {
    return programRepository.save(new Program(name, bin));
  }
}
