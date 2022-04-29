package com.clearspend.capital.service;

import com.clearspend.capital.client.stripe.StripeClient;
import com.clearspend.capital.common.data.model.TypedMutable;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.common.typedid.data.business.BusinessOwnerId;
import com.clearspend.capital.controller.type.review.ApplicationReviewRequirements;
import com.clearspend.capital.controller.type.review.DocumentType;
import com.clearspend.capital.controller.type.review.KybErrorCode;
import com.clearspend.capital.controller.type.review.KycErrorCode;
import com.clearspend.capital.controller.type.review.StripeRequirementsErrorCode;
import com.clearspend.capital.data.model.business.Business;
import com.clearspend.capital.data.model.business.BusinessOwner;
import com.clearspend.capital.data.model.business.StripeRequirements;
import com.clearspend.capital.data.model.enums.BusinessType;
import com.clearspend.capital.data.repository.business.StripeRequirementsRepository;
import com.clearspend.capital.service.type.StripeAccountFieldsToClearspendBusinessFields;
import com.clearspend.capital.service.type.StripePersonFieldsToClearspendOwnerFields;
import com.google.common.base.Splitter;
import com.stripe.model.Account.Requirements;
import com.stripe.model.File;
import com.stripe.model.Person;
import com.stripe.param.AccountUpdateParams.Company.Verification;
import com.stripe.param.FileCreateParams.Purpose;
import com.stripe.param.PersonUpdateParams.Verification.Document;
import java.util.ArrayList;
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
import org.springframework.security.access.prepost.PreAuthorize;
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
  public static final String COMPANY_TAX_ID = "company.tax_id";
  private final RetrievalService retrievalService;
  private final BusinessOwnerService businessOwnerService;
  private final FileStoreService fileStoreService;
  private final StripeRequirementsRepository stripeRequirementsRepository;

  private final StripeClient stripeClient;

  public record KycOwnerDocuments(
      String owner, String entityTokenId, List<KycErrorCode> kycErrorCodes) {}

  public record KybEntityTokenAndErrorCode(
      String entityTokenId, List<KybErrorCode> kybErrorCodeList) {}

  public record RequiredDocumentsForStripe(
      KybEntityTokenAndErrorCode kybDocuments, List<KycOwnerDocuments> kycDocuments) {}

  public record StripeSavedFile(Boolean front, Boolean back, File file) {}

  @SneakyThrows
  @Transactional
  @PreAuthorize("hasRootPermission(#businessId, 'LINK_BANK_ACCOUNTS|CUSTOMER_SERVICE')")
  public void uploadStripeRequiredDocuments(
      final TypedId<BusinessId> businessId, final List<MultipartFile> files) {
    Business business = retrievalService.retrieveBusiness(businessId, true);
    Map<String, List<StripeSavedFile>> stripeSavedFilesForPerson = new HashMap<>();
    for (MultipartFile multipartFile : files) {
      String originalFileName =
          Objects.requireNonNull(
              multipartFile.getOriginalFilename(), "Required values for original file name");
      log.info("Uploading file name {}", originalFileName);
      List<String> strings = Splitter.on("|").splitToList(originalFileName);

      DocumentType documentType = DocumentType.valueOf(strings.get(1));
      String entityToken = strings.get(0);
      String fileName = strings.get(2);

      MultipartBodyBuilder builder = new MultipartBodyBuilder();
      builder.part(DEFAULT_FILE_RESOURCE_NAME, multipartFile.getResource());

      if (entityToken.startsWith(STRIPE_ACCOUNT_PREFIX)) {
        File file =
            stripeClient.uploadFile(
                multipartFile,
                Purpose.valueOf(documentType.toString()),
                business.getStripeData().getAccountRef());
        fileStoreService.saveFileForBusiness(
            business.getId(),
            file.getId(),
            fileName,
            Purpose.valueOf(documentType.toString()).getValue(),
            multipartFile.getBytes());

        Verification.Document document =
            Verification.Document.builder().setFront(file.getId()).build();
        stripeClient.updateAccountDocument(business.getStripeData().getAccountRef(), document);
      } else {
        BusinessOwner businessOwner =
            businessOwnerService.findBusinessOwnerByStripePersonReference(entityToken);
        File file =
            stripeClient.uploadFile(
                multipartFile, Purpose.IDENTITY_DOCUMENT, business.getStripeData().getAccountRef());
        fileStoreService.saveFileForBusinessOwner(
            business.getId(),
            businessOwner.getId(),
            file.getId(),
            fileName,
            documentType.toString(),
            multipartFile.getBytes());
        StripeSavedFile stripeSavedFile =
            new StripeSavedFile(
                documentType == DocumentType.IDENTITY_DOCUMENT_FRONT,
                documentType == DocumentType.IDENTITY_DOCUMENT_BACK,
                file);
        if (stripeSavedFilesForPerson.containsKey(entityToken)) {
          List<StripeSavedFile> stripeSavedFiles = stripeSavedFilesForPerson.get(entityToken);
          stripeSavedFiles.add(stripeSavedFile);
        } else {
          List<StripeSavedFile> stripeSavedFiles = new ArrayList<>();
          stripeSavedFiles.add(stripeSavedFile);
          stripeSavedFilesForPerson.put(entityToken, stripeSavedFiles);
        }
      }
    }

    stripeSavedFilesForPerson.forEach(
        (entityToken, stripeSavedFiles) -> {
          Person person = new Person();
          person.setId(entityToken);
          person.setAccount(business.getStripeData().getAccountRef());

          Document.Builder documentBuilder = Document.builder();
          stripeSavedFiles.forEach(
              stripeSavedFile -> {
                if (Boolean.TRUE.equals(stripeSavedFile.front)) {
                  documentBuilder.setFront(stripeSavedFile.file.getId());
                }
                if (Boolean.TRUE.equals(stripeSavedFile.back)) {
                  documentBuilder.setBack(stripeSavedFile.file.getId());
                }
              });

          stripeClient.updatePersonDocuments(
              entityToken, business.getStripeData().getAccountRef(), documentBuilder.build());
        });
  }

  @Transactional
  @PreAuthorize("hasRootPermission(#businessId, 'LINK_BANK_ACCOUNTS|CUSTOMER_SERVICE')")
  public ApplicationReviewRequirements getStripeApplicationRequirements(
      TypedId<BusinessId> businessId) {

    Business business = retrievalService.retrieveBusiness(businessId, true);
    Requirements requirements =
        stripeRequirementsRepository
            .findByBusinessId(businessId)
            .orElse(new StripeRequirements())
            .getRequirements();
    List<BusinessOwner> businessOwners =
        businessOwnerService.findBusinessOwnerByBusinessId(business.getId());

    return getReviewRequirements(business, businessOwners, requirements);
  }

  @PreAuthorize("hasRootPermission(#business, 'LINK_BANK_ACCOUNTS|CUSTOMER_SERVICE')")
  public ApplicationReviewRequirements getReviewRequirements(
      Business business, List<BusinessOwner> businessOwners, Requirements requirements) {
    List<KycOwnerDocuments> kycDocuments =
        extractStripeRequiredDocumentsForPerson(businessOwners, requirements);
    List<KybErrorCode> kybErrorCodeList =
        extractStripeRequiredDocumentsForAccount(requirements).stream().toList();

    KybEntityTokenAndErrorCode kybEntityTokenAndErrorCode =
        new KybEntityTokenAndErrorCode(business.getStripeData().getAccountRef(), kybErrorCodeList);

    Map<TypedId<BusinessOwnerId>, List<String>> kycRequirements =
        extractStripeRequirementsForPersons(requirements, businessOwners);
    List<String> kybRequirements = extractStripeRequirementsForAccount(requirements);

    return ApplicationReviewRequirements.from(
        kybRequirements,
        kycRequirements,
        new RequiredDocumentsForStripe(kybEntityTokenAndErrorCode, kycDocuments),
        business.getType() == BusinessType.INDIVIDUAL
            ? requiredRelationShipToBusiness(requirements, INDIVIDUAL)
            : requiredRelationShipToBusiness(requirements, OWNERS),
        requiredRelationShipToBusiness(requirements, REPRESENTATIVE),
        retrievePendingVerification(requirements),
        retrieveErrorCodes(requirements));
  }

  private Set<KybErrorCode> extractStripeRequiredDocumentsForAccount(Requirements requirements) {

    Set<String> accountRequiredFields = getAccountRequiredFields(requirements);

    return accountRequiredFields.stream()
        .filter(
            accountFieldRequired ->
                (accountFieldRequired.endsWith(DOCUMENT)
                        || accountFieldRequired.startsWith(COMPANY_TAX_ID))
                    && !accountFieldRequired.startsWith(PERSON)
                    && !accountFieldRequired.startsWith(OWNERS)
                    && !accountFieldRequired.startsWith(REPRESENTATIVE))
        .map(accountRequiredField -> KybErrorCode.COMPANY_VERIFICATION_DOCUMENT)
        .collect(Collectors.toSet());
  }

  private List<KycOwnerDocuments> extractStripeRequiredDocumentsForPerson(
      List<BusinessOwner> businessOwners, Requirements requirements) {

    Set<String> accountRequiredFields = getAccountRequiredFields(requirements);

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
                  List.of(
                      KycErrorCode.IDENTITY_DOCUMENT_FRONT, KycErrorCode.IDENTITY_DOCUMENT_BACK));
            })
        .filter(Objects::nonNull)
        .toList();
  }

  private List<String> extractStripeRequirementsForAccount(Requirements requirements) {
    return getAccountRequiredFields(requirements).stream()
        .filter(
            accountFieldRequired ->
                (!accountFieldRequired.endsWith(DOCUMENT)
                    && !accountFieldRequired.startsWith(PERSON)
                    && !accountFieldRequired.startsWith(OWNERS)
                    && !accountFieldRequired.startsWith(REPRESENTATIVE)
                    && !accountFieldRequired.startsWith(COMPANY_OWNERS_PROVIDED)
                    && !accountFieldRequired.startsWith(COMPANY_TAX_ID)
                    && (accountFieldRequired.startsWith(BUSINESS_PROFILE)
                        || accountFieldRequired.startsWith(COMPANY))
                    && !accountFieldRequired.equals(EXTERNAL_ACCOUNT_CODE_REQUIREMENT)))
        .map(
            field ->
                StripeAccountFieldsToClearspendBusinessFields.fromStripeField(
                    field.substring(field.indexOf(".") + 1)))
        .toList();
  }

  private Set<String> getAccountRequiredFields(Requirements accountRequirements) {
    if (accountRequirements == null) {
      return Collections.emptySet();
    }
    return Stream.of(
            accountRequirements.getCurrentlyDue(),
            accountRequirements.getEventuallyDue(),
            accountRequirements.getPastDue())
        .flatMap(Collection::stream)
        .collect(Collectors.toSet());
  }

  private Boolean requiredRelationShipToBusiness(Requirements requirements, final String mark) {
    Set<String> accountRequiredFields = getAccountRequiredFields(requirements);

    return accountRequiredFields.stream()
        .anyMatch(accountFieldRequired -> accountFieldRequired.startsWith(mark));
  }

  private Map<TypedId<BusinessOwnerId>, List<String>> extractStripeRequirementsForPersons(
      Requirements requirements, List<BusinessOwner> businessOwners) {
    Map<String, TypedId<BusinessOwnerId>> businessOwnerIdByStripeReference = new HashMap<>();
    return getAccountRequiredFields(requirements).stream()
        .filter(
            accountRequiredField ->
                accountRequiredField.startsWith(PERSON) && !accountRequiredField.endsWith(DOCUMENT))
        .collect(
            Collectors.groupingBy(
                s ->
                    businessOwnerIdByStripeReference.computeIfAbsent(
                        Splitter.on(".").splitToList(s).get(0),
                        stripeReferenceId ->
                            businessOwners.stream()
                                .filter(
                                    businessOwner ->
                                        stripeReferenceId.equals(
                                            businessOwner.getStripePersonReference()))
                                .findFirst()
                                .map(TypedMutable::getId)
                                .orElseThrow()),
                Collectors.mapping(
                    s ->
                        StripePersonFieldsToClearspendOwnerFields.fromStripeField(
                            s.substring(s.indexOf(".") + 1)),
                    Collectors.toList())));
  }

  private List<String> retrievePendingVerification(Requirements accountRequirements) {
    if (accountRequirements == null) {
      return Collections.emptyList();
    }
    return accountRequirements.getPendingVerification();
  }

  private List<StripeRequirementsErrorCode> retrieveErrorCodes(Requirements accountRequirements) {
    if (accountRequirements == null) {
      return Collections.emptyList();
    }
    return accountRequirements.getErrors().stream()
        .map(errors -> StripeRequirementsErrorCode.valueOf(errors.getCode()))
        .toList();
  }
}
