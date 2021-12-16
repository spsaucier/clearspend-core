package com.tranwall.capital.configuration;

import com.tranwall.capital.data.model.enums.LimitPeriod;
import com.tranwall.capital.data.model.enums.LimitType;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.MapSchema;
import io.swagger.v3.oas.models.media.NumberSchema;
import io.swagger.v3.oas.models.media.StringSchema;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;
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
                .addSchemas(
                    "LimitTypeMap",
                    new MapSchema()
                        .addProperties(
                            "limitType", new StringSchema()._enum(enumToList(LimitType.class)))
                        .additionalProperties(
                            new MapSchema()
                                .addProperties(
                                    "limitPeriod",
                                    new StringSchema()._enum(enumToList(LimitPeriod.class)))
                                .additionalProperties(new NumberSchema()))));
  }

  private <T extends Enum<T>> List<String> enumToList(Class<T> enumClass) {
    return EnumSet.allOf(enumClass).stream().map(Enum::name).collect(Collectors.toList());
  }
}
