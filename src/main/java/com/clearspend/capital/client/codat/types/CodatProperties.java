package com.clearspend.capital.client.codat.types;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@ToString
@Configuration
@ConfigurationProperties(prefix = "client.codat")
public class CodatProperties {

  private String authToken;
  private String baseUrl;
  private String authSecret;
  private String quickbooksonlineCode;
  private Integer connectTimeout;
  private Integer responseTimeout;
  private Integer readTimeout;
  private Integer writeTimeout;
}
