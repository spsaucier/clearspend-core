package com.clearspend.capital.service;

import com.clearspend.capital.client.stripe.StripeClient;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.common.typedid.data.business.BusinessOwnerId;
import com.clearspend.capital.controller.type.review.ApplicationReviewRequirements;
import com.clearspend.capital.controller.type.review.DocumentType;
import com.clearspend.capital.controller.type.review.KybErrorCode;
import com.clearspend.capital.controller.type.review.KycErrorCode;
import com.clearspend.capital.data.model.business.Business;
import com.clearspend.capital.data.model.business.BusinessOwner;
import com.clearspend.capital.data.model.enums.BusinessType;
import com.clearspend.capital.service.type.CurrentUser;
import com.clearspend.capital.service.type.StripeAccountFieldsToClearspendBusinessFields;
import com.clearspend.capital.service.type.StripePersonFieldsToClearspendOwnerFields;
import com.google.common.base.Splitter;
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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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
  public static final String COMPANY_OWNERS_PROVIDED = "company.owners_provided";
  public static final String BUSINESS_PROFILE = "business_profile";
  public static final String INDIVIDUAL = "individual";
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
      List<String> strings = Splitter.on("|").splitToList(originalFileName);

      DocumentType documentType = DocumentType.valueOf(strings.get(1));
      String entityToken = strings.get(0);
      String fileName = strings.get(2);

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
    businessService.updateBusinessAccordingToStripeAccountRequirements(business, account);

    List<KycOwnerDocuments> kycDocuments =
        extractStripeRequiredDocumentsForPerson(account, businessOwners);
    List<KybErrorCode> kybErrorCodeList =
        extractStripeRequiredDocumentsForAccount(account).stream().toList();

    KybEntityTokenAndErrorCode kybEntityTokenAndErrorCode =
        new KybEntityTokenAndErrorCode(account.getId(), kybErrorCodeList);

    Map<TypedId<BusinessOwnerId>, List<String>> kycRequirements =
        extractStripeRequirementsForPersons(account);
    List<String> kybRequirements = extractStripeRequirementsForAccount(account);

    return ApplicationReviewRequirements.from(
        kybRequirements,
        kycRequirements,
        new RequiredDocumentsForStripe(kybEntityTokenAndErrorCode, kycDocuments),
        business.getType() == BusinessType.INDIVIDUAL
            ? requiredRelationShipToBusiness(account, INDIVIDUAL)
            : requiredRelationShipToBusiness(account, OWNERS),
        requiredRelationShipToBusiness(account, REPRESENTATIVE));
  }

  private Set<KybErrorCode> extractStripeRequiredDocumentsForAccount(Account account) {

    Set<String> accountRequiredFields = getAccountRequiredFields(account);

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

    Set<String> accountRequiredFields = getAccountRequiredFields(account);

    return accountRequiredFields.stream()
        .filter(
            accountRequiredField ->
                accountRequiredField.startsWith(PERSON) && accountRequiredField.endsWith(DOCUMENT))
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
                  List.of(KycErrorCode.IDENTITY_DOCUMENT));
            })
        .filter(Objects::nonNull)
        .toList();
  }

  private List<String> extractStripeRequirementsForAccount(Account account) {
    return getAccountRequiredFields(account).stream()
        .filter(
            accountFieldRequired ->
                (!accountFieldRequired.endsWith(DOCUMENT)
                    && !accountFieldRequired.startsWith(PERSON)
                    && !accountFieldRequired.startsWith(OWNERS)
                    && !accountFieldRequired.startsWith(REPRESENTATIVE)
                    && !accountFieldRequired.startsWith(COMPANY_OWNERS_PROVIDED)
                    && (accountFieldRequired.startsWith(BUSINESS_PROFILE)
                        || accountFieldRequired.startsWith(COMPANY))
                    && !accountFieldRequired.equals(EXTERNAL_ACCOUNT_CODE_REQUIREMENT)))
        .map(
            field ->
                StripeAccountFieldsToClearspendBusinessFields.fromStripeField(
                    field.substring(field.indexOf(".") + 1)))
        .toList();
  }

  private Set<String> getAccountRequiredFields(Account account) {
    Requirements accountRequirements = account.getRequirements();
    if (accountRequirements == null) {
      return Collections.emptySet();
    }
    return Stream.of(
            accountRequirements.getCurrentlyDue(),
            accountRequirements.getEventuallyDue(),
            accountRequirements.getPastDue(),
            accountRequirements.getPendingVerification())
        .flatMap(Collection::stream)
        .collect(Collectors.toSet());
  }

  private Boolean requiredRelationShipToBusiness(Account account, final String mark) {
    Set<String> accountRequiredFields = getAccountRequiredFields(account);

    return accountRequiredFields.stream()
        .anyMatch(accountFieldRequired -> accountFieldRequired.startsWith(mark));
  }

  private Map<TypedId<BusinessOwnerId>, List<String>> extractStripeRequirementsForPersons(
      Account account) {
    Map<String, TypedId<BusinessOwnerId>> businessOwnerIdByStripeReference = new HashMap<>();
    return getAccountRequiredFields(account).stream()
        .filter(
            accountRequiredField ->
                accountRequiredField.startsWith(PERSON) && !accountRequiredField.endsWith(DOCUMENT))
        .collect(
            Collectors.groupingBy(
                s ->
                    businessOwnerIdByStripeReference.computeIfAbsent(
                        Splitter.on(".").splitToList(s).get(0),
                        stripeReferenceId ->
                            businessOwnerService
                                .findBusinessOwnerByStripePersonReference(stripeReferenceId)
                                .getId()),
                Collectors.mapping(
                    s ->
                        StripePersonFieldsToClearspendOwnerFields.fromStripeField(
                            s.substring(s.indexOf(".") + 1)),
                    Collectors.toList())));
  }
}
