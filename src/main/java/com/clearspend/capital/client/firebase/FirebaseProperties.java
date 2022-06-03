package com.clearspend.capital.client.firebase;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "client.firebase")
@Getter
@Setter
public class FirebaseProperties {
  private String credentials;
}
