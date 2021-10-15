package com.tranwall.capital.service;

import com.tranwall.capital.client.fusionauth.FusionAuthClient;
import com.tranwall.capital.common.data.model.Address;
import com.tranwall.capital.common.typedid.data.BusinessId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.crypto.data.model.embedded.NullableEncryptedString;
import com.tranwall.capital.crypto.data.model.embedded.RequiredEncryptedStringWithHash;
import com.tranwall.capital.data.model.User;
import com.tranwall.capital.data.model.enums.UserType;
import com.tranwall.capital.data.repository.UserRepository;
import io.micrometer.core.instrument.util.StringUtils;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

  private final UserRepository userRepository;

  private final FusionAuthClient fusionAuthClient;

  @Transactional
  public User createUser(
      TypedId<BusinessId> businessId,
      UserType type,
      String firstName,
      String lastName,
      Address address,
      String email,
      String phone,
      String password) {
    User user =
        new User(
            businessId,
            type,
            new RequiredEncryptedStringWithHash(email),
            new RequiredEncryptedStringWithHash(phone));
    user.setFirstName(new NullableEncryptedString(firstName));
    user.setLastName(new NullableEncryptedString(lastName));
    user.setAddress(address);

    if (StringUtils.isNotBlank(password)) {
      user.setSubjectRef(fusionAuthClient.createUser(businessId, user.getId(), email, password));
    }

    return userRepository.save(user);
  }
}
