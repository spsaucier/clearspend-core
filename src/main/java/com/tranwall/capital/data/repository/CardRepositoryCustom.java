package com.tranwall.capital.data.repository;

import com.tranwall.capital.data.model.Account;
import com.tranwall.capital.data.model.Allocation;
import com.tranwall.capital.data.model.Card;
import com.tranwall.capital.data.model.User;
import com.tranwall.capital.service.CardFilterCriteria;
import org.springframework.data.domain.Page;

public interface CardRepositoryCustom {

  record FilteredCardRecord(Card card, Allocation allocation, Account account, User user) {}

  Page<FilteredCardRecord> find(CardFilterCriteria filterCriteria);
}
