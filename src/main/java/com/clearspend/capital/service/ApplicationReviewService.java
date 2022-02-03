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
import com.clearspend.capital.data.model.enums.KnowYourCustomerStatus;
import com.clearspend.capital.service.type.CurrentUser;
import com.stripe.exception.StripeException;
import com.stripe.model.Account;
import com.stripe.model.File;
import com.stripe.model.Person;
import com.stripe.param.AccountUpdateParams;
import com.stripe.param.AccountUpdateParams.Company;
import com.stripe.param.FileCreateParams.Purpose;
import com.stripe.param.PersonUpdateParams;
import com.stripe.param.PersonUpdateParams.Verification;
import com.stripe.param.PersonUpdateParams.Verification.Document;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApplicationReviewService {

  public static final String DEFAULT_FILE_RESOURCE_NAME = "file";
  public static final String STRIPE_ACCOUNT_PREFIX = "acct";
  public static final String DOCUMENT = "document";
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
  public void uploadStripeRequiredDocuments(List<MultipartFile> files) {
    Business business = businessService.getBusiness(CurrentUser.get().businessId()).business();

    for (MultipartFile multipartFile : files) {
      String[] strings = Objects.requireNonNull(multipartFile.getOriginalFilename()).split("\\|");
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
            multipartFile);
        uploadDocumentToStripeForAccount(business, multipartFile, documentType);
      } else {
        BusinessOwner businessOwner =
            businessOwnerService.findBusinessOwnerByStripePersonReference(entityToken);
        fileStoreService.saveFileForBusinessOwner(
            business.getId(),
            businessOwner.getId(),
            fileName,
            Purpose.valueOf(documentType.toString()).getValue(),
            multipartFile);
        uploadDocumentToStripeForPerson(business, multipartFile, entityToken);
      }
    }

    Account account = stripeClient.retrieveAccount(business.getStripeAccountReference());
    businessService.updateBusinessAccordingToStripeAccountRequirements(business, account);
  }

  private void uploadDocumentToStripeForPerson(
      Business business, MultipartFile multipartFile, String entityToken) throws StripeException {
    Person person = stripeClient.retrievePerson(entityToken, business.getStripeAccountReference());

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

    Account account = stripeClient.retrieveAccount(business.getStripeAccountReference());
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

    Business business = businessService.retrieveBusiness(businessId);
    String stripeAccountReference = business.getStripeAccountReference();
    Account account = stripeClient.retrieveAccount(stripeAccountReference);
    List<Person> personList =
        businessOwnerService.findBusinessOwnerByBusinessId(businessId).stream()
            .map(
                businessOwner -> {
                  String stripePersonReference =
                      businessOwnerService
                          .retrieveBusinessOwner(businessOwner.getId())
                          .getStripePersonReference();
                  return stripeClient.retrievePerson(stripePersonReference, stripeAccountReference);
                })
            .toList();

    List<KycOwnerDocuments> kycDocuments = extractStripeRequiredDocumentsForPerson(personList);
    List<KybErrorCode> kybErrorCodeList = extractStripeRequiredDocumentsForAccount(account);

    businessService.updateBusinessAccordingToStripeAccountRequirements(business, account);

    KybEntityTokenAndErrorCode kybEntityTokenAndErrorCode =
        new KybEntityTokenAndErrorCode(account.getId(), kybErrorCodeList);
    return new ApplicationReviewRequirements(
        new RequiredDocumentsForStripe(kybEntityTokenAndErrorCode, kycDocuments));
  }

  private List<KybErrorCode> extractStripeRequiredDocumentsForAccount(Account account) {
    List<KybErrorCode> kybErrorCodeList = new ArrayList<>();
    if (account.getRequirements() != null) {
      Set<String> accountRequiredFields = new HashSet<>();
      accountRequiredFields.addAll(account.getRequirements().getCurrentlyDue());
      accountRequiredFields.addAll(account.getRequirements().getEventuallyDue());
      accountRequiredFields.addAll(account.getRequirements().getPastDue());
      accountRequiredFields.addAll(account.getRequirements().getPendingVerification());
      accountRequiredFields.forEach(
          accountFieldRequired -> {
            if (accountFieldRequired.endsWith(DOCUMENT)
                && !accountFieldRequired.startsWith("person")) {
              kybErrorCodeList.add(KybErrorCode.COMPANY_VERIFICATION_DOCUMENT);
            }
          });
    }
    return kybErrorCodeList;
  }

  private List<KycOwnerDocuments> extractStripeRequiredDocumentsForPerson(List<Person> personList) {
    return personList.stream()
        .filter(person -> person.getRequirements() != null)
        .map(
            person -> {
              Person.Requirements personRequirements = person.getRequirements();
              if (personRequirements.getCurrentlyDue().isEmpty()
                  && personRequirements.getPastDue().isEmpty()
                  && personRequirements.getEventuallyDue().isEmpty()
                  && personRequirements.getPendingVerification().isEmpty()
                  && personRequirements.getErrors().isEmpty()) {
                businessOwnerService.updateBusinessOwnerStatusByStripePersonReference(
                    person.getId(), KnowYourCustomerStatus.PASS);
                return null;
              }
              Set<String> personRequiredFields = new HashSet<>();
              personRequiredFields.addAll(personRequirements.getCurrentlyDue());
              personRequiredFields.addAll(personRequirements.getPastDue());
              personRequiredFields.addAll(personRequirements.getEventuallyDue());
              personRequiredFields.addAll(personRequirements.getPendingVerification());
              return personRequiredFields.stream()
                  .filter(s1 -> s1.endsWith(DOCUMENT) && s1.startsWith("person"))
                  .map(
                      s1 ->
                          new KycOwnerDocuments(
                              person.getEmail(),
                              person.getId(),
                              List.of(KycErrorCode.NAME_NOT_VERIFIED)))
                  .toList();
            })
        .filter(Objects::nonNull)
        .flatMap(Collection::stream)
        .toList();
  }
}
