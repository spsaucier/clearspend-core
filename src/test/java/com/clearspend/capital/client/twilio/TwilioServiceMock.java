package com.clearspend.capital.client.twilio;

import com.clearspend.capital.client.sendgrid.SendGridProperties;
import com.clearspend.capital.data.model.business.BusinessProspect;
import com.clearspend.capital.service.TwilioService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.twilio.rest.verify.v2.service.Verification;
import com.twilio.rest.verify.v2.service.VerificationCheck;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Service;

@Service("twilioService")
public class TwilioServiceMock extends TwilioService {

  private final ObjectMapper objectMapper = new ObjectMapper();
  private final Verification verification =
      Verification.fromJson("{\"status\": \"pending\"}", objectMapper);
  private final VerificationCheck verificationCheck =
      VerificationCheck.fromJson("{\"valid\": \"true\"}", objectMapper);

  @Setter @Getter private String lastChangePasswordId;
  @Setter @Getter private String lastVerificationEmail;
  @Setter @Getter private String lastVerificationPhone;
  @Setter @Getter private String lastOtp;

  public TwilioServiceMock(
      TwilioProperties twilioProperties, SendGridProperties sendGridProperties) {
    super(twilioProperties, sendGridProperties);
  }

  @Override
  protected void initTwilio() {}

  @Override
  protected void destroyTwilio() {}

  @Override
  public void sendResetPasswordEmail(String to, String changePasswordId) {
    this.lastChangePasswordId = changePasswordId;
  }

  @Override
  public void sendNotificationEmail(String to, String messageText) {}

  @Override
  public void sendOnboardingWelcomeEmail(String to, BusinessProspect businessProspect) {}

  @Override
  public void sendKybKycPassEmail(String to, String firstName) {}

  @Override
  public void sendKybKycFailEmail(String to, String firstName, List<String> reasons) {}

  @Override
  public Verification sendVerificationSms(String to) {
    lastVerificationPhone = to;
    return verification;
  }

  @Override
  public Verification sendVerificationEmail(String to, BusinessProspect businessProspect) {
    lastVerificationEmail = to;
    return verification;
  }

  @Override
  public VerificationCheck checkVerification(String subject, String challenge) {
    lastOtp = challenge;

    return verificationCheck;
  }
}
