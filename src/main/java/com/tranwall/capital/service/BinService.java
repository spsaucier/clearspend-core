package com.tranwall.capital.service;

import com.tranwall.capital.common.error.RecordNotFoundException;
import com.tranwall.capital.common.error.RecordNotFoundException.Table;
import com.tranwall.capital.common.typedid.data.BinId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.data.model.Bin;
import com.tranwall.capital.data.repository.BinRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class BinService {

  private final BinRepository binRepository;

  public Bin createBin(String bin, String name) {
    return binRepository.save(new Bin(bin, name));
  }

  public Bin retrieveBin(String bin) {
    return binRepository
        .findByBin(bin)
        .orElseThrow(() -> new RecordNotFoundException(Table.BIN, bin));
  }

  public List<Bin> findAllBins() {
    return binRepository.findAll();
  }

  public Bin retrieveBin(TypedId<BinId> binId) {
    return binRepository
        .findById(binId)
        .orElseThrow(() -> new RecordNotFoundException(Table.BIN, binId));
  }
}
