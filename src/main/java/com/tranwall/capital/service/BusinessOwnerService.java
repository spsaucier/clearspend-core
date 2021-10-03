package com.tranwall.capital.service;

import com.tranwall.capital.common.data.model.Address;
import com.tranwall.capital.crypto.data.model.embedded.NullableEncryptedString;
import com.tranwall.capital.crypto.data.model.embedded.RequiredEncryptedString;
import com.tranwall.capital.crypto.data.model.embedded.RequiredEncryptedStringWithHash;
import com.tranwall.capital.data.model.BusinessOwner;
import com.tranwall.capital.data.model.enums.BusinessOwnerStatus;
import com.tranwall.capital.data.model.enums.BusinessOwnerType;
import com.tranwall.capital.data.model.enums.Country;
import com.tranwall.capital.data.model.enums.KnowYourCustomerStatus;
import com.tranwall.capital.data.model.enums.RelationshipToBusiness;
import com.tranwall.capital.data.repository.BusinessOwnerRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class BusinessOwnerService {

  private final BusinessOwnerRepository businessOwnerRepository;

  @Transactional
  BusinessOwner createBusinessOwner(
      UUID businessId,
      String firstName,
      String lastName,
      Address address,
      String email,
      String phone) {
    BusinessOwner business =
        new BusinessOwner(
            businessId,
            BusinessOwnerType.UNSPECIFIED,
            new NullableEncryptedString(firstName),
            new NullableEncryptedString(lastName),
            RelationshipToBusiness.UNSPECIFIED,
            address,
            "",
            new RequiredEncryptedStringWithHash(email),
            new RequiredEncryptedString(phone),
            Country.UNSPECIFIED,
            KnowYourCustomerStatus.PENDING,
            BusinessOwnerStatus.ACTIVE);

    return businessOwnerRepository.save(business);
  }
}
