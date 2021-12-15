package com.tranwall.capital.data.repository;

import com.tranwall.capital.common.typedid.data.BusinessId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.data.model.AccountActivity;
import com.tranwall.capital.data.repository.impl.AccountActivityRepositoryImpl.ChartData;
import com.tranwall.capital.service.AccountActivityFilterCriteria;
import com.tranwall.capital.service.type.ChartFilterCriteria;
import com.tranwall.capital.service.type.DashboardData;
import com.tranwall.capital.service.type.GraphFilterCriteria;
import java.util.List;
import org.springframework.data.domain.Page;

public interface AccountActivityRepositoryCustom {

  Page<AccountActivity> find(
      TypedId<BusinessId> businessId, AccountActivityFilterCriteria filterCriteria);

  DashboardData findDataForLineGraph(
      TypedId<BusinessId> businessId, GraphFilterCriteria filterCriteria);

  List<ChartData> findDataForChart(
      TypedId<BusinessId> businessId, ChartFilterCriteria filterCriteria);
}
