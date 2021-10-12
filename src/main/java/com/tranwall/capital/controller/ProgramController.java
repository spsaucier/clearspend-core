package com.tranwall.capital.controller;

import com.tranwall.capital.controller.type.program.Program;
import com.tranwall.capital.service.ProgramService;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/programs")
@RequiredArgsConstructor
public class ProgramController {

  private final ProgramService programService;

  @GetMapping("")
  private List<Program> getPrograms() {
    return programService.findAllPrograms().stream()
        .map(e -> new Program(e.getId(), e.getName(), e.getBin(), e.getFundingType()))
        .collect(Collectors.toList());
  }
}
