package com.clearspend.capital.service;

import com.clearspend.capital.client.stripe.StripeClient;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.controller.type.review.ApplicationReviewRequirements;
import com.clearspend.capital.controller.type.review.DocumentType;
import com.clearspend.capital.controller.type.review.KybErrorCode;
import com.clearspend.capital.controller.type.review.KycErrorCode;
import com.clearspend.capital.data.model.business.Business;
import com.clearspend.capital.data.model.business.BusinessOwner;
import com.clearspend.capital.service.type.CurrentUser;
import com.stripe.exception.StripeException;
import com.stripe.model.Account;
import com.stripe.model.Account.Requirements;
import com.stripe.model.File;
import com.stripe.model.Person;
import com.stripe.param.AccountUpdateParams;
import com.stripe.param.AccountUpdateParams.Company;
import com.stripe.param.FileCreateParams.Purpose;
import com.stripe.param.PersonUpdateParams;
import com.stripe.param.PersonUpdateParams.Verification;
import com.stripe.param.PersonUpdateParams.Verification.Document;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApplicationReviewService {

  public static final String DEFAULT_FILE_RESOURCE_NAME = "file";
  public static final String STRIPE_ACCOUNT_PREFIX = "acct";
  public static final String DOCUMENT = "document";
  public static final String PERSON = "person";
  public static final String EXTERNAL_ACCOUNT_CODE_REQUIREMENT = "external_account";
  public static final String REPRESENTATIVE = "representative";
  public static final String OWNERS = "owners";
  public static final String COMPANY = "company";
  private final BusinessService businessService;
  private final BusinessOwnerService businessOwnerService;
  private final FileStoreService fileStoreService;

  private final StripeClient stripeClient;

  public record KycOwnerDocuments(
      String owner, String entityTokenId, List<KycErrorCode> kycErrorCodes) {}

  public record KybEntityTokenAndErrorCode(
      String entityTokenId, List<KybErrorCode> kybErrorCodeList) {}

  public record RequiredDocumentsForStripe(
      KybEntityTokenAndErrorCode kybDocuments, List<KycOwnerDocuments> kycDocuments) {}

  @SneakyThrows
  @Transactional
  public void uploadStripeRequiredDocuments(List<MultipartFile> files) {
    Business business = businessService.getBusiness(CurrentUser.get().businessId()).business();

    for (MultipartFile multipartFile : files) {
      String originalFileName =
          Objects.requireNonNull(
              multipartFile.getOriginalFilename(), "Required values for original file name");
      String[] strings = originalFileName.split("\\|");

      DocumentType documentType = DocumentType.valueOf(strings[1]);
      String entityToken = strings[0];
      String fileName = strings[2];

      MultipartBodyBuilder builder = new MultipartBodyBuilder();
      builder.part(DEFAULT_FILE_RESOURCE_NAME, multipartFile.getResource());

      if (entityToken.startsWith(STRIPE_ACCOUNT_PREFIX)) {
        fileStoreService.saveFileForBusiness(
            business.getId(),
            fileName,
            Purpose.valueOf(documentType.toString()).getValue(),
            multipartFile.getBytes());
        uploadDocumentToStripeForAccount(business, multipartFile, documentType);
      } else {
        BusinessOwner businessOwner =
            businessOwnerService.findBusinessOwnerByStripePersonReference(entityToken);
        fileStoreService.saveFileForBusinessOwner(
            business.getId(),
            businessOwner.getId(),
            fileName,
            Purpose.valueOf(documentType.toString()).getValue(),
            multipartFile.getBytes());
        uploadDocumentToStripeForPerson(business, multipartFile, entityToken);
      }
    }

    Account account = stripeClient.retrieveAccount(business.getStripeData().getAccountRef());
    businessService.updateBusinessAccordingToStripeAccountRequirements(business, account);
  }

  private void uploadDocumentToStripeForPerson(
      Business business, MultipartFile multipartFile, String entityToken) throws StripeException {
    Person person =
        stripeClient.retrievePerson(entityToken, business.getStripeData().getAccountRef());

    File file = stripeClient.uploadFile(multipartFile, Purpose.IDENTITY_DOCUMENT);

    person.update(
        PersonUpdateParams.builder()
            .setVerification(
                Verification.builder()
                    .setDocument(Document.builder().setFront(file.getId()).build())
                    .build())
            .build());
  }

  private void uploadDocumentToStripeForAccount(
      Business business, MultipartFile multipartFile, DocumentType documentType)
      throws StripeException {

    Account account = stripeClient.retrieveAccount(business.getStripeData().getAccountRef());
    File file = stripeClient.uploadFile(multipartFile, Purpose.valueOf(documentType.toString()));
    account.update(
        AccountUpdateParams.builder()
            .setCompany(
                Company.builder()
                    .setVerification(
                        Company.Verification.builder()
                            .setDocument(
                                Company.Verification.Document.builder()
                                    .setFront(file.getId())
                                    .build())
                            .build())
                    .build())
            .build());
  }

  public ApplicationReviewRequirements getStripeApplicationRequirements(
      TypedId<BusinessId> businessId) {

    Business business = businessService.retrieveBusiness(businessId, true);
    String stripeAccountReference = business.getStripeData().getAccountRef();
    Account account = stripeClient.retrieveAccount(stripeAccountReference);
    List<BusinessOwner> businessOwners =
        businessOwnerService.findBusinessOwnerByBusinessId(businessId);

    List<KycOwnerDocuments> kycDocuments =
        extractStripeRequiredDocumentsForPerson(account, businessOwners);
    List<KybErrorCode> kybErrorCodeList =
        extractStripeRequiredDocumentsForAccount(account).stream().toList();

    businessService.updateBusinessAccordingToStripeAccountRequirements(business, account);

    KybEntityTokenAndErrorCode kybEntityTokenAndErrorCode =
        new KybEntityTokenAndErrorCode(account.getId(), kybErrorCodeList);

    ApplicationReviewRequirements applicationReviewRequirements =
        new ApplicationReviewRequirements(
            new RequiredDocumentsForStripe(kybEntityTokenAndErrorCode, kycDocuments));

    List<String> kycRequirements = extractStripeRequirementsForPersons(account);
    List<String> kybRequirements = extractStripeRequirementsForAccount(account);

    applicationReviewRequirements.setKybRequiredFields(kybRequirements);
    applicationReviewRequirements.setKycRequiredFields(kycRequirements);

    return applicationReviewRequirements;
  }

  private Set<KybErrorCode> extractStripeRequiredDocumentsForAccount(Account account) {
    if (account.getRequirements() == null) {
      return new HashSet<>();
    }
    Set<String> accountRequiredFields = new HashSet<>();
    accountRequiredFields.addAll(account.getRequirements().getCurrentlyDue());
    accountRequiredFields.addAll(account.getRequirements().getEventuallyDue());
    accountRequiredFields.addAll(account.getRequirements().getPastDue());
    accountRequiredFields.addAll(account.getRequirements().getPendingVerification());

    return accountRequiredFields.stream()
        .filter(
            accountFieldRequired ->
                accountFieldRequired.endsWith(DOCUMENT)
                    && !accountFieldRequired.startsWith(PERSON)
                    && !accountFieldRequired.startsWith(OWNERS)
                    && !accountFieldRequired.startsWith(REPRESENTATIVE))
        .map(accountRequiredField -> KybErrorCode.COMPANY_VERIFICATION_DOCUMENT)
        .collect(Collectors.toSet());
  }

  private List<KycOwnerDocuments> extractStripeRequiredDocumentsForPerson(
      Account account, List<BusinessOwner> businessOwners) {

    Requirements accountRequirements = account.getRequirements();
    if (accountRequirements == null) {
      return Collections.emptyList();
    }
    Set<String> accountRequiredFields = new HashSet<>();
    accountRequiredFields.addAll(accountRequirements.getCurrentlyDue());
    accountRequiredFields.addAll(accountRequirements.getEventuallyDue());
    accountRequiredFields.addAll(accountRequirements.getPastDue());
    accountRequiredFields.addAll(accountRequirements.getPendingVerification());

    return accountRequiredFields.stream()
        .filter(
            accountRequiredField ->
                accountRequiredField.startsWith(PERSON) && accountRequiredField.endsWith(DOCUMENT))
        // TODO:gb: what are all the correct document type required by Stripe for account
        .map(
            accountRequiredField -> {
              String entityTokenId = accountRequiredField.split("\\.")[0];
              Optional<BusinessOwner> first =
                  businessOwners.stream()
                      .filter(
                          businessOwner ->
                              entityTokenId.equals(businessOwner.getStripePersonReference()))
                      .findFirst();
              if (first.isEmpty()) {
                return null;
              }
              return new KycOwnerDocuments(
                  first.get().getFirstName() + " " + first.get().getLastName(),
                  entityTokenId,
                  List.of(KycErrorCode.NAME_NOT_VERIFIED));
            })
        .filter(Objects::nonNull)
        .toList();
  }

  private List<String> extractStripeRequirementsForAccount(Account account) {
    List<String> kybRequirements = new ArrayList<>();
    if (account.getRequirements() != null) {
      Set<String> accountRequiredFields = new HashSet<>();
      accountRequiredFields.addAll(account.getRequirements().getCurrentlyDue());
      accountRequiredFields.addAll(account.getRequirements().getEventuallyDue());
      accountRequiredFields.addAll(account.getRequirements().getPastDue());
      accountRequiredFields.addAll(account.getRequirements().getPendingVerification());
      accountRequiredFields.forEach(
          accountFieldRequired -> {
            if (!accountFieldRequired.endsWith(DOCUMENT)
                && !accountFieldRequired.startsWith(PERSON)
                && !accountFieldRequired.equals(EXTERNAL_ACCOUNT_CODE_REQUIREMENT)) {
              kybRequirements.add(accountFieldRequired);
            }
          });
    }

    return kybRequirements;
  }

  private List<String> extractStripeRequirementsForPersons(Account account) {

    Requirements accountRequirements = account.getRequirements();
    if (accountRequirements == null) {
      return Collections.emptyList();
    }
    Set<String> accountRequiredFields = new HashSet<>();
    accountRequiredFields.addAll(accountRequirements.getCurrentlyDue());
    accountRequiredFields.addAll(accountRequirements.getEventuallyDue());
    accountRequiredFields.addAll(accountRequirements.getPastDue());
    accountRequiredFields.addAll(accountRequirements.getPendingVerification());

    return accountRequiredFields.stream()
        .filter(
            accountRequiredField ->
                accountRequiredField.startsWith(PERSON) && !accountRequiredField.endsWith(DOCUMENT))
        // TODO:gb: what are all the correct document type required by Stripe for account
        .toList();
  }
}
