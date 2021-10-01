package com.tranwall.capital.service;

import com.tranwall.capital.common.data.model.Address;
import com.tranwall.capital.crypto.data.model.embedded.NullableEncryptedString;
import com.tranwall.capital.data.model.Business;
import com.tranwall.capital.data.model.Program;
import com.tranwall.capital.data.model.enums.AccountType;
import com.tranwall.capital.data.model.enums.BusinessOnboardingStep;
import com.tranwall.capital.data.model.enums.BusinessStatus;
import com.tranwall.capital.data.model.enums.Currency;
import com.tranwall.capital.data.model.enums.KnowYourBusinessStatus;
import com.tranwall.capital.data.repository.BusinessRepository;
import com.tranwall.capital.data.repository.ProgramRepository;
import com.tranwall.capital.service.AllocationService.AllocationRecord;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class BusinessService {

  private final BusinessRepository businessRepository;
  private final ProgramRepository programRepository;

  private final AllocationService allocationService;
  private final AccountService accountService;

  @Transactional
  Business createBusiness(
      String legalName,
      Address address,
      String employerIdentificationNumber,
      String email,
      String phone,
      LocalDate formationDate,
      List<UUID> programIds,
      Currency currency) {
    Business business =
        new Business(
            legalName,
            address,
            employerIdentificationNumber,
            BusinessOnboardingStep.COMPLETE,
            KnowYourBusinessStatus.PENDING,
            BusinessStatus.ONBOARDING);
    business.setEmail(new NullableEncryptedString(email));
    business.setPhone(new NullableEncryptedString(phone));
    business.setFormationDate(formationDate);

    business = businessRepository.save(business);

    accountService.createAccount(
        business.getId(), AccountType.BUSINESS, business.getId(), currency);

    for (UUID programId : programIds) {
      Optional<Program> programOptional = programRepository.findById(programId);
      if (programOptional.isEmpty()) {
        throw new IllegalArgumentException("Program not found: " + programId);
      }

      AllocationRecord allocationRecord =
          allocationService.createAllocation(
              programId,
              business.getId(),
              null,
              business.getLegalName() + " - " + programOptional.get().getName(),
              currency);

      accountService.createAccount(
          business.getId(),
          AccountType.ALLOCATION,
          allocationRecord.acccount().getId(),
          Currency.USD);
    }

    return business;
  }
}
