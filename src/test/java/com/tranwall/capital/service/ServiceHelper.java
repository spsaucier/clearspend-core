package com.tranwall.capital.service;

import com.github.javafaker.Faker;
import com.tranwall.capital.data.model.Bin;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
//@RequiredArgsConstructor(onConstructor = @__({@Autowired}))
@RequiredArgsConstructor
@Slf4j
public class ServiceHelper {

  @NonNull private BinService binService;

  @NonNull private Faker faker;

  public Bin createBin() {
    return binService.createBin(faker.random().nextInt(500000, 599999).toString());
  }
}
