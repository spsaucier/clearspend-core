package com.tranwall.capital.service;

import com.tranwall.capital.common.data.model.Address;
import com.tranwall.capital.crypto.data.model.embedded.NullableEncryptedString;
import com.tranwall.capital.data.model.Allocation;
import com.tranwall.capital.data.model.Business;
import com.tranwall.capital.data.model.Program;
import com.tranwall.capital.data.model.enums.AccountType;
import com.tranwall.capital.data.model.enums.BusinessOnboardingStep;
import com.tranwall.capital.data.model.enums.BusinessStatus;
import com.tranwall.capital.data.model.enums.Currency;
import com.tranwall.capital.data.model.enums.KnowYourBusinessStatus;
import com.tranwall.capital.data.repository.BusinessRepository;
import com.tranwall.capital.data.repository.ProgramRepository;
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
      List<UUID> programIds) {
    Business business =
        businessRepository.save(
            Business.builder()
                .legalName(legalName)
                .address(address)
                .employerIdentificationNumber(employerIdentificationNumber)
                .email(new NullableEncryptedString(email))
                .phone(new NullableEncryptedString(phone))
                .formationDate(formationDate)
                .onboardingStep(BusinessOnboardingStep.COMPLETE)
                .knowYourBusinessStatus(KnowYourBusinessStatus.PENDING)
                .status(BusinessStatus.ONBOARDING)
                .build());

    accountService.createAccount(
        business.getId(), AccountType.BUSINESS, business.getId(), Currency.USD);

    for (UUID programId : programIds) {
      Optional<Program> programOptional = programRepository.findById(programId);
      if (programOptional.isEmpty()) {
        throw new IllegalArgumentException("Program not found: " + programId);
      }

      Allocation allocation =
          allocationService.createAllocation(
              programId,
              business.getId(),
              null,
              business.getLegalName() + " - " + programOptional.get().getName());

      accountService.createAccount(
          business.getId(), AccountType.ALLOCATION, allocation.getId(), Currency.USD);
    }

    return business;
  }
}
