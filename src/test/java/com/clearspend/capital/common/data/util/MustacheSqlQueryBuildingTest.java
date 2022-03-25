package com.clearspend.capital.common.data.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.TestHelper.CreateBusinessRecord;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Template;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

@RequiredArgsConstructor(onConstructor = @__({@Autowired}))
public class MustacheSqlQueryBuildingTest extends BaseCapitalTest {
  private static final String QUERY_TEMPLATE =
      """
            SELECT *
            FROM business
            WHERE type = :type
            {{#name}}AND business_name = :name{{/name}}
            """;
  private static final String FULL_QUERY =
      """
            SELECT *
            FROM business
            WHERE type = :type
            AND business_name = :name
            """;

  private final TestHelper testHelper;
  private final DataSource dataSource;
  private CreateBusinessRecord createBusinessRecord;
  private NamedParameterJdbcTemplate jdbcTemplate;

  @BeforeEach
  void setup() {
    createBusinessRecord = testHelper.createBusiness();
    jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
  }

  @Test
  void mustacheContextAndSqlParamsCombined() {
    final RequestPojo request =
        new RequestPojo(
            createBusinessRecord.business().getType().name(),
            createBusinessRecord.business().getBusinessName());
    final BeanPropertySqlParameterSource source = new BeanPropertySqlParameterSource(request);
    final MustacheSqlBeanPropertyContext context = new MustacheSqlBeanPropertyContext(source);
    final Template template = Mustache.compiler().compile(QUERY_TEMPLATE);
    final String query = template.execute(context);
    assertEquals(FULL_QUERY, query);

    final List<BusinessResult> results =
        jdbcTemplate.query(
            query,
            source,
            (resultSet, rowNum) ->
                new BusinessResult(new TypedId<>(resultSet.getObject("id", UUID.class))));
    assertEquals(1, results.size());
    assertEquals(createBusinessRecord.business().getId(), results.get(0).businessId());
  }

  @Test
  void mustacheContextAndSqlParamsCombined_Record() {
    final RequestPojoRecord request =
        new RequestPojoRecord(
            createBusinessRecord.business().getType().name(),
            createBusinessRecord.business().getBusinessName());
    final BeanPropertySqlParameterSource source = new BeanPropertySqlParameterSource(request);
    final MustacheSqlBeanPropertyContext context = new MustacheSqlBeanPropertyContext(source);
    final Template template = Mustache.compiler().compile(QUERY_TEMPLATE);
    final String query = template.execute(context);

    final List<BusinessResult> results =
        jdbcTemplate.query(
            query,
            source,
            (resultSet, rowNum) ->
                new BusinessResult(new TypedId<>(resultSet.getObject("id", UUID.class))));
    assertEquals(1, results.size());
    assertEquals(createBusinessRecord.business().getId(), results.get(0).businessId());
  }

  @Test
  void mapForMustacheContextAndSqlParams() {
    final MapSqlParameterSource source =
        new MapSqlParameterSource()
            .addValue("type", createBusinessRecord.business().getType().name())
            .addValue("name", createBusinessRecord.business().getBusinessName());
    final Template template = Mustache.compiler().compile(QUERY_TEMPLATE);
    final String query = template.execute(source);

    final List<BusinessResult> results =
        jdbcTemplate.query(
            query,
            source,
            (resultSet, rowNum) ->
                new BusinessResult(new TypedId<>(resultSet.getObject("id", UUID.class))));
    assertEquals(1, results.size());
    assertEquals(createBusinessRecord.business().getId(), results.get(0).businessId());
  }

  @Getter
  @RequiredArgsConstructor
  private static class RequestPojo {
    private final String type;
    private final String name;
  }

  private record RequestPojoRecord(String type, String name) {}

  private record BusinessResult(TypedId<BusinessId> businessId) {}
}
