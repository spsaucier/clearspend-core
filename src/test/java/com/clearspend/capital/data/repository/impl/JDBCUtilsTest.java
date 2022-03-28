package com.clearspend.capital.data.repository.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.TestHelper.CreateBusinessRecord;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.data.model.business.Business;
import com.clearspend.capital.data.repository.business.BusinessRepository;
import com.clearspend.capital.data.repository.impl.JDBCUtils.MustacheQueryConfig;
import com.clearspend.capital.data.repository.impl.JDBCUtils.QueryAndResult;
import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Template;
import java.util.List;
import java.util.UUID;
import javax.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

@RequiredArgsConstructor(onConstructor = @__({@Autowired}))
public class JDBCUtilsTest extends BaseCapitalTest {
  private static final String NON_DYNAMIC_QUERY =
      """
          SELECT *
          FROM business
          """;
  private static final String QUERY_TEMPLATE =
      """
                SELECT *
                FROM business
                WHERE type = :type
                {{#name}}AND business_name = :name{{/name}}
                """;
  private static final String QUERY_WITHOUT_NAME =
      """
                SELECT *
                FROM business
                WHERE type = :type
                """;
  private static final String QUERY_WITH_NAME =
      """
                SELECT *
                FROM business
                WHERE type = :type
                AND business_name = :name
                """;
  private final Template queryTemplate = Mustache.compiler().compile(QUERY_TEMPLATE);
  private final Template nonDynamicQueryTemplate = Mustache.compiler().compile(NON_DYNAMIC_QUERY);

  private final TestHelper testHelper;
  private final BusinessRepository businessRepository;
  private final EntityManager entityManager;
  private CreateBusinessRecord createBusinessRecord1;
  private CreateBusinessRecord createBusinessRecord2;

  @BeforeEach
  void setup() {
    createBusinessRecord1 = testHelper.createBusiness();
    createBusinessRecord2 = testHelper.createBusiness();
    assertEquals(2, businessRepository.count());
    assertEquals(
        createBusinessRecord1.business().getType(), createBusinessRecord2.business().getType());
    assertNotEquals(
        createBusinessRecord1.business().getBusinessName(),
        createBusinessRecord2.business().getBusinessName());
  }

  @Test
  void executeMustacheQuery_NativeQuery_NoParams() {
    final MustacheQueryConfig<Business> config =
        MustacheQueryConfig.<Business>builder().entityClass(Business.class).build();
    final QueryAndResult<Business> queryAndResult =
        JDBCUtils.executeMustacheQuery(entityManager, nonDynamicQueryTemplate, config);
    assertEquals(NON_DYNAMIC_QUERY.trim(), queryAndResult.query().trim());
    assertEquals(2, queryAndResult.result().size());
    final List<TypedId<BusinessId>> businessIds = getBusinessIds(queryAndResult.result());
    assertTrue(businessIds.contains(createBusinessRecord1.business().getBusinessId()));
    assertTrue(businessIds.contains(createBusinessRecord2.business().getBusinessId()));
  }

  private List<TypedId<BusinessId>> getBusinessIds(final List<Business> businesses) {
    return businesses.stream().map(Business::getBusinessId).toList();
  }

  private List<TypedId<BusinessId>> getBusinessRecordIds(final List<BusinessRecord> businesses) {
    return businesses.stream().map(BusinessRecord::id).toList();
  }

  @Test
  void executeMustacheQuery_NativeQuery_WithMapParams_OneParam() {
    final SqlParameterSource source =
        new MapSqlParameterSource()
            .addValue("type", createBusinessRecord1.business().getType().name());
    final MustacheQueryConfig<Business> config =
        MustacheQueryConfig.<Business>builder()
            .parameterSource(source)
            .entityClass(Business.class)
            .build();
    final QueryAndResult<Business> queryAndResult =
        JDBCUtils.executeMustacheQuery(entityManager, queryTemplate, config);
    assertEquals(QUERY_WITHOUT_NAME.trim(), queryAndResult.query().trim());
    assertEquals(2, queryAndResult.result().size());
    final List<TypedId<BusinessId>> businessIds = getBusinessIds(queryAndResult.result());
    assertTrue(businessIds.contains(createBusinessRecord1.business().getBusinessId()));
    assertTrue(businessIds.contains(createBusinessRecord2.business().getBusinessId()));
  }

