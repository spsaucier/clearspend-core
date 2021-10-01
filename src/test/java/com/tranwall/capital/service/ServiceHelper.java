package com.tranwall.capital.service;

import com.github.javafaker.Faker;
import com.tranwall.capital.data.model.Bin;
import com.tranwall.capital.data.model.Program;
import com.tranwall.capital.data.model.enums.FundingType;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ServiceHelper {

  private final BinService binService;
  private final ProgramService programService;

  private final Faker faker = new Faker();

  public Bin createBin() {
    return binService.createBin(faker.random().nextInt(500000, 599999).toString());
  }

  public Program createProgram(Bin bin) {
    return programService.createProgram(
        UUID.randomUUID().toString(), bin.getBin(), FundingType.POOLED);
  }
}
