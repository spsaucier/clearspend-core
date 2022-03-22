package com.clearspend.capital.service;

import com.clearspend.capital.client.sendgrid.SendGridProperties;
import com.clearspend.capital.client.twilio.TwilioProperties;
import com.clearspend.capital.data.model.business.BusinessProspect;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import com.sendgrid.helpers.mail.objects.Personalization;
import com.twilio.Twilio;
import com.twilio.rest.verify.v2.service.Verification;
import com.twilio.rest.verify.v2.service.Verification.Channel;
import com.twilio.rest.verify.v2.service.VerificationCheck;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Slf4j
@Profile("!test")
@Service
public class TwilioService {

  private final String FIRST_NAME_KEY = "first_name";
  private final String REASONS_KEY = "reasons";
  private final String DOCS_LIST = "docs_list";
  private final String ENV_URL = "env-url";
  private final String FORGOT_PASSWORD_CHANGE_PASSWORD_ID_KEY = "change_password_id";
  private final String COMPANY_NAME_KEY = "company_name";
  private final String PASSWORD_KEY = "password";
  private final String CARD_LAST_FOUR_KEY = "card_last_four";
  private final String AMOUNT_KEY = "amount";
  private final String EMPLOYEE_NAME_KEY = "employee_name";
  private final String BANK_ACCOUNT_NAME_KEY = "bank_account_name";
  private final String BANK_ACCOUNT_LAST_FOUR_KEY = "bank_account_last_four";
  private final String BANK_ACCOUNT_OWNER_NAME_KEY = "bank_account_owner_name";
  private final String DATE_KEY = "date";

  private final ObjectMapper objectMapper = new ObjectMapper();

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
    personalization.addDynamicTemplateData(ENV_URL, sendGridProperties.getEnvURL());
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
    personalization.addDynamicTemplateData(ENV_URL, sendGridProperties.getEnvURL());
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
    personalization.addDynamicTemplateData(ENV_URL, sendGridProperties.getEnvURL());
    personalization.addTo(new Email(to));
    mail.addPersonalization(personalization);

