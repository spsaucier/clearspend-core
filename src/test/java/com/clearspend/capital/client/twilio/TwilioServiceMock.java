package com.clearspend.capital.client.twilio;

import com.clearspend.capital.client.sendgrid.SendGridProperties;
import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.data.model.business.BusinessProspect;
import com.clearspend.capital.service.TwilioService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.twilio.rest.verify.v2.service.Verification;
import com.twilio.rest.verify.v2.service.VerificationCheck;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

  public static Map<String, List<String>> emails = new HashMap<>();

  @Setter @Getter private String lastChangePasswordId;
  @Setter @Getter private String lastVerificationEmail;
  @Setter @Getter private String lastVerificationPhone;
  @Setter @Getter private String lastOtp;
  @Setter @Getter private String lastUserAccountCreatedPassword;
  @Getter private final List<LastLowBalanceEmail> lastLowBalanceEmail = new ArrayList<>();
  @Setter @Getter private String lastCardUnlinkedEmail;

  public record LastLowBalanceEmail(String to, Amount amount) {}

  public TwilioServiceMock(
      TwilioProperties twilioProperties, SendGridProperties sendGridProperties) {
    super(twilioProperties, sendGridProperties);
  }

  @Override
  protected void sendBankDetailsRemovedEmail(
      String to,
      String firstName,
      String bankName,
      String accountOwnerName,
      String accountName,
      String accountLastFour) {}

  @Override
  protected void sendUserDetailsUpdatedEmail(String to, String firstName) {}

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
  public void sendKybKycPassEmail(String to, String firstName) {
    emails.put(to, List.of(firstName));
  }

  @Override
  public void sendKybKycFailEmail(String to, String firstName, List<String> reasons) {
    emails.put(to, List.of(firstName, reasons == null ? "" : String.join(",", reasons)));
  }

  @Override
  public void sendKybKycReviewStateEmail(String to, String firstName) {
    emails.put(to, List.of(firstName));
  }

  @Override
  public void sendKybKycRequireAdditionalInfoEmail(
      String to, String firstName, List<String> reasons) {
    emails.put(to, List.of(firstName, reasons == null ? "" : String.join(",", reasons)));
  }

  @Override
  public void sendKybKycRequireDocumentsEmail(
      String to, String firstName, List<String> requiredDocuments) {
    emails.put(to, List.of(firstName, requiredDocuments.toString()));
  }

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

  @Override
  public void sendPasswordResetSuccessEmail(String to, String firstName) {}

  @Override
  public void sendBankDetailsAddedEmail(
      String to,
      String firstName,
      String bankName,
      String accountOwnerName,
      String accountName,
      String accountLastFour) {}

  @Override
  public void sendBankFundsAvailableEmail(String to, String firstName) {
    emails.put(to, List.of(firstName));
  }

  @Override
  public void sendBankFundsReturnEmail(String to, String firstName) {}

  @Override
  protected void sendLowBalanceEmail(
      final String to,
      final String firstName,
      final String allocationName,
      final Amount lowBalanceLevel) {
    this.lastLowBalanceEmail.add(new LastLowBalanceEmail(to, lowBalanceLevel));
  }

  @Override
  public void sendBankFundsWithdrawalEmail(
      String to,
      String firstName,
      String bankName,
      String amount,
      String accountOwnerName,
      String accountName,
      String accountLastFour) {}

  @Override
  public void sendCardIssuedNotifyOwnerEmail(String to, String ownerName, String employeeName) {}

  @Override
  public void sendCardIssuedVirtualNotifyUserEmail(
      String to, String firstName, String companyName) {}

  @Override
  public void sendCardIssuedPhysicalNotifyUserEmail(
      String to, String firstName, String companyName) {}

  @Override
  public void sendCardShippedNotifyUserEmail(String to, String firstName) {}

  @Override
  public void sendCardActivationCompletedEmail(String to, String firstName) {}

  @Override
  public void sendCardFrozenEmail(String to, String firstName, String lastFour) {}

  @Override
  public void sendCardUnfrozenEmail(String to, String firstName, String lastFour) {}

  @Override
  protected void sendCardCancelledEmail(String to, String firstName, String lastFour) {}

  @Override
  protected void sendCardUnlinkedEmail(
      String to, String firstName, String lastFour, String allocationName) {
    this.lastCardUnlinkedEmail = to;
  }

  @Override
  public void sendUserAccountCreatedEmail(
      String to, String firstName, String companyName, String password) {
    lastUserAccountCreatedPassword = password;
  }

  @Override
  public void sendBankFundsDepositRequestEmail(
      String to,
      String firstName,
      String bankName,
      String amount,
      String accountOwnerName,
      String accountName,
      String accountLastFour) {}

  @Override
  public void sendFinancialAccountReadyEmail(String to, String firstName) {}
}
