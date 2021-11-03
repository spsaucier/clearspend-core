package com.tranwall.capital.service;

import com.tranwall.capital.common.data.model.Address;
import com.tranwall.capital.common.error.InvalidRequestException;
import com.tranwall.capital.common.error.RecordNotFoundException;
import com.tranwall.capital.common.error.RecordNotFoundException.Table;
import com.tranwall.capital.common.typedid.data.BusinessId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.common.typedid.data.UserId;
import com.tranwall.capital.crypto.PasswordUtil;
import com.tranwall.capital.crypto.data.model.embedded.NullableEncryptedStringWithHash;
import com.tranwall.capital.crypto.data.model.embedded.RequiredEncryptedStringWithHash;
import com.tranwall.capital.data.model.User;
import com.tranwall.capital.data.model.enums.UserType;
import com.tranwall.capital.data.repository.UserRepository;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

  private final UserRepository userRepository;

  private final FusionAuthService fusionAuthService;
  private final TwilioService twilioService;

  public record CreateUserRecord(User user, String password) {}

  @Transactional
  public CreateUserRecord createUser(
      TypedId<BusinessId> businessId,
      UserType type,
      String firstName,
      String lastName,
      @Nullable Address address,
      String email,
      String phone,
      boolean generatePassword,
      String subjectRef)
      throws IOException {
    User user =
        new User(
            businessId,
            type,
            new RequiredEncryptedStringWithHash(firstName),
            new RequiredEncryptedStringWithHash(lastName),
            new RequiredEncryptedStringWithHash(email),
            new NullableEncryptedStringWithHash(phone));
    user.setAddress(address);
    user.setSubjectRef(subjectRef);

    String password = null;
    if (generatePassword) {
      if (StringUtils.isNoneEmpty(subjectRef)) {
        throw new InvalidRequestException(
            "SubjectRef must be empty when generate password is true");
      }
      password = PasswordUtil.generatePassword();
      user.setSubjectRef(
          fusionAuthService.createUser(businessId, user.getId(), email, password).toString());
      twilioService.sendNotificationEmail(
          user.getEmail().getEncrypted(),
          String.format("Welcome to ClearSpend, your password is %s", password));
    }

    return new CreateUserRecord(userRepository.save(user), password);
  }

  public User retrieveUser(TypedId<UserId> userId) {
    return userRepository
        .findById(userId)
        .orElseThrow(() -> new RecordNotFoundException(Table.USER, userId));
  }

  public Optional<User> retrieveUserBySubjectRef(String subjectRef) {
    return userRepository.findBySubjectRef(subjectRef);
  }

  public List<User> retrieveUsersForBusiness(TypedId<BusinessId> businessId) {
    return userRepository.findByBusinessId(businessId);
  }

  public List<User> retrieveUsersByUsernameForBusiness(
      TypedId<BusinessId> businessId, RequiredEncryptedStringWithHash userName) {
    return userRepository.findByBusinessIdAndFirstNameLikeOrLastNameLike(
        businessId, userName, userName);
  }
}
