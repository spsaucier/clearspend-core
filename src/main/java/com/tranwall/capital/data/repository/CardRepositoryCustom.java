package com.tranwall.capital.data.repository;

import com.tranwall.capital.common.typedid.data.BusinessId;
import com.tranwall.capital.common.typedid.data.CardId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.common.typedid.data.UserId;
import com.tranwall.capital.data.model.Account;
import com.tranwall.capital.data.model.Allocation;
import com.tranwall.capital.data.model.Card;
import com.tranwall.capital.data.model.TransactionLimit;
import com.tranwall.capital.data.model.User;
import com.tranwall.capital.service.CardFilterCriteria;
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

  Optional<CardDetailsRecord> findDetailsByBusinessIdAndId(
      @NotNull TypedId<BusinessId> businessId, @NotNull TypedId<CardId> cardId);

  List<CardDetailsRecord> findDetailsByBusinessIdAndUserId(
      @NotNull TypedId<BusinessId> businessId, @NotNull TypedId<UserId> userId);

  Optional<CardDetailsRecord> findDetailsByBusinessIdAndUserIdAndId(
      @NotNull TypedId<BusinessId> businessId,
      @NonNull TypedId<CardId> cardId,
      @NotNull TypedId<UserId> userId);
}
