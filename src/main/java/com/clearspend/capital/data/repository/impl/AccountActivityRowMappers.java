package com.clearspend.capital.data.repository.impl;

import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.data.model.enums.MerchantType;
import com.clearspend.capital.data.model.enums.UserType;
import com.clearspend.capital.service.type.AllocationChartData;
import com.clearspend.capital.service.type.MerchantCategoryChartData;
import com.clearspend.capital.service.type.MerchantChartData;
import com.clearspend.capital.service.type.UserChartData;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.jdbc.core.RowMapper;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AccountActivityRowMappers {
  public static final RowMapper<MerchantCategoryChartData> MERCHANT_CATEGORY_CHART_MAPPER =
      (resultSet, rowNum) ->
          new MerchantCategoryChartData(
              MerchantType.valueOf(resultSet.getString(2)),
              new Amount(Currency.valueOf(resultSet.getString(4)), resultSet.getBigDecimal(3)));
  public static final RowMapper<MerchantChartData> MERCHANT_CHART_MAPPER =
      (resultSet, rowNum) ->
          new MerchantChartData(
              new Amount(Currency.valueOf(resultSet.getString(7)), resultSet.getBigDecimal(6)),
              resultSet.getInt(4),
              MerchantType.valueOf(resultSet.getString(2)),
              resultSet.getString(1),
              resultSet.getString(3),
              resultSet.getString(5));
  public static final RowMapper<UserChartData> USER_CHART_MAPPER =
      (resultSet, rowNum) ->
          new UserChartData(
              new TypedId<>(resultSet.getObject(1, UUID.class)),
              UserType.valueOf(resultSet.getString(2)),
              resultSet.getString(3),
              resultSet.getString(4),
              new Amount(Currency.valueOf(resultSet.getString(6)), resultSet.getBigDecimal(5)));
  public static final RowMapper<AllocationChartData> ALLOCATION_CHART_MAPPER =
      (resultSet, rowNum) ->
          new AllocationChartData(
              new TypedId<>(resultSet.getObject(1, UUID.class)),
              resultSet.getString(2),
              new Amount(Currency.valueOf(resultSet.getString(4)), resultSet.getBigDecimal(3)));
}
