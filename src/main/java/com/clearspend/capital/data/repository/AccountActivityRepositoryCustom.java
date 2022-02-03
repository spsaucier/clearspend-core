package com.clearspend.capital.data.repository;

import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.CardId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.data.model.AccountActivity;
import com.clearspend.capital.service.AccountActivityFilterCriteria;
import com.clearspend.capital.service.type.CardAllocationSpendingDaily;
import com.clearspend.capital.service.type.CardStatementData;
import com.clearspend.capital.service.type.CardStatementFilterCriteria;
import com.clearspend.capital.service.type.ChartData;
import com.clearspend.capital.service.type.ChartFilterCriteria;
import com.clearspend.capital.service.type.DashboardData;
import com.clearspend.capital.service.type.GraphFilterCriteria;
import org.springframework.data.domain.Page;

public interface AccountActivityRepositoryCustom {

  Page<AccountActivity> find(
      TypedId<BusinessId> businessId, AccountActivityFilterCriteria filterCriteria);

  DashboardData findDataForLineGraph(
      TypedId<BusinessId> businessId, GraphFilterCriteria filterCriteria);

  ChartData findDataForChart(TypedId<BusinessId> businessId, ChartFilterCriteria filterCriteria);

  CardStatementData findDataForCardStatement(
      TypedId<BusinessId> businessId, CardStatementFilterCriteria filterCriteria);

  CardAllocationSpendingDaily findCardAllocationSpendingDaily(
      TypedId<BusinessId> businessId,
      TypedId<AllocationId> allocationId,
      TypedId<CardId> cardId,
      int daysAgo);
}