    send(mail);
  }

  public void sendKybKycReviewStateEmail(String to, String firstName) {
    Mail mail = new Mail();
    mail.setFrom(new Email(sendGridProperties.getNotificationsSenderEmail()));
    mail.setTemplateId(sendGridProperties.getKybKycReviewStateTemplateId());

    Personalization personalization = new Personalization();
    personalization.addDynamicTemplateData(FIRST_NAME_KEY, firstName);
    personalization.addDynamicTemplateData(ENV_URL, sendGridProperties.getEnvURL());
    personalization.addTo(new Email(to));
    mail.addPersonalization(personalization);

    send(mail);
  }

  public void sendKybKycRequireAdditionalInfoEmail(
      String to, String firstName, List<String> additionalRequirements) {

    if (additionalRequirements.isEmpty()) {
      log.warn("Kyc Require documents Email can't be send. RequiredDocuments list is empty.");
      return;
    }

    Mail mail = new Mail();
    mail.setFrom(new Email(sendGridProperties.getNotificationsSenderEmail()));
    mail.setTemplateId(sendGridProperties.getKybKycRequireAdditionalInfoTemplateId());

    Personalization personalization = new Personalization();
    personalization.addDynamicTemplateData(FIRST_NAME_KEY, firstName);
    personalization.addDynamicTemplateData(REASONS_KEY, additionalRequirements);
    personalization.addDynamicTemplateData(ENV_URL, sendGridProperties.getEnvURL());
    personalization.addTo(new Email(to));
    mail.addPersonalization(personalization);

    send(mail);
  }

  public void sendKybKycRequireDocumentsEmail(
      String to, String firstName, List<String> requiredDocuments) {

    if (requiredDocuments.isEmpty()) {
      log.warn("Kyc Require documents Email can't be send. RequiredDocuments list is empty.");
      return;
    }

    Mail mail = new Mail();
    mail.setFrom(new Email(sendGridProperties.getNotificationsSenderEmail()));
    mail.setTemplateId(sendGridProperties.getKybKycRequireDocTemplateId());

    Personalization personalization = new Personalization();
    personalization.addDynamicTemplateData(FIRST_NAME_KEY, firstName);
    personalization.addDynamicTemplateData(DOCS_LIST, requiredDocuments);
    personalization.addDynamicTemplateData(ENV_URL, sendGridProperties.getEnvURL());
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
    personalization.addDynamicTemplateData(ENV_URL, sendGridProperties.getEnvURL());
    mail.addPersonalization(personalization);

    send(mail);
  }

  private void send(Mail mail) {

    if (!sendGridProperties.isEmailNotificationsEnabled()) {
      return;
    }

    Request request = new Request();
    request.setMethod(Method.POST);
    request.setEndpoint("mail/send");

    try {
      request.setBody(mail.build());

      log.debug(
          "Sending Email with subject {} and templateID {} via Sendgrid",
          mail.subject,
          mail.templateId);

      Response response = sendGrid.api(request);

      if (response != null && response.getStatusCode() != 202) {
        log.error(
            "Sendgrid failed to send {} email with code {}",
            objectMapper.writeValueAsString(request),
            response.getStatusCode());
      }
    } catch (IOException e) {
      log.error("failed to call Sendgrid (" + mail.subject + " [" + mail.templateId + "])", e);
    }
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

  private Mail initMailWithTemplate(String templateId, String to, Personalization personalization) {
    Mail mail = new Mail();
    mail.setFrom(new Email(sendGridProperties.getNotificationsSenderEmail()));
    mail.setTemplateId(templateId);
    personalization.addTo(new Email(to));
    personalization.addDynamicTemplateData(ENV_URL, sendGridProperties.getEnvURL());
    mail.addPersonalization(personalization);
    return mail;
  }

  /* Login: Password Reset Success */
  public void sendPasswordResetSuccessEmail(String to, String firstName) {
    Personalization personalization = new Personalization();
    personalization.addDynamicTemplateData(FIRST_NAME_KEY, firstName);
    Mail mail =
        initMailWithTemplate(
            sendGridProperties.getPasswordResetSuccessTemplateId(), to, personalization);
    send(mail);
  }

  /* Onboarding: Welcome Invite only */
  public void sendWelcomeByInviteOnlyEmail(String to, String firstName) {
    Personalization personalization = new Personalization();
    personalization.addDynamicTemplateData(FIRST_NAME_KEY, firstName);
    Mail mail =
        initMailWithTemplate(
            sendGridProperties.getWelcomeInviteOnlyTemplateId(), to, personalization);
    send(mail);
  }

  /* Onboarding: KYB/KYC docs received */
  public void sendKybKycDocsReceivedEmail(String to, String firstName) {
    Personalization personalization = new Personalization();
    personalization.addDynamicTemplateData(FIRST_NAME_KEY, firstName);
    Mail mail =
        initMailWithTemplate(
            sendGridProperties.getKybKycDocsReceivedTemplateId(), to, personalization);
    send(mail);
  }

  /* Bank Details Added */
  public void sendBankDetailsAddedEmail(
      String to,
      String firstName,
      String accountOwnerName,
      String accountName,
      String accountLastFour) {
    Personalization personalization = new Personalization();
    personalization.addDynamicTemplateData(FIRST_NAME_KEY, firstName);
    personalization.addDynamicTemplateData(BANK_ACCOUNT_OWNER_NAME_KEY, accountOwnerName);
    personalization.addDynamicTemplateData(BANK_ACCOUNT_NAME_KEY, accountName);
    personalization.addDynamicTemplateData(BANK_ACCOUNT_LAST_FOUR_KEY, accountLastFour);
    personalization.addDynamicTemplateData(
        DATE_KEY,
        Instant.now().atZone(ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("MM/dd/yyyy")));

    Mail mail =
        initMailWithTemplate(
            sendGridProperties.getBankDetailsAddedTemplateId(), to, personalization);
    send(mail);
  }

  /* Bank Funds Available */
  public void sendBankFundsAvailableEmail(String to, String firstName) {
    Personalization personalization = new Personalization();
    personalization.addDynamicTemplateData(FIRST_NAME_KEY, firstName);
    Mail mail =
        initMailWithTemplate(
            sendGridProperties.getBankFundsAvailableTemplateId(), to, personalization);
    send(mail);
  }

  /* Bank Funds Deposit Request */
  public void sendBankFundsDepositRequestEmail(String to, String firstName, String amount) {
    Personalization personalization = new Personalization();
    personalization.addDynamicTemplateData(FIRST_NAME_KEY, firstName);
    personalization.addDynamicTemplateData(AMOUNT_KEY, amount);
    Mail mail =
        initMailWithTemplate(
            sendGridProperties.getBankFundsDepositRequestTemplateId(), to, personalization);
    send(mail);
  }

  /* Bank Details Removed */
  public void sendBankDetailsRemovedEmail(String to, String firstName) {
    Personalization personalization = new Personalization();
    personalization.addDynamicTemplateData(FIRST_NAME_KEY, firstName);
    Mail mail =
        initMailWithTemplate(
            sendGridProperties.getBankDetailsRemovedTemplateId(), to, personalization);
    send(mail);
  }

  /* Bank Funds Available */
  public void sendBankFundsReturnEmail(String to, String firstName) {
    Personalization personalization = new Personalization();
    personalization.addDynamicTemplateData(FIRST_NAME_KEY, firstName);
    Mail mail =
        initMailWithTemplate(
            sendGridProperties.getBankFundsReturnTemplateId(), to, personalization);
    send(mail);
  }

  /* Bank Funds Withdrawal */
  public void sendBankFundsWithdrawalEmail(String to, String firstName, String amount) {
    Personalization personalization = new Personalization();
    personalization.addDynamicTemplateData(FIRST_NAME_KEY, firstName);
    personalization.addDynamicTemplateData(AMOUNT_KEY, amount);
    Mail mail =
        initMailWithTemplate(
            sendGridProperties.getBankFundsWithdrawalTemplateId(), to, personalization);
    send(mail);
  }

  /* Card : Card issued Owner Notification */
  public void sendCardIssuedNotifyOwnerEmail(String to, String ownerName, String employeeName) {
    Personalization personalization = new Personalization();
    personalization.addDynamicTemplateData(FIRST_NAME_KEY, ownerName);
    personalization.addDynamicTemplateData(EMPLOYEE_NAME_KEY, employeeName);
    Mail mail =
        initMailWithTemplate(
            sendGridProperties.getCardIssuedNotifyOwnerTemplateId(), to, personalization);
    send(mail);
  }

  /* Card : Virtual Card issued to Employee Notification */
  public void sendCardIssuedVirtualNotifyUserEmail(
      String to, String firstName, String companyName) {
    Personalization personalization = new Personalization();
    personalization.addDynamicTemplateData(FIRST_NAME_KEY, firstName);
    personalization.addDynamicTemplateData(COMPANY_NAME_KEY, companyName);
    Mail mail =
        initMailWithTemplate(
            sendGridProperties.getCardIssuedVirtualNotifyUserTemplateId(), to, personalization);
    send(mail);
  }

  /* Card : Physical Card issued to Employee Notification */
  public void sendCardIssuedPhysicalNotifyUserEmail(
      String to, String firstName, String companyName) {
    Personalization personalization = new Personalization();
    personalization.addDynamicTemplateData(FIRST_NAME_KEY, firstName);
    personalization.addDynamicTemplateData(COMPANY_NAME_KEY, companyName);
    Mail mail =
        initMailWithTemplate(
            sendGridProperties.getCardIssuedPhysicalNotifyUserTemplateId(), to, personalization);
    send(mail);
  }

  /* Card : Physical Card Shipped to Employee Notification */
  public void sendCardShippedNotifyUserEmail(String to, String firstName) {
    Personalization personalization = new Personalization();
    personalization.addDynamicTemplateData(FIRST_NAME_KEY, firstName);
    Mail mail =
        initMailWithTemplate(
            sendGridProperties.getCardShippedNotifyUserTemplateId(), to, personalization);
    send(mail);
  }

  /* Card : Physical Card Activation Email */
  public void sendCardStartActivationEmail(String to, String firstName) {
    Personalization personalization = new Personalization();
    personalization.addDynamicTemplateData(FIRST_NAME_KEY, firstName);
    Mail mail =
        initMailWithTemplate(
            sendGridProperties.getCardStartActivationTemplateId(), to, personalization);
    send(mail);
  }

  /* Card : Physical Card Activation Completed email */
  public void sendCardActivationCompletedEmail(String to, String firstName) {
    Personalization personalization = new Personalization();
    personalization.addDynamicTemplateData(FIRST_NAME_KEY, firstName);
    Mail mail =
        initMailWithTemplate(
            sendGridProperties.getCardActivationCompletedTemplateId(), to, personalization);
    send(mail);
  }

  /* Card : Card Freeze email */
  public void sendCardFrozenEmail(String to, String firstName, String lastFour) {
    Personalization personalization = new Personalization();
    personalization.addDynamicTemplateData(FIRST_NAME_KEY, firstName);
    personalization.addDynamicTemplateData(CARD_LAST_FOUR_KEY, lastFour);
    Mail mail =
        initMailWithTemplate(sendGridProperties.getCardFrozenTemplateId(), to, personalization);
    send(mail);
  }

  /* Card : Card Unfreeze email */
  public void sendCardUnfrozenEmail(String to, String firstName, String lastFour) {
    Personalization personalization = new Personalization();
    personalization.addDynamicTemplateData(FIRST_NAME_KEY, firstName);
    personalization.addDynamicTemplateData(CARD_LAST_FOUR_KEY, lastFour);
    Mail mail =
        initMailWithTemplate(sendGridProperties.getCardUnfrozenTemplateId(), to, personalization);
    send(mail);
  }

  /* Update : Employee Update Phone number or email or Address email */
  public void sendUserDetailsUpdatedEmail(String to, String firstName) {
    Personalization personalization = new Personalization();
    personalization.addDynamicTemplateData(FIRST_NAME_KEY, firstName);
    Mail mail =
        initMailWithTemplate(
            sendGridProperties.getUserDetailsUpdatedTemplateId(), to, personalization);
    send(mail);
  }

  /* Login: User Account Created Notification */
  public void sendUserAccountCreatedEmail(
      String to, String firstName, String companyName, String password) {
    Personalization personalization = new Personalization();
    personalization.addDynamicTemplateData(FIRST_NAME_KEY, firstName);
    personalization.addDynamicTemplateData(COMPANY_NAME_KEY, companyName);
    personalization.addDynamicTemplateData(PASSWORD_KEY, password);
    Mail mail =
        initMailWithTemplate(
            sendGridProperties.getUserAccountCreatedTemplateId(), to, personalization);
    send(mail);
  }

  /* Financial Account Ready */
  public void sendFinancialAccountReadyEmail(String to, String firstName) {
    Personalization personalization = new Personalization();
    personalization.addDynamicTemplateData(FIRST_NAME_KEY, firstName);
    Mail mail =
        initMailWithTemplate(
            sendGridProperties.getFinancialAccountReadyTemplateId(), to, personalization);
    send(mail);
  }
}
