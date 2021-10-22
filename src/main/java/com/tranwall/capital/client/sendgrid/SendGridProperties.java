package com.tranwall.capital.client.sendgrid;

import javax.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Validated
@Component
@ConfigurationProperties(prefix = "client.sendgrid")
@Getter
@Setter
public class SendGridProperties {

  @NotBlank private String apiKey;

  @NotBlank private String notificationsSenderEmail;

  @NotBlank private String notificationsEmailSubject;

  @NotBlank private String onboardingWelcomeEmailTemplateId;
}
