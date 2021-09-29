package com.tranwall.service;

import com.tranwall.common.data.model.Address;
import com.tranwall.crypto.data.model.embedded.NullableEncryptedString;
import com.tranwall.data.model.Business;
import com.tranwall.data.model.Program;
import com.tranwall.data.model.enums.BusinessOnboardingStep;
import com.tranwall.data.model.enums.BusinessStatus;
import com.tranwall.data.model.enums.Currency;
import com.tranwall.data.model.enums.KnowYourBusinessStatus;
import com.tranwall.data.repository.BusinessRepository;
import com.tranwall.data.repository.ProgramRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class BusinessService {

  @NonNull private final BusinessRepository businessRepository;
  @NonNull private final ProgramRepository programRepository;

  @NonNull private final AllocationService allocationService;

  @Transactional
  Business createBusiness(
      String legalName,
      Address address,
      String employerIdentificationNumber,
      String email,
      String phone,
      LocalDate formationDate,
      List<UUID> programIds) {
    Business business = businessRepository.save(
        new Business(
            legalName,
            address,
            employerIdentificationNumber,
            new NullableEncryptedString(email),
            new NullableEncryptedString(phone),
            formationDate,
            BusinessOnboardingStep.COMPLETE,
            KnowYourBusinessStatus.PENDING,
            BusinessStatus.ONBOARDING));

    // FIXME(kuchlein): need to create business account

    for (UUID programId : programIds) {
      Optional<Program> programOptional = programRepository.findById(programId);
      if (programOptional.isEmpty()){
        throw new IllegalArgumentException("Program not found: " + programId);
      }

      allocationService.createAllocation(
          programId,
          business.getId(),
          null,
          business.getLegalName() + " - " + programOptional.get().getName());

      // FIXME(kuchlein): need to create allocation account
    }
    return business;
  }
}
