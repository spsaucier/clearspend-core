package com.tranwall.capital.client.twilio;

import com.sendgrid.SendGrid;
import com.tranwall.capital.client.sendgrid.SendGridProperties;
import com.tranwall.capital.service.TwilioService;
import com.twilio.Twilio;
import com.twilio.http.TwilioRestClient;
import org.mockito.Mockito;
import org.mockserver.springtest.MockServerPort;
import org.springframework.stereotype.Service;

@Service("twilioService")
public class TwilioServiceMock extends TwilioService {

  @MockServerPort Integer mockServerPort;

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
}
