package com.tranwall.capital.service;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import com.tranwall.capital.client.sendgrid.SendGridProperties;
import com.tranwall.capital.client.twilio.TwilioProperties;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.rest.verify.v2.service.Verification;
import com.twilio.rest.verify.v2.service.Verification.Channel;
import com.twilio.rest.verify.v2.service.VerificationCheck;
import java.io.IOException;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class TwilioService {

  private final TwilioProperties twilioProperties;
  private final SendGridProperties sendGridProperties;
  private final SendGrid sendGrid;

  public TwilioService(
      TwilioProperties twilioProperties,
      SendGridProperties sendGridProperties) {
    this.twilioProperties = twilioProperties;
    this.sendGridProperties = sendGridProperties;

    this.sendGrid = new SendGrid(sendGridProperties.getApiKey());
  }

  @PostConstruct
  void initTwilio() {
    Twilio.init(twilioProperties.getAccountSid(), twilioProperties.getAuthToken());
  }

  @PreDestroy
  void destroyTwilio() {
    Twilio.destroy();
  }

  public Message sendNotificationSms(String to, String messageText) {
    return Message
        .creator(
            new com.twilio.type.PhoneNumber(to),
            twilioProperties.getMessageServiceId(),
            messageText)
        .create();
  }

  public Response sendNotificationEmail(String to, String messageText) throws IOException {
    Mail mail = new Mail(
        new Email(sendGridProperties.getNotificationsSenderEmail()),
        sendGridProperties.getNotificationsEmailSubject(),
        new Email(to),
        new Content("text/plain", messageText));
    Request request = new Request();
    request.setMethod(Method.POST);
    request.setEndpoint("mail/send");
    request.setBody(mail.build());
    return sendGrid.api(request);
  }

  public Verification sendVerificationSms(String to) {
    return Verification
        .creator(
            twilioProperties.getVerifyServiceId(),
            to,
            Channel.SMS.toString())
        .create();
  }

  public Verification sendVerificationEmail(String to) {
    return Verification.creator(
            twilioProperties.getVerifyServiceId(),
            to,
            Channel.EMAIL.toString())
        .create();
  }

  public VerificationCheck checkVerification(String subject, String challenge) {
    return VerificationCheck
        .creator(
            twilioProperties.getVerifyServiceId(),
            challenge)
        .setTo(subject)
        .create();
  }
}