  @Test
  void executeMustacheQuery_NativeQuery_WithMapParams_MultiParam() {
    final SqlParameterSource source =
        new MapSqlParameterSource()
            .addValue("type", createBusinessRecord1.business().getType().name())
            .addValue("name", createBusinessRecord1.business().getBusinessName());
    final MustacheQueryConfig<Business> config =
        MustacheQueryConfig.<Business>builder()
            .parameterSource(source)
            .entityClass(Business.class)
            .build();
    final QueryAndResult<Business> queryAndResult =
        JDBCUtils.executeMustacheQuery(entityManager, queryTemplate, config);
    assertEquals(QUERY_WITH_NAME.trim(), queryAndResult.query().trim());
    assertEquals(1, queryAndResult.result().size());
    assertEquals(
        queryAndResult.result().get(0).getBusinessId(),
        createBusinessRecord1.business().getBusinessId());
  }

  @Test
  void executeMustacheQuery_NativeQuery_WithBeanParams_OneParam() {
    final BeanParamInput input =
        new BeanParamInput(createBusinessRecord1.business().getType().name(), null);
    final SqlParameterSource source = new BeanPropertySqlParameterSource(input);
    final MustacheQueryConfig<Business> config =
        MustacheQueryConfig.<Business>builder()
            .parameterSource(source)
            .entityClass(Business.class)
            .build();
    final QueryAndResult<Business> queryAndResult =
        JDBCUtils.executeMustacheQuery(entityManager, queryTemplate, config);
    assertEquals(QUERY_WITHOUT_NAME.trim(), queryAndResult.query().trim());
    assertEquals(2, queryAndResult.result().size());
    final List<TypedId<BusinessId>> businessIds = getBusinessIds(queryAndResult.result());
    assertTrue(businessIds.contains(createBusinessRecord1.business().getBusinessId()));
    assertTrue(businessIds.contains(createBusinessRecord2.business().getBusinessId()));
  }

  @Test
  void executeMustacheQuery_NativeQuery_WithBeanParams_MultiParam() {
    final BeanParamInput input =
        new BeanParamInput(
            createBusinessRecord1.business().getType().name(),
            createBusinessRecord1.business().getBusinessName());
    final SqlParameterSource source = new BeanPropertySqlParameterSource(input);
    final MustacheQueryConfig<Business> config =
        MustacheQueryConfig.<Business>builder()
            .parameterSource(source)
            .entityClass(Business.class)
            .build();
    final QueryAndResult<Business> queryAndResult =
        JDBCUtils.executeMustacheQuery(entityManager, queryTemplate, config);
    assertEquals(QUERY_WITH_NAME.trim(), queryAndResult.query().trim());
    assertEquals(1, queryAndResult.result().size());
    assertEquals(
        queryAndResult.result().get(0).getBusinessId(),
        createBusinessRecord1.business().getBusinessId());
  }

  @Test
  void executeMustacheQuery_JdbcTemplate_NoParams() {
    final MustacheQueryConfig<BusinessRecord> config =
        MustacheQueryConfig.<BusinessRecord>builder().rowMapper(BUSINESS_RECORD_MAPPER).build();
    final QueryAndResult<BusinessRecord> queryAndResult =
        JDBCUtils.executeMustacheQuery(entityManager, nonDynamicQueryTemplate, config);
    assertEquals(NON_DYNAMIC_QUERY.trim(), queryAndResult.query().trim());
    assertEquals(2, queryAndResult.result().size());
    final List<TypedId<BusinessId>> businessIds = getBusinessRecordIds(queryAndResult.result());
    assertTrue(businessIds.contains(createBusinessRecord1.business().getBusinessId()));
    assertTrue(businessIds.contains(createBusinessRecord2.business().getBusinessId()));
  }

