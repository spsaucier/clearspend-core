package com.clearspend.capital.service;

import com.clearspend.capital.common.error.RecordNotFoundException;
import com.clearspend.capital.common.error.Table;
import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.CardId;
import com.clearspend.capital.common.typedid.data.MccGroupId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.data.model.TransactionLimit;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.data.model.enums.LimitPeriod;
import com.clearspend.capital.data.model.enums.LimitType;
import com.clearspend.capital.data.model.enums.TransactionChannel;
import com.clearspend.capital.data.model.enums.TransactionLimitType;
import com.clearspend.capital.data.repository.TransactionLimitRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionLimitService {

  private final TransactionLimitRepository transactionLimitRepository;

  @Transactional
  public TransactionLimit initializeAllocationSpendLimit(
      TypedId<BusinessId> businessId, TypedId<AllocationId> allocationId) {

    return transactionLimitRepository.save(
        new TransactionLimit(
            businessId,
            TransactionLimitType.ALLOCATION,
            allocationId.toUuid(),
            Map.of(Currency.USD, new HashMap<>()),
            new ArrayList<>(),
            new HashSet<>()));
  }

  public TransactionLimit retrieveSpendLimit(
      TypedId<BusinessId> businessId, TransactionLimitType type, UUID ownerId) {
    return transactionLimitRepository
        .findByBusinessIdAndTypeAndOwnerId(businessId, type, ownerId)
        .orElseThrow(
            () -> new RecordNotFoundException(Table.SPEND_LIMIT, businessId, type, ownerId));
  }

  private TransactionLimit duplicateSpendLimit(
      TypedId<BusinessId> businessId, UUID ownerId, TransactionLimit existingTransactionLimit) {
    return transactionLimitRepository.save(
        new TransactionLimit(
            businessId,
            TransactionLimitType.CARD,
            ownerId,
            existingTransactionLimit.getLimits(),
            existingTransactionLimit.getDisabledMccGroups(),
            existingTransactionLimit.getDisabledTransactionChannels()));
  }

  @Transactional
  public TransactionLimit createAllocationSpendLimit(
      TypedId<BusinessId> businessId,
      TypedId<AllocationId> allocationId,
      Map<Currency, Map<LimitType, Map<LimitPeriod, BigDecimal>>> transactionLimits,
      List<TypedId<MccGroupId>> disabledMccGroups,
      Set<TransactionChannel> disabledTransactionChannels) {

    return transactionLimitRepository.save(
        new TransactionLimit(
            businessId,
            TransactionLimitType.ALLOCATION,
            allocationId.toUuid(),
            transactionLimits,
            disabledMccGroups,
            disabledTransactionChannels));
  }

  @Transactional
  public TransactionLimit createCardSpendLimit(
      TypedId<BusinessId> businessId,
      TypedId<CardId> cardId,
      Map<Currency, Map<LimitType, Map<LimitPeriod, BigDecimal>>> transactionLimits,
      List<TypedId<MccGroupId>> disabledMccGroups,
      Set<TransactionChannel> disabledTransactionChannels) {

    return transactionLimitRepository.save(
        new TransactionLimit(
            businessId,
            TransactionLimitType.CARD,
            cardId.toUuid(),
            transactionLimits,
            disabledMccGroups,
            disabledTransactionChannels));
  }

  @Transactional
  public TransactionLimit updateAllocationSpendLimit(
      TypedId<BusinessId> businessId,
      TypedId<AllocationId> allocationId,
      Map<Currency, Map<LimitType, Map<LimitPeriod, BigDecimal>>> transactionLimits,
      List<TypedId<MccGroupId>> disabledMccGroups,
      Set<TransactionChannel> disabledTransactionChannels) {

    return updateSpendLimit(
        businessId,
        TransactionLimitType.ALLOCATION,
        allocationId.toUuid(),
        transactionLimits,
        disabledMccGroups,
        disabledTransactionChannels);
  }

  @Transactional
  public TransactionLimit updateCardSpendLimit(
      TypedId<BusinessId> businessId,
      TypedId<CardId> cardId,
      Map<Currency, Map<LimitType, Map<LimitPeriod, BigDecimal>>> transactionLimits,
      List<TypedId<MccGroupId>> disabledMccGroups,
      Set<TransactionChannel> disabledTransactionChannels) {

    return updateSpendLimit(
        businessId,
        TransactionLimitType.CARD,
        cardId.toUuid(),
        transactionLimits,
        disabledMccGroups,
        disabledTransactionChannels);
  }

  private TransactionLimit updateSpendLimit(
      TypedId<BusinessId> businessId,
      TransactionLimitType limitType,
      UUID ownerId,
      Map<Currency, Map<LimitType, Map<LimitPeriod, BigDecimal>>> transactionLimits,
      List<TypedId<MccGroupId>> disabledMccGroups,
      Set<TransactionChannel> disabledTransactionChannels) {

    TransactionLimit transactionLimit = retrieveSpendLimit(businessId, limitType, ownerId);

    BeanUtils.setNotNull(transactionLimits, transactionLimit::setLimits);
    BeanUtils.setNotNull(disabledMccGroups, transactionLimit::setDisabledMccGroups);
    BeanUtils.setNotNull(
        disabledTransactionChannels, transactionLimit::setDisabledTransactionChannels);

    return transactionLimitRepository.save(transactionLimit);
  }
}
