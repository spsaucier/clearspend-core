package com.tranwall.capital.controller;

import com.tranwall.capital.common.typedid.data.ProgramId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.controller.type.program.CreateProgramRequest;
import com.tranwall.capital.controller.type.program.CreateProgramResponse;
import com.tranwall.capital.controller.type.program.Program;
import com.tranwall.capital.service.ProgramService;
import io.swagger.annotations.ApiParam;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/programs")
@RequiredArgsConstructor
public class ProgramController {

  private final ProgramService programService;

  @PostMapping("")
  private CreateProgramResponse createProgram(
      @RequestBody @Validated CreateProgramRequest request) {
    return new CreateProgramResponse(
        programService
            .createProgram(request.getName(), request.getBin(), request.getFundingType())
            .getId());
  }

  @GetMapping("")
  private List<Program> getPrograms() {
    return programService.findAllPrograms().stream().map(Program::new).collect(Collectors.toList());
  }

  @GetMapping("/{programId}")
  private Program getProgram(
      @PathVariable(value = "programId")
          @ApiParam(
              required = true,
              name = "programId",
              value = "ID of the program record.",
              example = "48104ecb-1343-4cc1-b6f2-e6cc88e9a80f")
          TypedId<ProgramId> programId) {
    return new Program(programService.retrieveProgram(programId));
  }
}
