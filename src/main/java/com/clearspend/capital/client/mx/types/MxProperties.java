package com.clearspend.capital.client.mx.types;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@ToString
@Configuration
@ConfigurationProperties(prefix = "client.mx")
public class MxProperties {
  private String baseUrl;
  private String authSecret;
  private Integer connectTimeout;
  private Integer responseTimeout;
  private Integer readTimeout;
  private Integer writeTimeout;
  private Boolean useMxLogos;
}
