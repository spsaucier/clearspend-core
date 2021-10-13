package com.tranwall.capital.client.twilio;

import javax.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Validated
@Component
@ConfigurationProperties(prefix = "client.twilio")
@Getter
@Setter
public class TwilioProperties {

  @NotBlank private String accountSid;

  @NotBlank private String authToken;

  @NotBlank private String messageServiceId;

  @NotBlank private String verifyServiceId;
}
