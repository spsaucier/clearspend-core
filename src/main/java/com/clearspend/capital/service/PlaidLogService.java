package com.clearspend.capital.service;

import com.clearspend.capital.common.error.RecordNotFoundException;
import com.clearspend.capital.common.error.Table;
import com.clearspend.capital.common.typedid.data.PlaidLogEntryId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.controller.type.PagedData;
import com.clearspend.capital.controller.type.plaid.PlaidLogEntryDetails;
import com.clearspend.capital.controller.type.plaid.PlaidLogEntryMetadata;
import com.clearspend.capital.controller.type.plaid.PlaidLogEntryRequest;
import com.clearspend.capital.data.model.PlaidLogEntry;
import com.clearspend.capital.data.repository.PlaidLogEntryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PlaidLogService {
  private final PlaidLogEntryRepository plaidLogEntryRepo;
  private final ObjectMapper objectMapper;

  @PreAuthorize("hasGlobalPermission('CUSTOMER_SERVICE_MANAGER')")
  public PagedData<PlaidLogEntryMetadata> getLogsForBusiness(
      @NonNull final TypedId<BusinessId> businessId, @NonNull final PlaidLogEntryRequest request) {
    final Pageable pageable = request.getPageable();
    return PagedData.of(
        plaidLogEntryRepo.findByBusinessIdOrderByCreatedAsc(businessId, pageable),
        PlaidLogEntryMetadata::fromPlaidLogEntry);
  }

  @PreAuthorize("hasGlobalPermission('CUSTOMER_SERVICE_MANAGER')")
  public <T> PlaidLogEntryDetails<T> getLogDetails(
      @NonNull final TypedId<BusinessId> businessId,
      @NonNull final TypedId<PlaidLogEntryId> plaidLogEntryId) {
    final PlaidLogEntry plaidLogEntry =
        plaidLogEntryRepo
            .findByBusinessIdAndId(businessId, plaidLogEntryId)
            .orElseThrow(
                () ->
                    new RecordNotFoundException(
                        Table.PLAID_LOG_ENTRY, businessId, plaidLogEntryId));
    return PlaidLogEntryDetails.fromPlaidLogEntry(objectMapper, plaidLogEntry);
  }
}
