package com.clearspend.capital.service;

import com.clearspend.capital.service.type.PageToken;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TransactionSyncLogFilterCriteria {
  private PageToken pageToken;
}
