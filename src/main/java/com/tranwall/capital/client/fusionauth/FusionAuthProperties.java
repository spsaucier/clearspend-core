package com.tranwall.capital.client.fusionauth;

import javax.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Validated
@Component
@ConfigurationProperties(prefix = "client.fusionauth")
@Getter
@Setter
public class FusionAuthProperties {

  @NotBlank
  private String apiKey;

  @NotBlank
  private String baseUrl;
}
