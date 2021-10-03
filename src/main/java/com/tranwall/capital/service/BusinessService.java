package com.tranwall.capital.service;

import com.tranwall.capital.common.data.model.ClearAddress;
import com.tranwall.capital.common.error.RecordNotFoundException;
import com.tranwall.capital.common.error.RecordNotFoundException.Table;
import com.tranwall.capital.crypto.data.model.embedded.NullableEncryptedString;
import com.tranwall.capital.data.model.Account;
import com.tranwall.capital.data.model.Business;
import com.tranwall.capital.data.model.Program;
import com.tranwall.capital.data.model.enums.AccountType;
import com.tranwall.capital.data.model.enums.BusinessOnboardingStep;
import com.tranwall.capital.data.model.enums.BusinessStatus;
import com.tranwall.capital.data.model.enums.BusinessType;
import com.tranwall.capital.data.model.enums.Currency;
import com.tranwall.capital.data.model.enums.KnowYourBusinessStatus;
import com.tranwall.capital.data.repository.BusinessRepository;
import com.tranwall.capital.data.repository.ProgramRepository;
import com.tranwall.capital.service.AllocationService.AllocationRecord;
import java.time.LocalDate;
import java.util.ArrayList;
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

  public record BusinessRecord(
      Business business, Account businessAccount, List<AllocationRecord> allocationRecords) {}

  @Transactional
  BusinessRecord createBusiness(
      String legalName,
      BusinessType type,
      ClearAddress clearAddress,
      String employerIdentificationNumber,
      String email,
      String phone,
      LocalDate formationDate,
      List<UUID> programIds,
      Currency currency) {
    Business business =
        new Business(
            legalName,
            type,
            clearAddress,
            employerIdentificationNumber,
            BusinessOnboardingStep.COMPLETE,
            KnowYourBusinessStatus.PENDING,
            BusinessStatus.ONBOARDING);
    business.setBusinessEmail(new NullableEncryptedString(email));
    business.setBusinessPhone(new NullableEncryptedString(phone));
    business.setFormationDate(formationDate);

    business = businessRepository.save(business);

    Account account =
        accountService.createAccount(
            business.getId(), AccountType.BUSINESS, business.getId(), currency);

    List<AllocationRecord> allocationRecords = new ArrayList<>(programIds.size());
    for (UUID programId : programIds) {
      Optional<Program> programOptional = programRepository.findById(programId);
      if (programOptional.isEmpty()) {
        throw new RecordNotFoundException(Table.PROGRAM, programId);
      }

      allocationRecords.add(
          allocationService.createAllocation(
              programId,
              business.getId(),
              null,
              business.getLegalName() + " - " + programOptional.get().getName(),
              currency));
    }

    return new BusinessRecord(business, account, allocationRecords);
  }
}
