package com.clearspend.capital.data.repository;

import com.clearspend.capital.BaseCapitalTest;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class AccountActivityRepositoryTest extends BaseCapitalTest
{

  @Autowired
  private AccountActivityRepository accountActivityRepository;
  
  @Test
  void findByBusinessIdAndReceiptId() {
    accountActivityRepository.findByBusinessIdAndReceiptId(UUID.randomUUID(), UUID.randomUUID());
  }
}