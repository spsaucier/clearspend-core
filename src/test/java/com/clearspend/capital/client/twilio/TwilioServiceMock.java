package com.clearspend.capital.client.twilio;

import com.clearspend.capital.client.sendgrid.SendGridProperties;
import com.clearspend.capital.service.TwilioService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.twilio.Twilio;
import com.twilio.http.TwilioRestClient;
import lombok.Getter;
import lombok.SneakyThrows;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service("twilioService")
public class TwilioServiceMock extends TwilioService {

  @Value("${mockServerPort}")
  Integer mockServerPort;

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Getter private String changePasswordId;

  public TwilioServiceMock(
      TwilioProperties twilioProperties, SendGridProperties sendGridProperties) {
    super(twilioProperties, sendGridProperties);
  }

  @Override
  protected void initTwilio() {
    Twilio.destroy();
    Twilio.init("", "");
    ProxiedTwilioClientCreator clientCreator =
        new ProxiedTwilioClientCreator("asdf", "ghjk", "localhost", mockServerPort);
    TwilioRestClient twilioRestClient = clientCreator.getClient();
    Twilio.setRestClient(twilioRestClient);

    this.sendGrid = Mockito.mock(SendGrid.class);
  }

  @SneakyThrows
  public void expectResetPassword() {
    Mockito.doAnswer(
            invocation -> {
              Request request = invocation.getArgument(0, Request.class);
              //    objectMapper.readValue(request.getBody(), )
              changePasswordId =
                  (String)
                      objectMapper
                          .readValue(request.getBody(), Mail.class)
                          .getPersonalization()
                          .get(0)
                          .getDynamicTemplateData()
                          .get("changePasswordId");
              return new Response();
            })
        .when(sendGrid)
        .api(Mockito.any());
  }

  public SendGrid getSendGrid() {
    return this.sendGrid;
  }
}
