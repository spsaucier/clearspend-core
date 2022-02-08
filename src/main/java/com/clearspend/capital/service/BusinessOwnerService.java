package com.clearspend.capital.service;

import com.clearspend.capital.client.stripe.StripeClient;
import com.clearspend.capital.common.error.RecordNotFoundException;
import com.clearspend.capital.common.error.Table;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.common.typedid.data.business.BusinessOwnerId;
import com.clearspend.capital.crypto.data.model.embedded.NullableEncryptedString;
import com.clearspend.capital.crypto.data.model.embedded.RequiredEncryptedStringWithHash;
import com.clearspend.capital.data.model.User;
import com.clearspend.capital.data.model.business.Business;
import com.clearspend.capital.data.model.business.BusinessOwner;
import com.clearspend.capital.data.model.enums.KnowYourCustomerStatus;
import com.clearspend.capital.data.model.enums.UserType;
import com.clearspend.capital.data.repository.business.BusinessOwnerRepository;
import com.clearspend.capital.service.type.BusinessOwnerData;
import com.stripe.model.Account;
import com.stripe.model.Account.Requirements.Errors;
import com.stripe.model.Person;
import com.stripe.model.Person.Requirements;
import io.jsonwebtoken.lang.Assert;
import io.jsonwebtoken.lang.Collections;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class BusinessOwnerService {

  private final TwilioService twilioService;

  private final BusinessOwnerRepository businessOwnerRepository;

  private final UserService userService;

  private final StripeClient stripeClient;

  private final BusinessService businessService;

  public record BusinessOwnerAndUserRecord(BusinessOwner businessOwner, User user) {}

  public record StripePersonAndErrorMessages(Person person, List<String> errorMessages) {}

  @Transactional
  public BusinessOwnerAndUserRecord createMainBusinessOwnerAndRepresentative(
      BusinessOwnerData businessOwnerData) {

    BusinessOwner businessOwner = businessOwnerData.toBusinessOwner();
    businessOwner = businessOwnerRepository.save(businessOwner);
    businessOwnerRepository.flush();

    User user =
        userService.createUserForFusionAuthUser(
            new TypedId<>(businessOwner.getId().toUuid()),
            businessOwnerData.getBusinessId(),
            UserType.BUSINESS_OWNER,
            businessOwnerData.getFirstName(),
            businessOwnerData.getLastName(),
            businessOwnerData.getAddress(),
            businessOwnerData.getEmail(),
            businessOwnerData.getPhone(),
            businessOwnerData.getSubjectRef());

    return new BusinessOwnerAndUserRecord(businessOwner, user);
  }

  @Transactional
  public List<BusinessOwner> createOrUpdateBusinessOwners(
      TypedId<BusinessId> businessId, List<BusinessOwnerData> businessOwnersData) {

    Assert.notEmpty(businessOwnersData);
    // TODO:gb: In case this will be updated not on the onboarding
    // what will be the flow to validate email and phone before this method
    List<BusinessOwner> businessOwners =
        businessOwnersData.stream()
            .map(
                businessOwner ->
                    businessOwner.getBusinessOwnerId() != null
                        ? updateBusinessOwner(businessOwner)
                        : createBusinessOwner(businessOwner))
            .toList();

    Business business = businessService.retrieveBusiness(businessId);
    String stripeAccountReference = business.getStripeAccountReference();

    businessOwners.forEach(
        businessOwner ->
            createOrUpdateStripePersonReference(businessOwner, stripeAccountReference));

    Account updatedAccount =
        stripeClient.triggerAccountValidationAfterPersonsProvided(
            stripeAccountReference,
            businessOwnersData.stream()
                .filter(businessOwnerData -> businessOwnerData.getRelationshipOwner() != null)
                .anyMatch(BusinessOwnerData::getRelationshipOwner),
            businessOwnersData.stream()
                .filter(businessOwnerData -> businessOwnerData.getRelationshipExecutive() != null)
                .anyMatch(BusinessOwnerData::getRelationshipExecutive));

    businessService.updateBusinessAccordingToStripeAccountRequirements(business, updatedAccount);

    return businessOwners;
  }

  @Transactional
  public BusinessOwner createBusinessOwner(BusinessOwnerData businessOwnerData) {

    BusinessOwner businessOwner = businessOwnerData.toBusinessOwner();
    businessOwner = businessOwnerRepository.save(businessOwner);
    businessOwnerRepository.flush();

    return businessOwner;
  }

  private StripePersonAndErrorMessages createOrUpdateStripePersonReference(
      BusinessOwner businessOwner, String stripeAccountReference) {

    Person stripePerson;

    if (businessOwner.getStripePersonReference() == null) {
      stripePerson = stripeClient.createPerson(businessOwner, stripeAccountReference);
      businessOwner.setStripePersonReference(stripePerson.getId());
    } else {
      stripePerson = stripeClient.updatePerson(businessOwner, stripeAccountReference);
    }

    List<String> stripePersonErrorMessages =
        updateBusinessOwnerAccordingToStripePersonRequirements(businessOwner, stripePerson);

    return new StripePersonAndErrorMessages(stripePerson, stripePersonErrorMessages);
  }

  private List<String> updateBusinessOwnerAccordingToStripePersonRequirements(
      BusinessOwner businessOwner, Person stripePerson) {
    Requirements stripePersonRequirements = stripePerson.getRequirements();

    List<String> stripePersonErrorMessages = new ArrayList<>();
    if (stripePersonRequirements != null
        && (!Collections.isEmpty(stripePersonRequirements.getCurrentlyDue())
            || !Collections.isEmpty(stripePersonRequirements.getEventuallyDue())
            || !Collections.isEmpty(stripePersonRequirements.getPastDue())
            || !Collections.isEmpty(stripePersonRequirements.getPendingVerification())
            || !Collections.isEmpty(stripePersonRequirements.getErrors()))) {
      businessOwner.setKnowYourCustomerStatus(KnowYourCustomerStatus.REVIEW);

      return extractErrorMessages(stripePersonRequirements);
    }

    businessOwner.setKnowYourCustomerStatus(KnowYourCustomerStatus.PASS);

    return stripePersonErrorMessages;
  }

  private List<String> extractErrorMessages(Person.Requirements requirements) {
    return requirements.getErrors() != null
        ? requirements.getErrors().stream().map(Errors::getReason).toList()
        : null;
  }

  public BusinessOwner retrieveBusinessOwner(TypedId<BusinessOwnerId> businessOwnerId) {
    return businessOwnerRepository
        .findById(businessOwnerId)
        .orElseThrow(() -> new RecordNotFoundException(Table.BUSINESS_OWNER, businessOwnerId));
  }

  public Optional<BusinessOwner> retrieveBusinessOwnerNotThrowingException(
      TypedId<BusinessOwnerId> businessOwnerId) {
    return businessOwnerRepository.findById(businessOwnerId);
  }

  @Transactional
  public BusinessOwner updateBusinessOwnerStatus(
      TypedId<BusinessOwnerId> businessOwnerId, KnowYourCustomerStatus knowYourCustomerStatus) {

    BusinessOwner businessOwner =
        businessOwnerRepository
            .findById(businessOwnerId)
            .orElseThrow(() -> new RecordNotFoundException(Table.BUSINESS_OWNER, businessOwnerId));

    if (knowYourCustomerStatus != null) {
      businessOwner.setKnowYourCustomerStatus(knowYourCustomerStatus);
    }

    return businessOwnerRepository.save(businessOwner);
  }

  @Transactional
  public void updateBusinessOwnerStatusByStripePersonReference(
      String stripePersonReference, KnowYourCustomerStatus knowYourCustomerStatus) {
    if (knowYourCustomerStatus == null) {
      return;
    }

    BusinessOwner businessOwner =
        businessOwnerRepository
            .findByStripePersonReference(stripePersonReference)
            .orElseThrow(
                () -> new RecordNotFoundException(Table.BUSINESS_OWNER, stripePersonReference));

    businessOwner.setKnowYourCustomerStatus(knowYourCustomerStatus);

    businessOwnerRepository.save(businessOwner);
  }

  @Transactional
  public BusinessOwner updateBusinessOwner(BusinessOwnerData businessOwnerData) {
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

    BusinessOwner owner = businessOwnerRepository.save(businessOwner);

    businessOwnerRepository.flush();

    return owner;
  }

  public Optional<BusinessOwner> retrieveBusinessOwnerBySubjectRef(String subjectRef) {
    return businessOwnerRepository.findBySubjectRef(subjectRef);
  }

  public List<BusinessOwner> findBusinessOwnerByBusinessId(TypedId<BusinessId> businessIdTypedId) {
    return businessOwnerRepository.findByBusinessId(businessIdTypedId);
  }

  public BusinessOwner findBusinessOwnerByStripePersonReference(String stripePersonReference) {
    return businessOwnerRepository
        .findByStripePersonReference(stripePersonReference)
        .orElseThrow(
            () -> new RecordNotFoundException(Table.BUSINESS_OWNER, stripePersonReference));
  }
}
