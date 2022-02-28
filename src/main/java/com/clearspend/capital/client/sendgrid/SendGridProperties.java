package com.clearspend.capital.client.sendgrid;

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

  @NotBlank private String envURL;

  @NotBlank private String notificationsSenderEmail;

  @NotBlank private String notificationsEmailSubject;

  @NotBlank private String onboardingWelcomeEmailTemplateId;

  @NotBlank private String kybKycPassEmailTemplateId;

  @NotBlank private String kybKycFailEmailTemplateId;

  @NotBlank private String kybKycReviewStateTemplateId;

  @NotBlank private String kybKycRequireAdditionalInfoTemplateId;

  @NotBlank private String kybKycRequireDocTemplateId;

  @NotBlank private String forgotPasswordEmailTemplateId;

  @NotBlank private String passwordResetSuccessTemplateId;

  @NotBlank private String welcomeInviteOnlyTemplateId;

  @NotBlank private String kybKycDocsReceivedTemplateId;

  @NotBlank private String bankDetailsAddedTemplateId;

  @NotBlank private String bankFundsAvailableTemplateId;

  @NotBlank private String bankDetailsRemovedTemplateId;

  @NotBlank private String bankFundsReturnTemplateId;

  @NotBlank private String bankFundsWithdrawalTemplateId;

  @NotBlank private String cardIssuedNotifyOwnerTemplateId;

  @NotBlank private String cardIssuedVirtualNotifyUserTemplateId;

  @NotBlank private String cardIssuedPhysicalNotifyUserTemplateId;

  @NotBlank private String cardShippedNotifyUserTemplateId;

  @NotBlank private String cardStartActivationTemplateId;

  @NotBlank private String cardActivationCompletedTemplateId;

  @NotBlank private String cardFrozenTemplateId;

  @NotBlank private String cardUnfrozenTemplateId;

  @NotBlank private String userDetailsUpdatedTemplateId;

  @NotBlank private String userAccountCreatedTemplateId;

  private boolean emailNotificationsEnabled;

  @NotBlank private String bankFundsDepositRequestTemplateId;
}
