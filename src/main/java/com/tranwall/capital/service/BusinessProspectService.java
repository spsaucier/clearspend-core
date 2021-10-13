package com.tranwall.capital.service;

import com.tranwall.capital.client.fusionauth.FusionAuthClient;
import com.tranwall.capital.common.data.model.Address;
import com.tranwall.capital.common.error.RecordNotFoundException;
import com.tranwall.capital.common.error.RecordNotFoundException.Table;
import com.tranwall.capital.controller.type.business.prospect.ValidateBusinessProspectIdentifierRequest.IdentifierType;
import com.tranwall.capital.crypto.data.model.embedded.NullableEncryptedString;
import com.tranwall.capital.crypto.data.model.embedded.RequiredEncryptedString;
import com.tranwall.capital.crypto.data.model.embedded.RequiredEncryptedStringWithHash;
import com.tranwall.capital.data.model.Account;
import com.tranwall.capital.data.model.Business;
import com.tranwall.capital.data.model.BusinessOwner;
import com.tranwall.capital.data.model.BusinessProspect;
import com.tranwall.capital.data.model.enums.BusinessType;
import com.tranwall.capital.data.model.enums.Currency;
import com.tranwall.capital.data.repository.BusinessProspectRepository;
import com.tranwall.capital.service.AllocationService.AllocationRecord;
import com.tranwall.capital.service.BusinessService.BusinessAndAllocationsRecord;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class BusinessProspectService {

  private final BusinessProspectRepository businessProspectRepository;

  private final BusinessService businessService;
  private final BusinessOwnerService businessOwnerService;
  private final FusionAuthClient fusionAuthClient;

  public record CreateBusinessProspectRecord(BusinessProspect businessProspect, String otp) {}

  @Transactional
  public CreateBusinessProspectRecord createBusinessProspect(
      String firstName, String lastName, String email) {
    BusinessProspect businessProspect =
        businessProspectRepository.save(
            new BusinessProspect(
                new RequiredEncryptedString(firstName),
                new RequiredEncryptedString(lastName),
                new RequiredEncryptedStringWithHash(email)));

    // TODO(kuchlein): need to call Twilio to generate OTP
    String otp = "1234";

    return new CreateBusinessProspectRecord(businessProspect, otp);
  }

  @Transactional
  public CreateBusinessProspectRecord setBusinessProspectPhone(
      UUID businessProspectId, String phone) {
    BusinessProspect businessProspect =
        businessProspectRepository
            .findById(businessProspectId)
            .orElseThrow(
                () -> new RecordNotFoundException(Table.BUSINESS_PROSPECT, businessProspectId));

    businessProspect.setPhone(new NullableEncryptedString(phone));

    // TODO(kuchlein): need to call Twilio to generate OTP
    String otp = "1234";

    businessProspect = businessProspectRepository.save(businessProspect);

    return new CreateBusinessProspectRecord(businessProspect, otp);
  }

  @Transactional
  public BusinessProspect validateBusinessProspectIdentifier(
      UUID businessProspectId, IdentifierType identifierType, String otp) {
    BusinessProspect businessProspect =
        businessProspectRepository
            .findById(businessProspectId)
            .orElseThrow(
                () -> new RecordNotFoundException(Table.BUSINESS_PROSPECT, businessProspectId));

    // TODO(kuchlein): validate OTP

    switch (identifierType) {
      case EMAIL -> businessProspect.setEmailVerified(true);
      case PHONE -> businessProspect.setPhoneVerified(true);
    }

    return businessProspectRepository.save(businessProspect);
  }

  @Transactional
  public BusinessProspect setBusinessProspectPassword(UUID businessProspectId, String password) {
    BusinessProspect businessProspect =
        businessProspectRepository
            .findById(businessProspectId)
            .orElseThrow(
                () -> new RecordNotFoundException(Table.BUSINESS_PROSPECT, businessProspectId));

    businessProspect.setSubjectRef(
        fusionAuthClient.createBusinessOwner(
            businessProspect.getBusinessId(),
            businessProspect.getBusinessOwnerId(),
            businessProspect.getEmail().getEncrypted(),
            password));

    return businessProspectRepository.save(businessProspect);
  }

  public record ConvertBusinessProspectRecord(
      Business business,
      Account businessAccount,
      List<AllocationRecord> allocationRecords,
      BusinessOwner businessOwner) {}

  @Transactional
  public ConvertBusinessProspectRecord convertBusinessProspect(
      UUID businessProspectId,
      String legalName,
      BusinessType businessType,
      String businessPhone,
      String employerIdentificationNumber,
      Address toAddress) {
    BusinessProspect businessProspect =
        businessProspectRepository
            .findById(businessProspectId)
            .orElseThrow(
                () -> new RecordNotFoundException(Table.BUSINESS_PROSPECT, businessProspectId));

    BusinessAndAllocationsRecord businessAndAllocationsRecord =
        businessService.createBusiness(
            businessProspect.getBusinessId(),
            legalName,
            businessType,
            toAddress,
            employerIdentificationNumber,
            businessProspect.getEmail().getEncrypted(),
            businessPhone,
            Collections.emptyList(),
            Currency.USD);

    BusinessOwner businessOwner =
        businessOwnerService.createBusinessOwner(
            businessProspect.getBusinessOwnerId(),
            businessAndAllocationsRecord.business().getId(),
            businessProspect.getFirstName().getEncrypted(),
            businessProspect.getLastName().getEncrypted(),
            toAddress,
            businessProspect.getEmail().getEncrypted(),
            businessProspect.getPhone().getEncrypted(),
            businessProspect.getSubjectRef());

    return new ConvertBusinessProspectRecord(
        businessAndAllocationsRecord.business(),
        businessAndAllocationsRecord.businessAccount(),
        businessAndAllocationsRecord.allocationRecords(),
        businessOwner);
  }
}
