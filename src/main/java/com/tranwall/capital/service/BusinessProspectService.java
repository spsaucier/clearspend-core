package com.tranwall.capital.service;

import com.tranwall.capital.common.data.model.ClearAddress;
import com.tranwall.capital.common.error.RecordNotFoundException;
import com.tranwall.capital.common.error.RecordNotFoundException.Table;
import com.tranwall.capital.controller.type.ValidateBusinessProspectIdentifierRequest.IdentifierType;
import com.tranwall.capital.crypto.data.model.embedded.RequiredEncryptedString;
import com.tranwall.capital.crypto.data.model.embedded.RequiredEncryptedStringWithHash;
import com.tranwall.capital.data.model.BusinessProspect;
import com.tranwall.capital.data.model.enums.BusinessType;
import com.tranwall.capital.data.model.enums.Currency;
import com.tranwall.capital.data.repository.BusinessProspectRepository;
import com.tranwall.capital.service.BusinessService.BusinessRecord;
import java.time.LocalDate;
import java.util.Collections;
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

  @Transactional
  public BusinessProspect createBusinessProspect(String firstName, String lastName, String email) {
    return businessProspectRepository.save(
        new BusinessProspect(
            new RequiredEncryptedString(firstName),
            new RequiredEncryptedString(lastName),
            new RequiredEncryptedStringWithHash(email)));
  }

  @Transactional
  public BusinessProspect setBusinessProspectPhone(UUID businessProspectId, String phone) {
    BusinessProspect businessProspect = businessProspectRepository
        .findById(businessProspectId)
        .orElseThrow(
            () -> new RecordNotFoundException(Table.BUSINESS_PROSPECT, businessProspectId));

    businessProspect.setEmail(new RequiredEncryptedStringWithHash(phone));

    return businessProspectRepository.save(businessProspect);
  }

  @Transactional
  public BusinessProspect validateBusinessProspectIdentifier(UUID businessProspectId,
      IdentifierType identifierType, String otp) {
    BusinessProspect businessProspect = businessProspectRepository
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
    BusinessProspect businessProspect = businessProspectRepository
        .findById(businessProspectId)
        .orElseThrow(
            () -> new RecordNotFoundException(Table.BUSINESS_PROSPECT, businessProspectId));

    // TODO(kuchlein): create account on FusionAuth
    businessProspect.setSubjectRef(UUID.randomUUID().toString());

    return businessProspectRepository.save(businessProspect);
  }

  @Transactional
  public BusinessRecord convertBusinessProspect(UUID businessProspectId, String legalName,
      BusinessType businessType, LocalDate formationDate, ClearAddress address) {
    BusinessProspect businessProspect = businessProspectRepository
        .findById(businessProspectId)
        .orElseThrow(
            () -> new RecordNotFoundException(Table.BUSINESS_PROSPECT, businessProspectId));

    BusinessRecord businessRecord = businessService.createBusiness(legalName, businessType, address, "", businessProspect.getEmail().getEncrypted(),
        businessProspect.getPhone().getEncrypted(), formationDate, Collections.emptyList(), Currency.USD);

    businessOwnerService.createBusinessOwner(businessRecord.business().getId(),
        businessProspect.getFirstName().getEncrypted(),
        businessProspect.getLastName().getEncrypted(),
        address.toAddress(),
        businessProspect.getEmail().getEncrypted(),
        businessProspect.getPhone().getEncrypted() );

    return businessRecord;
  }
}
