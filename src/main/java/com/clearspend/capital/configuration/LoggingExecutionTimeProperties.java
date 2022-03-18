package com.clearspend.capital.configuration;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.slf4j.event.Level;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@ToString
@Configuration
@ConfigurationProperties(prefix = "logging.execution-time")
public class LoggingExecutionTimeProperties {

  private boolean enabled;
  private Level level;
  private String mode;
  private long threshold;
}
