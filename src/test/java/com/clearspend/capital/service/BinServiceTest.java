package com.clearspend.capital.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.data.model.Bin;
import com.clearspend.capital.data.repository.BinRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class BinServiceTest extends BaseCapitalTest {

  @Autowired private BinService binService;

  @Autowired private TestHelper testHelper;

  @Autowired private BinRepository binRepository;

  @Test
  void createBin() {
    Bin bin = testHelper.createBin();
    Bin foundBin = binRepository.findById(bin.getId()).orElseThrow();
    assertThat(foundBin).isNotNull();
    assertThat(foundBin.getBin()).isEqualTo(bin.getBin());
  }
}
