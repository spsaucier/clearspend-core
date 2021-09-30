package com.tranwall.capital.service;

import com.tranwall.capital.data.model.Bin;
import com.tranwall.capital.data.repository.BinRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class BinServiceTest {

  @Autowired private BinService binService;

  @Autowired private BinRepository binRepository;

  @Test
  void createBin() {
    Bin bin = binService.createBin("");
    Bin foundBin = binRepository.findById(bin.getId()).orElseThrow();
  }
}
