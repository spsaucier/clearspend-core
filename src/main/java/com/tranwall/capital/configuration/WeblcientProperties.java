package com.tranwall.capital.configuration;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@ToString
@Configuration
@ConfigurationProperties(prefix = "client")
public class WeblcientProperties {

  private int connectTimeout;
  private int responseTimeout;
  private int readTimeout;
  private int writeTimeout;
}
