package com.clearspend.capital.service;

import static org.apache.commons.lang3.BooleanUtils.isTrue;

import com.clearspend.capital.client.stripe.StripeClient;
import com.clearspend.capital.common.error.DeleteBusinessOwnerNotAllowedException;
import com.clearspend.capital.common.error.InvalidKycDataException;
import com.clearspend.capital.common.error.RecordNotFoundException;
import com.clearspend.capital.common.error.Table;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.common.typedid.data.business.BusinessOwnerId;
import com.clearspend.capital.controller.type.business.owner.OwnersProvidedRequest;
import com.clearspend.capital.crypto.HashUtil;
import com.clearspend.capital.crypto.data.model.embedded.NullableEncryptedString;
import com.clearspend.capital.crypto.data.model.embedded.RequiredEncryptedString;
import com.clearspend.capital.crypto.data.model.embedded.RequiredEncryptedStringWithHash;
import com.clearspend.capital.data.model.User;
import com.clearspend.capital.data.model.business.Business;
import com.clearspend.capital.data.model.business.BusinessOwner;
import com.clearspend.capital.data.model.enums.BusinessOnboardingStep;
import com.clearspend.capital.data.model.enums.BusinessType;
import com.clearspend.capital.data.repository.business.BusinessOwnerRepository;
import com.clearspend.capital.service.type.BusinessOwnerData;
import com.google.errorprone.annotations.RestrictedApi;
import com.stripe.model.Person;
import io.jsonwebtoken.lang.Assert;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class BusinessOwnerService {

  public @interface TestDataBusinessOp {
    String reviewer();

    String explanation();
  }

  public @interface CreateBusinessOwner {
    String reviewer();

    String explanation();
  }

  public @interface KycBusinessOwner {
    String reviewer();

    String explanation();
  }

  public @interface LoginBusinessOwner {
    String reviewer();

    String explanation();
  }

  private final BusinessOwnerRepository businessOwnerRepository;

  private final StripeClient stripeClient;

  private final BusinessService businessService;

  public record BusinessOwnerAndUserRecord(BusinessOwner businessOwner, User user) {}

  public record BusinessOwnerAndStripePersonRecord(BusinessOwner businessOwner, Person person) {}

  public record BusinessAndAccountErrorMessages(Business business, List<String> errorMessages) {}

  @PreAuthorize("hasRootPermission(#businessId, 'MANAGE_USERS')")
  public BusinessAndAccountErrorMessages allOwnersProvided(
      TypedId<BusinessId> businessId, OwnersProvidedRequest ownersProvidedRequest) {
    return doAllOwnersProvided(businessId, ownersProvidedRequest);
  }

  @RestrictedApi(
      explanation =
          "This method is needed for generating test data when no security context is available",
      link = "",
      allowlistAnnotations = {TestDataBusinessOp.class})
  public BusinessAndAccountErrorMessages restrictedAllOwnersProvided(
      final TypedId<BusinessId> businessId, final OwnersProvidedRequest ownersProvidedRequest) {
    return doAllOwnersProvided(businessId, ownersProvidedRequest);
  }

  private BusinessAndAccountErrorMessages doAllOwnersProvided(
      final TypedId<BusinessId> businessId, final OwnersProvidedRequest ownersProvidedRequest) {
    Business business = businessService.retrieveBusinessForService(businessId, true);
    List<BusinessOwner> businessOwners = findBusinessOwnerByBusinessId(business.getId());

    stripeClient.triggerAccountValidationAfterPersonsProvided(
        business.getStripeData().getAccountRef(),
        isTrue(ownersProvidedRequest.getNoOtherOwnersToProvide())
            || businessOwners.stream()
                .filter(businessOwnerData -> businessOwnerData.getRelationshipOwner() != null)
                .anyMatch(BusinessOwner::getRelationshipOwner),
        isTrue(ownersProvidedRequest.getNoExecutiveToProvide())
            || businessOwners.stream()
                .filter(businessOwnerData -> businessOwnerData.getRelationshipExecutive() != null)
                .anyMatch(BusinessOwner::getRelationshipExecutive));

    return new BusinessAndAccountErrorMessages(
        businessService.retrieveBusinessForService(businessId, true),
        java.util.Collections.emptyList());
  }

  @PreAuthorize("hasRootPermission(#businessOwnerData.businessId, 'MANAGE_USERS')")
  public void validateOwner(BusinessOwnerData businessOwnerData) {

    Assert.notNull(businessOwnerData);

    Business business =
        businessService.retrieveBusinessForService(businessOwnerData.getBusinessId(), true);

    List<BusinessOwner> ownersForBusinessId =
        businessOwnerRepository.findByBusinessId(businessOwnerData.getBusinessId());

    if (business.getType() != BusinessType.INDIVIDUAL) {
      if (ownersForBusinessId.stream()
              .filter(businessOwner -> businessOwner.getRelationshipRepresentative() != null)
              .anyMatch(BusinessOwner::getRelationshipRepresentative)
          && isTrue(businessOwnerData.getRelationshipRepresentative())) {
        throw new InvalidKycDataException(
            businessOwnerData.getFirstName() + businessOwnerData.getLastName(),
            "Only one representative is allowed.");
      }

    } else {
      if (!ownersForBusinessId.isEmpty()
          && businessOwnerRepository.findById(businessOwnerData.getBusinessOwnerId()).isEmpty()) {
        throw new InvalidKycDataException(
            businessOwnerData.getFirstName() + businessOwnerData.getLastName(),
            String.format("Only one owner is allowed for business %s.", business.getLegalName()));
      }

      if (Boolean.FALSE.equals(businessOwnerData.getRelationshipOwner())) {
        throw new InvalidKycDataException(
            businessOwnerData.getFirstName() + businessOwnerData.getLastName(),
            String.format(
                "Please provide owner details for business %s.", business.getLegalName()));
      }
    }
  }

  @PreAuthorize("hasRootPermission(#businessId, 'MANAGE_USERS')")
  public void validateBusinessOwners(
      TypedId<BusinessId> businessId, OwnersProvidedRequest ownersProvidedRequest) {

    Business business = businessService.retrieveBusinessForService(businessId, true);

    List<BusinessOwner> ownersForBusinessId = businessOwnerRepository.findByBusinessId(businessId);

    if (!List.of(BusinessType.SOLE_PROPRIETORSHIP, BusinessType.INDIVIDUAL)
            .contains(business.getType())
        && ownersForBusinessId.stream().noneMatch(BusinessOwner::getRelationshipRepresentative)) {
      throw new AssertionError(
          String.format(
              "Please provide at least one representative for %s.", business.getLegalName()));
    }

    if (!Boolean.TRUE.equals(ownersProvidedRequest.getNoOtherOwnersToProvide())
        && List.of(
                BusinessType.MULTI_MEMBER_LLC,
                BusinessType.PRIVATE_PARTNERSHIP,
                BusinessType.PRIVATE_CORPORATION)
            .contains(business.getType())
        && ownersForBusinessId.stream().noneMatch(BusinessOwner::getRelationshipOwner)) {
      throw new AssertionError(
          String.format("Please provide owner details for business %s.", business.getLegalName()));
    }

    if (!Boolean.TRUE.equals(ownersProvidedRequest.getNoExecutiveToProvide())
        && List.of(
                BusinessType.MULTI_MEMBER_LLC,
                BusinessType.PRIVATE_PARTNERSHIP,
                BusinessType.PRIVATE_CORPORATION,
                BusinessType.INCORPORATED_NON_PROFIT)
            .contains(business.getType())
        && ownersForBusinessId.stream().noneMatch(BusinessOwner::getRelationshipExecutive)) {
      throw new AssertionError(
          String.format("Please provide the executive for business %s.", business.getLegalName()));
    }
  }

  @Transactional
  @PreAuthorize("hasRootPermission(#businessId, 'MANAGE_USERS')")
  public BusinessOwnerAndStripePersonRecord createBusinessOwnerAndStripePerson(
      TypedId<BusinessId> businessId, BusinessOwnerData businessOwnerData) {

    Assert.notNull(businessOwnerData);
    // TODO:gb: In case this will be updated not on the onboarding
    // what will be the flow to validate email and phone before this method
    BusinessOwner businessOwner = createBusinessOwner(businessOwnerData);

    Business business = businessService.retrieveBusinessForService(businessId, true);
    String stripeAccountReference = business.getStripeData().getAccountRef();

    Person stripePerson;
    stripePerson = stripeClient.createPerson(businessOwner, stripeAccountReference);
    businessOwner.setStripePersonReference(stripePerson.getId());

    return new BusinessOwnerAndStripePersonRecord(businessOwner, stripePerson);
  }

  @Transactional
  @PreAuthorize("hasRootPermission(#user.businessId, 'MANAGE_USERS')")
  public BusinessOwnerAndStripePersonRecord updateBusinessOwnerAndStripePerson(User user) {
    BusinessOwner businessOwner =
        businessOwnerRepository.findBySubjectRef(user.getSubjectRef()).orElseThrow();
    BusinessOwnerData businessOwnerData =
        new BusinessOwnerData(
            businessOwner.getId(),
            user.getBusinessId(),
            user.getFirstName().getEncrypted(),
            user.getLastName().getEncrypted(),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            user.getAddress(),
            null,
            null,
            user.getSubjectRef(),
            false);
    return updateBusinessOwnerAndStripePerson(user.getBusinessId(), businessOwnerData);
  }

  @Transactional
  @PreAuthorize("hasRootPermission(#businessId, 'MANAGE_USERS')")
  public BusinessOwnerAndStripePersonRecord updateBusinessOwnerAndStripePerson(
      TypedId<BusinessId> businessId, BusinessOwnerData businessOwnerData) {
    return doUpdateBusinessOwnerAndStripePerson(businessId, businessOwnerData);
  }

  private BusinessOwnerAndStripePersonRecord doUpdateBusinessOwnerAndStripePerson(
      final TypedId<BusinessId> businessId, final BusinessOwnerData businessOwnerData) {
    Assert.notNull(businessOwnerData);
    // TODO:gb: In case this will be updated not on the onboarding
    // what will be the flow to validate email and phone before this method
    BusinessOwner businessOwner = updateBusinessOwner(businessOwnerData);

    Business business = businessService.retrieveBusinessForService(businessId, true);
    String stripeAccountReference = business.getStripeData().getAccountRef();

    Person stripePerson;
    if (businessOwner.getStripePersonReference() != null) {
      stripePerson = stripeClient.updatePerson(businessOwner, stripeAccountReference);
    } else {
      stripePerson = stripeClient.createPerson(businessOwner, stripeAccountReference);
      businessOwner.setStripePersonReference(stripePerson.getId());
    }

    return new BusinessOwnerAndStripePersonRecord(businessOwner, stripePerson);
  }

  @RestrictedApi(
      explanation =
          "This method is needed for generating test data which cannot support permissions restriction",
      link =
          "https://tranwall.atlassian.net/wiki/spaces/CAP/pages/2088828965/Dev+notes+Service+method+security",
      allowlistAnnotations = {TestDataBusinessOp.class})
  public BusinessOwnerAndStripePersonRecord restrictedUpdateBusinessOwnerAndStripePerson(
      final TypedId<BusinessId> businessId, final BusinessOwnerData businessOwnerData) {
    return doUpdateBusinessOwnerAndStripePerson(businessId, businessOwnerData);
  }

  @Transactional
  BusinessOwner createBusinessOwner(BusinessOwnerData businessOwnerData) {

    BusinessOwner businessOwner = businessOwnerData.toBusinessOwner();
    businessOwner = businessOwnerRepository.save(businessOwner);
    businessOwnerRepository.flush();

    return businessOwner;
  }

  @RestrictedApi(
      link =
          "https://tranwall.atlassian.net/wiki/spaces/CAP/pages/2088828965/Dev+notes+Service+method+security",
      explanation = "This mainly exists to support generating test data.",
      allowlistAnnotations = {CreateBusinessOwner.class})
  public BusinessOwner restrictedCreateBusinessOwner(final BusinessOwnerData businessOwnerData) {
    return createBusinessOwner(businessOwnerData);
  }

  BusinessOwner retrieveBusinessOwner(TypedId<BusinessOwnerId> businessOwnerId) {
    return businessOwnerRepository
        .findById(businessOwnerId)
        .orElseThrow(() -> new RecordNotFoundException(Table.BUSINESS_OWNER, businessOwnerId));
  }

  Optional<BusinessOwner> retrieveBusinessOwnerByEmail(String email) {
    return businessOwnerRepository.findByEmailHash(HashUtil.calculateHash(email));
  }

  // This works because the one place this is called, the CurrentUser should be the business owner
  @PostAuthorize(
      "returnObject.isEmpty() || hasRootPermission(returnObject.orElse(null)?.businessId, 'MANAGE_USERS')")
  public Optional<BusinessOwner> retrieveBusinessOwnerNotThrowingException(
      TypedId<BusinessOwnerId> businessOwnerId) {
    return businessOwnerRepository.findById(businessOwnerId);
  }

  @RestrictedApi(
      link =
          "https://tranwall.atlassian.net/wiki/spaces/CAP/pages/2088828965/Dev+notes+Service+method+security",
      explanation = "This mainly exists to support generating test data.",
      allowlistAnnotations = {TestDataBusinessOp.class})
  public BusinessOwner restrictedUpdateBusinessOwner(final BusinessOwnerData businessOwnerData) {
    return updateBusinessOwner(businessOwnerData);
  }

  @Transactional
  BusinessOwner updateBusinessOwner(BusinessOwnerData businessOwnerData) {
    BusinessOwner businessOwner =
        businessOwnerRepository
            .findById(businessOwnerData.getBusinessOwnerId())
            .orElseThrow(
                () ->
                    new RecordNotFoundException(
                        Table.BUSINESS_OWNER, businessOwnerData.getBusinessOwnerId()));

    if (StringUtils.isNotBlank(businessOwnerData.getFirstName())) {
      businessOwner.setFirstName(new NullableEncryptedString(businessOwnerData.getFirstName()));
    }

    if (StringUtils.isNotBlank(businessOwnerData.getLastName())) {
      businessOwner.setLastName(new NullableEncryptedString(businessOwnerData.getLastName()));
    }

    if (StringUtils.isNotBlank(businessOwnerData.getEmail())) {
      businessOwner.setEmail(new RequiredEncryptedStringWithHash(businessOwnerData.getEmail()));
    }

    if (StringUtils.isNotBlank(businessOwnerData.getTaxIdentificationNumber())) {
      businessOwner.setTaxIdentificationNumber(
          new NullableEncryptedString(businessOwnerData.getTaxIdentificationNumber()));
    }

    if (businessOwnerData.getDateOfBirth() != null) {
      businessOwner.setDateOfBirth(businessOwnerData.getDateOfBirth());
    }

    if (businessOwnerData.getAddress() != null) {
      businessOwner.setAddress(businessOwnerData.getAddress());
    }

    if (StringUtils.isNotBlank(businessOwnerData.getTitle())) {
      businessOwner.setTitle(businessOwnerData.getTitle());
    }

    if (businessOwnerData.getPercentageOwnership() != null) {
      businessOwner.setPercentageOwnership(businessOwnerData.getPercentageOwnership());
    }

    if (businessOwnerData.getRelationshipOwner() != null) {
      businessOwner.setRelationshipOwner(businessOwnerData.getRelationshipOwner());
    }

    if (businessOwnerData.getRelationshipRepresentative() != null) {
      businessOwner.setRelationshipRepresentative(
          businessOwnerData.getRelationshipRepresentative());
    }

    if (businessOwnerData.getRelationshipExecutive() != null) {
      businessOwner.setRelationshipExecutive(businessOwnerData.getRelationshipExecutive());
    }

    if (businessOwnerData.getRelationshipDirector() != null) {
      businessOwner.setRelationshipDirector(businessOwnerData.getRelationshipDirector());
    }

    if (businessOwnerData.getPhone() != null) {
      businessOwner.setPhone(new RequiredEncryptedString(businessOwnerData.getPhone()));
    }

    BusinessOwner owner = businessOwnerRepository.save(businessOwner);

    businessOwnerRepository.flush();

    return owner;
  }

  @RestrictedApi(
      link =
          "https://tranwall.atlassian.net/wiki/spaces/CAP/pages/2088828965/Dev+notes+Service+method+security",
      explanation =
          "This method is used as part of the login flow before the SecurityContext is available",
      allowlistAnnotations = {LoginBusinessOwner.class})
  public Optional<BusinessOwner> retrieveBusinessOwnerBySubjectRef(String subjectRef) {
    return businessOwnerRepository.findBySubjectRef(subjectRef);
  }

  List<BusinessOwner> findBusinessOwnerByBusinessId(TypedId<BusinessId> businessIdTypedId) {
    return businessOwnerRepository.findByBusinessId(businessIdTypedId);
  }

  @PreAuthorize("hasRootPermission(#businessId, 'MANAGE_USERS')")
  public List<BusinessOwner> secureFindBusinessOwner(final TypedId<BusinessId> businessId) {
    return findBusinessOwnerByBusinessId(businessId);
  }

  @RestrictedApi(
      link =
          "https://tranwall.atlassian.net/wiki/spaces/CAP/pages/2088828965/Dev+notes+Service+method+security",
      explanation =
          "This method needs to be accessible by the KYC process which is called from Stripe webhooks, so permissions are not possible.",
      allowlistAnnotations = {KycBusinessOwner.class})
  public List<BusinessOwner> findBusinessOwnerForKyc(final TypedId<BusinessId> businessId) {
    return findBusinessOwnerByBusinessId(businessId);
  }

  BusinessOwner findBusinessOwnerByStripePersonReference(String stripePersonReference) {
    return businessOwnerRepository
        .findByStripePersonReference(stripePersonReference)
        .orElseThrow(
            () -> new RecordNotFoundException(Table.BUSINESS_OWNER, stripePersonReference));
  }

  @Transactional
  @PreAuthorize("hasRootPermission(#businessId, 'MANAGE_USERS')")
  public void deleteBusinessOwner(
      TypedId<BusinessOwnerId> businessOwnerId, TypedId<BusinessId> businessId) {
    Business business = businessService.getBusiness(businessId).business();
    if (business.getOnboardingStep() != BusinessOnboardingStep.BUSINESS_OWNERS) {
      throw new DeleteBusinessOwnerNotAllowedException(
          "You can delete owners just at onboarding stage.");
    }
    Optional<BusinessOwner> businessOwner = businessOwnerRepository.findById(businessOwnerId);
    if (businessOwner.isPresent()) {
      String stripePersonReference = businessOwner.get().getStripePersonReference();
      if (stripePersonReference != null) {

        stripeClient.deletePerson(businessOwner.get(), business.getStripeData().getAccountRef());
      }
      businessOwnerRepository.deleteById(businessOwnerId);
    }
  }
}
