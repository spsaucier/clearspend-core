package com.clearspend.capital.service;

import com.clearspend.capital.client.stripe.StripeClient;
import com.clearspend.capital.common.data.model.Address;
import com.clearspend.capital.common.error.RecordNotFoundException;
import com.clearspend.capital.common.error.RecordNotFoundException.Table;
import com.clearspend.capital.common.typedid.data.BusinessId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.crypto.PasswordUtil;
import com.clearspend.capital.crypto.data.model.embedded.NullableEncryptedStringWithHash;
import com.clearspend.capital.crypto.data.model.embedded.RequiredEncryptedStringWithHash;
import com.clearspend.capital.data.model.Business;
import com.clearspend.capital.data.model.User;
import com.clearspend.capital.data.model.enums.UserType;
import com.clearspend.capital.data.repository.BusinessRepository;
import com.clearspend.capital.data.repository.UserRepository;
import com.clearspend.capital.data.repository.UserRepositoryCustom.FilteredUserWithCardListRecord;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import javax.transaction.Transactional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

  private final BusinessRepository businessRepository;
  private final UserRepository userRepository;

  private final FusionAuthService fusionAuthService;
  private final TwilioService twilioService;
  private final StripeClient stripeClient;

  public record CreateUpdateUserRecord(User user, String password) {}

  /** Creates a user for an existing fusion auth user */
  @Transactional
  public User createUserForFusionAuthUser(
      @NonNull TypedId<UserId> userId,
      TypedId<BusinessId> businessId,
      UserType type,
      String firstName,
      String lastName,
      @Nullable Address address,
      String email,
      String phone,
      @NonNull String subjectRef) {
    Business business =
        businessRepository
            .findById(businessId)
            .orElseThrow(() -> new RecordNotFoundException(Table.BUSINESS, businessId));

    User user =
        new User(
            businessId,
            type,
            new RequiredEncryptedStringWithHash(firstName),
            new RequiredEncryptedStringWithHash(lastName),
            new RequiredEncryptedStringWithHash(email),
            new NullableEncryptedStringWithHash(phone));
    user.setId(userId);
    user.setAddress(address);
    user.setSubjectRef(subjectRef);

    user = userRepository.save(user);
    userRepository.flush();

    user.setExternalRef(stripeClient.createCardholder(user, business.getClearAddress()).getId());

    return user;
  }

  /** Creates a new user and a corresponding fusion auth user */
  @Transactional
  public CreateUpdateUserRecord createUser(
      TypedId<BusinessId> businessId,
      UserType type,
      String firstName,
      String lastName,
      @Nullable Address address,
      String email,
      String phone) {
    String password = PasswordUtil.generatePassword();

    Business business =
        businessRepository
            .findById(businessId)
            .orElseThrow(() -> new RecordNotFoundException(Table.BUSINESS, businessId));

    User user =
        new User(
            businessId,
            type,
            new RequiredEncryptedStringWithHash(firstName),
            new RequiredEncryptedStringWithHash(lastName),
            new RequiredEncryptedStringWithHash(email),
            new NullableEncryptedStringWithHash(phone));
    user.setAddress(address);

    user = userRepository.save(user);
    userRepository.flush();

    user.setSubjectRef(
        fusionAuthService.createUser(businessId, user.getId(), email, password).toString());
    user.setExternalRef(stripeClient.createCardholder(user, business.getClearAddress()).getId());

    twilioService.sendNotificationEmail(
        user.getEmail().getEncrypted(),
        String.format("Welcome to ClearSpend, your password is %s", password));

    return new CreateUpdateUserRecord(user, password);
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

  public Page<FilteredUserWithCardListRecord> retrieveUserPage(
      TypedId<BusinessId> businessId, UserFilterCriteria userFilterCriteria) {
    return userRepository.find(businessId, userFilterCriteria);
  }

  public boolean archiveUser(TypedId<BusinessId> businessId, TypedId<UserId> userId) {
    User user =
        userRepository
            .findByBusinessIdAndId(businessId, userId)
            .orElseThrow(() -> new RecordNotFoundException(Table.USER, userId));
    user.setArchived(true);
    return userRepository.save(user).isArchived();
  }
}
