package com.tranwall.capital.client.gcs;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "client.google.storage")
@Getter
@Setter
public class GoogleStorageProperties {
  private String credentials;
  private String receiptBucketName;
  private boolean createBucket;
  private boolean enabled;
}
