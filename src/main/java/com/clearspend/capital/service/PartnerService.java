package com.clearspend.capital.service;

import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.controller.type.partner.PartnerBusiness;
import com.clearspend.capital.data.model.PartnerUserDetails;
import com.clearspend.capital.data.model.security.UserAllocationRole;
import com.clearspend.capital.data.repository.AllocationRepository;
import com.clearspend.capital.data.repository.PartnerUserDetailsRepository;
import com.clearspend.capital.data.repository.security.UserAllocationRoleRepository;
import com.google.cloud.Tuple;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PartnerService {

  private final AccountService accountService;
  private final AllocationRepository allocationRepository;
  private final BusinessService businessService;
  private final PartnerUserDetailsRepository partnerUserDetailsRepository;
  private final UserAllocationRoleRepository userAllocationRoleRepository;

  @PreAuthorize("hasRootPermission('READ')")
  public List<PartnerBusiness> getAllPartneredBusinessesForUser(TypedId<UserId> userId) {
    return userAllocationRoleRepository.findAllByUserId(userId).stream()
        .map(UserAllocationRole::getAllocationId)
        .map(it -> Tuple.of(it, allocationRepository.findById(it)))
        .filter(it -> it.y().isPresent())
        .map(
            it ->
                Tuple.of(
                    it.x(),
                    businessService.retrieveBusinessForService(it.y().get().getBusinessId(), true)))
        .map(
            it ->
                Tuple.of(
                    it.y(),
                    accountService.retrieveAllocationAccount(
                        it.y().getId(), it.y().getCurrency(), it.x())))
        .collect(
            Collectors.groupingBy(
                (it) -> it.x(),
                Collectors.reducing(
                    BigDecimal.ZERO,
                    (it) -> it.y().getLedgerBalance().getAmount(),
                    BigDecimal::add)))
        .entrySet()
        .stream()
        .map(
            it ->
                PartnerBusiness.of(it.getKey())
                    .withLedgerBalance(
                        new com.clearspend.capital.controller.type.Amount(
                            it.getKey().getCurrency(), it.getValue())))
        .collect(Collectors.toList());
  }

  @PreAuthorize("hasRootPermission('READ')")
  public List<PartnerBusiness> getAllPinnedBusinessesForUser(TypedId<UserId> userId) {
    Optional<PartnerUserDetails> details = partnerUserDetailsRepository.findById(userId);
    if (details.isPresent()) {
      return getAllPartneredBusinessesForUser(userId).stream()
          .filter(it -> details.get().getPinnedBusinesses().contains(it.getBusinessId()))
          .collect(Collectors.toList());
    }
    return Collections.emptyList();
  }
}
