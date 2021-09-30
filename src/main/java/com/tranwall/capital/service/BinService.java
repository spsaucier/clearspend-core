package com.tranwall.capital.service;

import com.tranwall.capital.data.model.Bin;
import com.tranwall.capital.data.repository.BinRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class BinService {

  private final BinRepository binRepository;

  public Bin createBin(String bin) {
    return binRepository.save(new Bin(bin));
  }
}
