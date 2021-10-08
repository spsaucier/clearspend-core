package com.tranwall.capital.service;

import com.tranwall.capital.BaseCapitalTest;
import com.tranwall.capital.data.model.Bin;
import com.tranwall.capital.data.repository.BinRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class BinServiceTest extends BaseCapitalTest {

  @Autowired private BinService binService;

  @Autowired private ServiceHelper serviceHelper;

  @Autowired private BinRepository binRepository;

  @Test
  void createBin() {
    Bin bin = serviceHelper.createBin();
    Bin foundBin = binRepository.findById(bin.getId()).orElseThrow();
    assertThat(foundBin).isNotNull();
    assertThat(foundBin.getBin()).isEqualTo(bin.getBin());
  }
}
