package com.tranwall.service;

import com.tranwall.data.model.Bin;
import com.tranwall.data.repository.BinRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class BinService {

  @NonNull private final BinRepository binRepository;

  Bin createBin(String bin) {
    return binRepository.save(new Bin(bin));
  }
}
