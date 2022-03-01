package com.clearspend.capital.data.repository.impl;

import com.blazebit.persistence.CriteriaBuilder;
import com.blazebit.persistence.CriteriaBuilderFactory;
import com.blazebit.persistence.PagedList;
import com.blazebit.persistence.PaginatedCriteriaBuilder;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.data.model.TransactionSyncLog;
import com.clearspend.capital.data.repository.TransactionSyncLogRepositoryCustom;
import com.clearspend.capital.service.TransactionSyncLogFilterCriteria;
import com.clearspend.capital.service.type.PageToken;
import java.util.ArrayList;
import javax.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
@RequiredArgsConstructor
public class TransactionSyncLogRepositoryImpl implements TransactionSyncLogRepositoryCustom {
  private final CriteriaBuilderFactory criteriaBuilderFactory;
  private final EntityManager entityManager;

  @Override
  public Page<TransactionSyncLog> find(
      TypedId<BusinessId> businessIdTypedId, TransactionSyncLogFilterCriteria criteria) {
    // query
    CriteriaBuilder<TransactionSyncLog> select =
        criteriaBuilderFactory
            .create(entityManager, TransactionSyncLog.class, "transactionSyncLog")
            .select("transactionSyncLog");

    select.orderByDesc("transactionSyncLog.created");
    select.orderByDesc("transactionSyncLog.id");
    PageToken pageToken = criteria.getPageToken();
    int maxResults = pageToken.getPageSize();
    int firstResult = pageToken.getPageNumber() * maxResults;
    PaginatedCriteriaBuilder<TransactionSyncLog> page = select.page(firstResult, maxResults);
    PagedList<TransactionSyncLog> paged = page.getResultList();

    return new PageImpl<>(
        new ArrayList<>(paged),
        PageRequest.of(
            criteria.getPageToken().getPageNumber(), criteria.getPageToken().getPageSize()),
        paged.getTotalSize());
  }
}
