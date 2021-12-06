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
import com.tranwall.capital.data.repository.UserRepositoryCustom;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

  private final UserRepository userRepository;

  private final FusionAuthService fusionAuthService;
  private final TwilioService twilioService;

  public record CreateUpdateUserRecord(User user, String password) {}

  @Transactional
  public CreateUpdateUserRecord createUser(
      TypedId<BusinessId> businessId,
      UserType type,
      String firstName,
      String lastName,
      @Nullable Address address,
      String email,
      String phone,
      boolean generatePassword,
      String subjectRef) {
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

    return new CreateUpdateUserRecord(userRepository.save(user), password);
  }

  @Transactional
  public CreateUpdateUserRecord updateUser(
      TypedId<BusinessId> businessId,
      TypedId<UserId> userId,
      String firstName,
      String lastName,
      @Nullable Address address,
      String email,
      String phone,
      boolean generatePassword) {

    User user = retrieveUser(userId);
    if (StringUtils.isNotEmpty(firstName)) {
      user.setFirstName(new RequiredEncryptedStringWithHash(firstName));
    }
    if (StringUtils.isNotEmpty(lastName)) {
      user.setLastName(new RequiredEncryptedStringWithHash(lastName));
    }
    if (StringUtils.isNotEmpty(email)) {
      user.setEmail(new RequiredEncryptedStringWithHash(email));
    }
    if (StringUtils.isNotEmpty(phone)) {
      user.setPhone(new NullableEncryptedStringWithHash(phone));
    }
    if (address != null) {
      user.setAddress(address);
    }

    String password = null;
    if (generatePassword) {
      password = PasswordUtil.generatePassword();
      user.setSubjectRef(
          fusionAuthService
              .updateUser(
                  businessId, user.getId(), email, password, user.getType(), user.getSubjectRef())
              .toString());
      twilioService.sendNotificationEmail(
          user.getEmail().getEncrypted(),
          String.format("Hello from ClearSpend, your new password is %s", password));
    }

    return new CreateUpdateUserRecord(userRepository.save(user), password);
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

  public List<User> retrieveUsersByIds(
      TypedId<BusinessId> businessId, List<TypedId<UserId>> userIds) {
    return userRepository.findByBusinessIdAndIdIn(businessId, userIds);
  }

  public Page<UserRepositoryCustom.FilteredUserWithCardListRecord> retrieveUserPage(
      TypedId<BusinessId> businessId, UserFilterCriteria userFilterCriteria) {
    return userRepository.find(businessId, userFilterCriteria);
  }
}
