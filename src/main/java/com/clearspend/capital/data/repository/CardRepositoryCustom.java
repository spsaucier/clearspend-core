package com.clearspend.capital.data.repository;

import com.clearspend.capital.common.typedid.data.CardId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.data.model.Account;
import com.clearspend.capital.data.model.Allocation;
import com.clearspend.capital.data.model.Card;
import com.clearspend.capital.data.model.TransactionLimit;
import com.clearspend.capital.data.model.User;
import com.clearspend.capital.service.CardFilterCriteria;
import java.util.List;
import java.util.Optional;
import javax.validation.constraints.NotNull;
import lombok.NonNull;
import org.springframework.data.domain.Page;

public interface CardRepositoryCustom {

  record CardDetailsRecord(
      Card card, Allocation allocation, Account account, TransactionLimit transactionLimit) {}

  record FilteredCardRecord(Card card, Allocation allocation, Account account, User user) {}

  Page<FilteredCardRecord> filter(CardFilterCriteria filterCriteria);

  Optional<CardDetailsRecord> findDetailsById(@NotNull TypedId<CardId> cardId);

  Optional<CardDetailsRecord> findDetailsByBusinessIdAndId(
      @NotNull TypedId<BusinessId> businessId, @NotNull TypedId<CardId> cardId);

  List<CardDetailsRecord> findDetailsByBusinessIdAndUserId(
      @NotNull TypedId<BusinessId> businessId, @NotNull TypedId<UserId> userId);

  Optional<CardDetailsRecord> findDetailsByBusinessIdAndUserIdAndId(
      @NotNull TypedId<BusinessId> businessId,
      @NonNull TypedId<CardId> cardId,
      @NotNull TypedId<UserId> userId);
}
