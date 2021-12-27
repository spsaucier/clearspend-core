package com.clearspend.capital.service;

import com.clearspend.capital.client.sendgrid.SendGridProperties;
import com.clearspend.capital.client.twilio.TwilioProperties;
import com.clearspend.capital.data.model.BusinessProspect;
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import com.sendgrid.helpers.mail.objects.Personalization;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.rest.verify.v2.service.Verification;
import com.twilio.rest.verify.v2.service.Verification.Channel;
import com.twilio.rest.verify.v2.service.VerificationCheck;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Slf4j
@Profile("!test")
@Service
public class TwilioService {

  private final String FIRST_NAME_KEY = "first_name";
  private final String REASONS_KEY = "reasons";
  private final String FORGOT_PASSWORD_CHANGE_PASSWORD_ID_KEY = "changePasswordId";

  private final TwilioProperties twilioProperties;
  private final SendGridProperties sendGridProperties;
  protected SendGrid sendGrid;

  public TwilioService(TwilioProperties twilioProperties, SendGridProperties sendGridProperties) {
    this.twilioProperties = twilioProperties;
    this.sendGridProperties = sendGridProperties;
  }

  @PostConstruct
  protected void initTwilio() {
    Twilio.init(twilioProperties.getAccountSid(), twilioProperties.getAuthToken());
    this.sendGrid = new SendGrid(sendGridProperties.getApiKey());
  }

  @PreDestroy
  protected void destroyTwilio() {
    Twilio.destroy();
  }

  public Message sendNotificationSms(String to, String messageText) {
    return Message.creator(
            new com.twilio.type.PhoneNumber(to),
            twilioProperties.getMessageServiceId(),
            messageText)
        .create();
  }

  public void sendNotificationEmail(String to, String messageText) {
    send(
        new Mail(
            new Email(sendGridProperties.getNotificationsSenderEmail()),
            sendGridProperties.getNotificationsEmailSubject(),
            new Email(to),
            new Content("text/plain", messageText)));
  }

  public void sendOnboardingWelcomeEmail(String to, BusinessProspect businessProspect) {
    Mail mail = new Mail();
    mail.setFrom(new Email(sendGridProperties.getNotificationsSenderEmail()));
    mail.setTemplateId(sendGridProperties.getOnboardingWelcomeEmailTemplateId());

    Personalization personalization = new Personalization();
    personalization.addDynamicTemplateData(
        FIRST_NAME_KEY, businessProspect.getFirstName().getEncrypted());
    personalization.addTo(new Email(to));
    mail.addPersonalization(personalization);

    send(mail);
  }

  public void sendKybKycPassEmail(String to, String firstName) {
    Mail mail = new Mail();
    mail.setFrom(new Email(sendGridProperties.getNotificationsSenderEmail()));
    mail.setTemplateId(sendGridProperties.getKybKycPassEmailTemplateId());

    Personalization personalization = new Personalization();
    personalization.addDynamicTemplateData(FIRST_NAME_KEY, firstName);
    personalization.addTo(new Email(to));
    mail.addPersonalization(personalization);

    send(mail);
  }

  public void sendKybKycFailEmail(String to, String firstName, List<String> reasons) {
    Mail mail = new Mail();
    mail.setFrom(new Email(sendGridProperties.getNotificationsSenderEmail()));
    mail.setTemplateId(sendGridProperties.getKybKycFailEmailTemplateId());

    Personalization personalization = new Personalization();
    personalization.addDynamicTemplateData(FIRST_NAME_KEY, firstName);
    personalization.addDynamicTemplateData(REASONS_KEY, reasons);
    personalization.addTo(new Email(to));
    mail.addPersonalization(personalization);

    send(mail);
  }

  public void sendResetPasswordEmail(String to, String changePasswordId) {
    Mail mail = new Mail();
    mail.setFrom(new Email(sendGridProperties.getNotificationsSenderEmail()));
    mail.setTemplateId(sendGridProperties.getForgotPasswordEmailTemplateId());

    Personalization personalization = new Personalization();
    personalization.addDynamicTemplateData(
        FORGOT_PASSWORD_CHANGE_PASSWORD_ID_KEY, changePasswordId);
    personalization.addTo(new Email(to));
    mail.addPersonalization(personalization);

    send(mail);
  }

  @SneakyThrows
  private void send(Mail mail) {
    Request request = new Request();
    request.setMethod(Method.POST);
    request.setEndpoint("mail/send");
    request.setBody(mail.build());
    sendGrid.api(request);
  }

  public Verification sendVerificationSms(String to) {
    return Verification.creator(twilioProperties.getVerifyServiceId(), to, Channel.SMS.toString())
        .create();
  }

  public Verification sendVerificationEmail(String to, BusinessProspect businessProspect) {
    return Verification.creator(twilioProperties.getVerifyServiceId(), to, Channel.EMAIL.toString())
        .setChannelConfiguration(
            Map.of("substitutions", Map.of(FIRST_NAME_KEY, businessProspect.getFirstName())))
        .create();
  }

  public VerificationCheck checkVerification(String subject, String challenge) {
    return VerificationCheck.creator(twilioProperties.getVerifyServiceId(), challenge)
        .setTo(subject)
        .create();
  }
}