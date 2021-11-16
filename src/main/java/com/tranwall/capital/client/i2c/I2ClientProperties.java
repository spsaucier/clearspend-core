package com.tranwall.capital.client.i2c;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@ToString
@Configuration
@ConfigurationProperties(prefix = "client.i2c")
public class I2ClientProperties {

  private String url;
  private String acquirerId;
  private String userId;
  private String password;

  private Program stakeholderProgram = new Program();
  private Program virtualProgram = new Program();
  private Program plasticProgram = new Program();

  @Getter
  @Setter
  @ToString
  public static class Program {
    private String id;
    private String bin;
    private String startingNumbers;
  }
}
