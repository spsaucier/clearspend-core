package com.clearspend.capital.service.notification;

import com.clearspend.capital.service.type.PushNotificationEvent;
import com.clearspend.capital.service.type.TransactionNotificationEventType;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Map;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

@Service
@Slf4j
public class NotificationTemplateProcessor {

  private final Map<String, String> templates;

  public NotificationTemplateProcessor(
      @Value("classpath:notifications/templates.yml") @NonNull Resource yamlTemplate)
      throws IOException {
    Yaml yaml = new Yaml();
    // TODO: move notification templates outside of Clearspend resource
    this.templates = yaml.load(yamlTemplate.getInputStream());
  }

  String retrieveTransactionEventNotificationCompiledTemplate(
      TransactionNotificationEventType type, PushNotificationEvent pushNotificationEvent) {

    String formattedAmount =
        new DecimalFormat("$0.00")
            .format(pushNotificationEvent.getAmount().getAmount().doubleValue());
    String merchant = pushNotificationEvent.getMerchantName();

    return String.format(templates.get(type.name()), formattedAmount, merchant);
  }
}