  @Test
  void executeMustacheQuery_JdbcTemplate_WithMapParams_OneParam() {
    final SqlParameterSource source =
        new MapSqlParameterSource()
            .addValue("type", createBusinessRecord1.business().getType().name());
    final MustacheQueryConfig<BusinessRecord> config =
        MustacheQueryConfig.<BusinessRecord>builder()
            .parameterSource(source)
            .rowMapper(BUSINESS_RECORD_MAPPER)
            .build();
    final QueryAndResult<BusinessRecord> queryAndResult =
        JDBCUtils.executeMustacheQuery(entityManager, queryTemplate, config);
    assertEquals(QUERY_WITHOUT_NAME.trim(), queryAndResult.query().trim());
    assertEquals(2, queryAndResult.result().size());
    final List<TypedId<BusinessId>> businessIds = getBusinessRecordIds(queryAndResult.result());
    assertTrue(businessIds.contains(createBusinessRecord1.business().getBusinessId()));
    assertTrue(businessIds.contains(createBusinessRecord2.business().getBusinessId()));
  }

  @Test
  void executeMustacheQuery_JdbcTemplate_WithMapParams_MultiParam() {
    final SqlParameterSource source =
        new MapSqlParameterSource()
            .addValue("type", createBusinessRecord1.business().getType().name())
            .addValue("name", createBusinessRecord1.business().getBusinessName());
    final MustacheQueryConfig<BusinessRecord> config =
        MustacheQueryConfig.<BusinessRecord>builder()
            .parameterSource(source)
            .rowMapper(BUSINESS_RECORD_MAPPER)
            .build();
    final QueryAndResult<BusinessRecord> queryAndResult =
        JDBCUtils.executeMustacheQuery(entityManager, queryTemplate, config);
    assertEquals(QUERY_WITH_NAME.trim(), queryAndResult.query().trim());
    assertEquals(1, queryAndResult.result().size());
    assertEquals(
        queryAndResult.result().get(0).id(), createBusinessRecord1.business().getBusinessId());
  }

  @Test
  void executeMustacheQuery_JdbcTemplate_WithBeanParams_OneParam() {
    final BeanParamInput input =
        new BeanParamInput(createBusinessRecord1.business().getType().name(), null);
    final SqlParameterSource source = new BeanPropertySqlParameterSource(input);
    final MustacheQueryConfig<BusinessRecord> config =
        MustacheQueryConfig.<BusinessRecord>builder()
            .parameterSource(source)
            .rowMapper(BUSINESS_RECORD_MAPPER)
            .build();
    final QueryAndResult<BusinessRecord> queryAndResult =
        JDBCUtils.executeMustacheQuery(entityManager, queryTemplate, config);
    assertEquals(QUERY_WITHOUT_NAME.trim(), queryAndResult.query().trim());
    assertEquals(2, queryAndResult.result().size());
    final List<TypedId<BusinessId>> businessIds = getBusinessRecordIds(queryAndResult.result());
    assertTrue(businessIds.contains(createBusinessRecord1.business().getBusinessId()));
    assertTrue(businessIds.contains(createBusinessRecord2.business().getBusinessId()));
  }

  @Test
  void executeMustacheQuery_JdbcTemplate_WithBeanParams_MultiParam() {
    final BeanParamInput input =
        new BeanParamInput(
            createBusinessRecord1.business().getType().name(),
            createBusinessRecord1.business().getBusinessName());
    final SqlParameterSource source = new BeanPropertySqlParameterSource(input);
    final MustacheQueryConfig<BusinessRecord> config =
        MustacheQueryConfig.<BusinessRecord>builder()
            .parameterSource(source)
            .rowMapper(BUSINESS_RECORD_MAPPER)
            .build();
    final QueryAndResult<BusinessRecord> queryAndResult =
        JDBCUtils.executeMustacheQuery(entityManager, queryTemplate, config);
    assertEquals(QUERY_WITH_NAME.trim(), queryAndResult.query().trim());
    assertEquals(1, queryAndResult.result().size());
    assertEquals(
        queryAndResult.result().get(0).id(), createBusinessRecord1.business().getBusinessId());
  }

  record BeanParamInput(String type, String name) {}

  record BusinessRecord(TypedId<BusinessId> id) {}

  private static final RowMapper<BusinessRecord> BUSINESS_RECORD_MAPPER =
      (resultSet, rowNum) ->
          new BusinessRecord(new TypedId<>(resultSet.getObject("id", UUID.class)));
}
