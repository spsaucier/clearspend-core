package com.clearspend.capital.util;

import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.data.model.enums.AccountActivityStatus;
import com.clearspend.capital.data.model.enums.AccountActivityType;
import com.clearspend.capital.data.repository.impl.JDBCUtils;
import com.clearspend.capital.service.AccountActivityFilterCriteria;
import com.clearspend.capital.service.type.PageToken;
import com.clearspend.capital.service.type.PageToken.OrderBy;
import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Template;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@Slf4j
class MustacheTemplateTest {

  @Test
  void test() {
    Template template =
        Mustache.compiler()
            .compile(
                "foo: {{foo}}<br>\n"
                    + "getBar(): {{bar}}<br>\n"
                    + "<br>\n"
                    + "strings:<br>\n"
                    + "{{#strings}}\n"
                    + "  value: {{.}}<br>\n"
                    + "{{/strings}}\n"
                    + "<br>\n"
                    + "exists:{{#isTrue}} true {{/isTrue}}\n"
                    + "<br>\n"
                    + "list:<br>\n"
                    + "{{#list}}\n"
                    + "  value: {{.}}<br>\n"
                    + "{{/list}}\n"
                    + "<br>\n"
                    + "map:<br>\n"
                    + "{{#map}}\n"
                    + "  {{key1}}, {{key2}}, {{key3}}\n"
                    + "{{/map}}");

    SampleData sampleData = new SampleData();
    String out = template.execute(sampleData);
    log.info(out);
    Assertions.assertEquals(
        "foo: foo<br>\n"
            + "getBar(): bar<br>\n"
            + "<br>\n"
            + "strings:<br>\n"
            + "  value: S1<br>\n"
            + "  value: S2<br>\n"
            + "  value: S3<br>\n"
            + "<br>\n"
            + "exists:\n"
            + "<br>\n"
            + "list:<br>\n"
            + "  value: L1<br>\n"
            + "  value: L2<br>\n"
            + "  value: L3<br>\n"
            + "<br>\n"
            + "map:<br>\n"
            + "  value1, value2, value3\n",
        out);
  }

  // Read a file from the resource directory
  public String getResourceText(String path) throws IOException {
    try (InputStream is = getClass().getResourceAsStream(path)) {
      return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  public class SampleData {

    public String foo = "foo";

    public String getBar() {
      return "bar";
    }

    public String[] strings = {"S1", "S2", "S3"};

    public List list = List.of("L1", "L2", "L3");

    public Map map = Map.of("key1", "value1", "key2", "value2", "key3", "value3");
  }

  @Test
  void testSql() {
    Template template =
        Mustache.compiler()
            .compile(
                "select {{#count}}count(*){{/count}}{{^count}}*{{/count}} from account_activity\n"
                    + "where\n"
                    + "    ( account_activity.hide_after >= now() or account_activity.hide_after is null )\n"
                    + "    and ( account_activity.visible_after <= now() or account_activity.visible_after is null)\n"
                    + "    and account_activity.business_id = '{{businessId}}'\n"
                    + "    {{#userId}} and account_activity.user_id = '{{userId}}' {{/userId}}\n"
                    + "    {{#cardId}} and account_activity.card_card_id = '{{cardId}}' {{/cardId}}\n"
                    + "    {{#allocationId}} and account_activity.allocation_id = '{{allocationId}}'{{/allocationId}}\n"
                    + "    {{#types.0}} and account_activity.type in ({{{typesString}}}) {{/types.0}}\n"
                    + "    {{#statuses.0}} and account_activity.status in ( {{{statusesString}}} ) {{/statuses.0}}\n"
                    + "    {{#from}} {{#to}}\n"
                    + "    and account_activity.activity_time between {{from}} and {{to}}\n"
                    + "    {{/to}}  {{/from}}\n"
                    + "    {{#searchText}}\n"
                    + "     AND (\n"
                    + "        accountActivity.card_last_four LIKE ('%{{searchText}}%')\n"
                    + "        OR UPPER(accountActivity.merchant_name) LIKE UPPER('%{{searchText}}%')\n"
                    + "        OR cast_string(accountActivity.amount_amount) LIKE ('%{{searchText}}%')\n"
                    + "        OR cast_string(accountActivity.activity_time) LIKE ('%{{searchText}}%')\n"
                    + "    )\n"
                    + "    {{/searchText}}\n"
                    + "{{^count}}order by\n"
                    + "    account_activity.activity_time desc nulls last,\n"
                    + "    account_activity.id desc "
                    + "{{#pageToken.pageSize}}\n"
                    + "limit {{.}}\n"
                    + "{{/pageToken.pageSize}}\n"
                    + "{{#pageToken.firstResult}}\n"
                    + "offset {{.}}\n"
                    + "{{/pageToken.firstResult}}"
                    + "{{/count}}");

    AccountActivityFilterCriteria accountActivityFilterCriteria =
        new AccountActivityFilterCriteria(
            new TypedId<>(),
            new TypedId<>(),
            new TypedId<>(),
            new TypedId<>(),
            List.of(AccountActivityType.BANK_DEPOSIT, AccountActivityType.FEE),
            "text",
            OffsetDateTime.now(),
            OffsetDateTime.now(),
            List.of(AccountActivityStatus.APPROVED),
            BigDecimal.ONE,
            BigDecimal.TEN,
            List.of(),
            true,
            true,
            new PageToken(10, 5, List.of(OrderBy.builder().item("id").build())));

    StringWriter out = new StringWriter();
    template.execute(accountActivityFilterCriteria, JDBCUtils.JMUSTACHE_COUNT_CONTEXT, out);
    log.info(out.toString());
    template.execute(accountActivityFilterCriteria, JDBCUtils.JMUSTACHE_ENTITY_CONTEXT, out);
    log.info(out.toString());
  }
}
