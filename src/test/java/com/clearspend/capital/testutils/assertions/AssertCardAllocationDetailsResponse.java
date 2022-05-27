package com.clearspend.capital.testutils.assertions;

import static org.assertj.core.api.Assertions.assertThat;

import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.controller.type.card.CardAllocationSpendControls;
import com.clearspend.capital.controller.type.card.limits.CurrencyLimit;
import com.clearspend.capital.data.model.Allocation;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.data.model.enums.MccGroup;
import com.clearspend.capital.data.model.enums.PaymentType;
import com.clearspend.capital.data.repository.AllocationRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor(onConstructor = @__({@Autowired}))
public class AssertCardAllocationDetailsResponse {

  public record LimitsToAssert(
      List<CurrencyLimit> limits,
      Set<MccGroup> disabledMccGroups,
      Set<PaymentType> disabledPaymentTypes,
      Boolean disableForeign) {
    public static LimitsToAssert fromUpdateCardRequest(final CardAllocationSpendControls request) {
      return new LimitsToAssert(
          request.getLimits(),
          request.getDisabledMccGroups(),
          request.getDisabledPaymentTypes(),
          request.getDisableForeign());
    }
  }

  private final AllocationRepository allocationRepository;

  public void doAssert(
      final LimitsToAssert limitsToAssert,
      final TypedId<AllocationId> expectedAllocationId,
      final CardAllocationSpendControls cardAllocationDetails) {
    final Allocation expectedAllocation =
        allocationRepository.findById(expectedAllocationId).orElseThrow();
    assertThat(cardAllocationDetails)
        .hasFieldOrPropertyWithValue("allocationId", expectedAllocation.getAllocationId())
        .hasFieldOrPropertyWithValue("allocationName", expectedAllocation.getName())
        .hasFieldOrPropertyWithValue("limits", limitsToAssert.limits())
        .hasFieldOrPropertyWithValue("disabledMccGroups", limitsToAssert.disabledMccGroups())
        .hasFieldOrPropertyWithValue("disabledPaymentTypes", limitsToAssert.disabledPaymentTypes())
        .hasFieldOrPropertyWithValue("disableForeign", limitsToAssert.disableForeign());
  }

  public void doAssert(
      final TypedId<AllocationId> expectedAllocationId,
      final CardAllocationSpendControls cardAllocationDetails) {
    final LimitsToAssert limitsToAssert =
        new LimitsToAssert(
            List.of(new CurrencyLimit(Currency.USD, new HashMap<>())), Set.of(), Set.of(), false);
    doAssert(limitsToAssert, expectedAllocationId, cardAllocationDetails);
  }
}
