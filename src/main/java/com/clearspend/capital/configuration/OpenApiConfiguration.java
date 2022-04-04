package com.clearspend.capital.configuration;

import com.clearspend.capital.data.model.enums.LimitPeriod;
import com.clearspend.capital.data.model.enums.LimitType;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.MapSchema;
import io.swagger.v3.oas.models.media.NumberSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import java.util.EnumSet;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

  /**
   * Creates a custom open api definition. Needed in order to extend swagger schema definitions for
   * complex types like Map<LimitType, Map<LimitPeriod, Limit>>. (in this case swagger cannot detect
   * second map types properly)
   */
  @Bean
  public OpenAPI capitalOpenAPI() {
    return new OpenAPI()
        .components(
            new Components()
                .addSchemas("LimitTypeMap", createEnumMapSchema(LimitType.class, "LimitPeriodMap"))
                .addSchemas("LimitPeriodMap", createEnumMapSchema(LimitPeriod.class, "LimitPeriod"))
                .addSchemas(
                    "LimitPeriod",
                    new ObjectSchema()
                        .addProperties("amount", new NumberSchema())
                        .addProperties("usedAmount", new NumberSchema())));
  }

  private <T extends Enum<T>> MapSchema createEnumMapSchema(Class<T> enumClass, String objectName) {
    MapSchema mapSchema = new MapSchema();

    EnumSet.allOf(enumClass)
        .forEach(
            enumValue ->
                mapSchema.addProperties(
                    enumValue.name(),
                    new ObjectSchema().$ref(Components.COMPONENTS_SCHEMAS_REF + objectName)));

    return mapSchema;
  }
}
