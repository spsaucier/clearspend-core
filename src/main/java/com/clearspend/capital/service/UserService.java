package com.clearspend.capital.service;

import com.clearspend.capital.client.stripe.StripeClient;
import com.clearspend.capital.common.data.model.Address;
import com.clearspend.capital.common.error.RecordNotFoundException;
import com.clearspend.capital.common.error.Table;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.crypto.HashUtil;
import com.clearspend.capital.crypto.PasswordUtil;
import com.clearspend.capital.crypto.data.model.embedded.NullableEncryptedStringWithHash;
import com.clearspend.capital.crypto.data.model.embedded.RequiredEncryptedStringWithHash;
import com.clearspend.capital.data.model.User;
import com.clearspend.capital.data.model.enums.UserType;
import com.clearspend.capital.data.model.enums.card.CardType;
import com.clearspend.capital.data.repository.UserRepository;
import com.clearspend.capital.data.repository.UserRepositoryCustom.FilteredUserWithCardListRecord;
import com.clearspend.capital.data.repository.business.BusinessRepository;
import com.clearspend.capital.service.FusionAuthService.FusionAuthUserCreator;
import com.clearspend.capital.service.FusionAuthService.FusionAuthUserModifier;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;
import javax.annotation.Nullable;
import javax.transaction.Transactional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
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

  /**
   * If the user has not already been assigned a subjectRef, generate a random password and send
   * them a welcome email to get started. This happens in some scenarios assigning a card to a user.
   * If there is already a subjectRef on the User, this call has no effect.
   *
   * @param user The user to welcome
   * @return The new User object reloaded from the repository, or the same if unchanged
   */
  @FusionAuthUserCreator(
      reviewer = "jscarbor",
      explanation = "User Service manages the sync between users and FA users")
  User sendWelcomeEmailIfNeeded(User user, CardType cardType) {
    if (StringUtils.isEmpty(user.getSubjectRef())) {
      String password = PasswordUtil.generatePassword();
      user.setSubjectRef(
          fusionAuthService
              .createUser(
                  user.getBusinessId(), user.getId(), user.getEmail().getEncrypted(), password)
              .toString());

      user = userRepository.save(user);

      switch (cardType) {
        case PHYSICAL -> twilioService.sendCardIssuedPhysicalNotifyUserEmail(
            user.getEmail().getEncrypted(),
            user.getFirstName().getEncrypted(),
            businessRepository.getById(user.getBusinessId()).getLegalName(),
            password);
        case VIRTUAL -> twilioService.sendCardIssuedVirtualNotifyUserEmail(
            user.getEmail().getEncrypted(),
            user.getFirstName().getEncrypted(),
            businessRepository.getById(user.getBusinessId()).getLegalName(),
            password);
      }
    }
    return user;
  }

  public record CreateUpdateUserRecord(User user, String password) {}

  /** Creates a user for an existing fusion auth user, happens during initial onboarding */
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
    User user =
        new User(
            businessId,
            type,
            new RequiredEncryptedStringWithHash(firstName),
            new RequiredEncryptedStringWithHash(lastName),
            new RequiredEncryptedStringWithHash(email));
    if (phone != null) {
      user.setPhone(new NullableEncryptedStringWithHash(phone));
    }
    user.setId(userId);
    user.setAddress(address);
    user.setSubjectRef(subjectRef);

    user = userRepository.save(user);
    userRepository.flush();
    return user;
  }

  /** Creates a new user without a corresponding fusion auth user, for regular employee */
  @Transactional
  public CreateUpdateUserRecord createUser(
      TypedId<BusinessId> businessId,
      UserType type,
      String firstName,
      String lastName,
      @Nullable Address address,
      String email,
      String phone) {

    User user =
        new User(
            businessId,
            type,
            new RequiredEncryptedStringWithHash(firstName),
            new RequiredEncryptedStringWithHash(lastName),
            new RequiredEncryptedStringWithHash(email));
    if (phone != null) {
      user.setPhone(new NullableEncryptedStringWithHash(phone));
    }
    user.setAddress(address);

    user = userRepository.save(user);
    userRepository.flush();
    return new CreateUpdateUserRecord(user, "");
  }

  /** Creates a new user with a corresponding fusion auth user, currently only for tests */
  @FusionAuthUserCreator(
      reviewer = "jscarbor",
      explanation = "Keeping User and FusionAuth records in sync by way of UserService")
  @Transactional
  public CreateUpdateUserRecord createUserAndFusionAuthRecord(
      TypedId<BusinessId> businessId,
      UserType type,
      String firstName,
      String lastName,
      @Nullable Address address,
      String email,
      String phone) {

    CreateUpdateUserRecord userRecord =
        createUser(businessId, type, firstName, lastName, address, email, phone);

    User user = userRecord.user;
    String password = PasswordUtil.generatePassword();
    user.setSubjectRef(
        fusionAuthService
            .createUser(businessId, user.getId(), user.getEmail().getEncrypted(), password)
            .toString());
    user = userRepository.save(user);
    userRepository.flush();
    return new CreateUpdateUserRecord(user, password);
  }

  @FusionAuthUserModifier(
      reviewer = "jscarbor",
      explanation = "Keeping User and FusionAuth records in sync by way of UserService")
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

  public Optional<User> retrieveUserByEmail(String email) {
    return userRepository.findByEmailHash(HashUtil.calculateHash(email));
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

  public byte[] createCSVFile(
      TypedId<BusinessId> businessId, UserFilterCriteria userFilterCriteria) {

    Page<FilteredUserWithCardListRecord> userPage =
        userRepository.find(businessId, userFilterCriteria);

    List<String> headerFields = Arrays.asList("Employee", "Card Info", "Email Address");
    ByteArrayOutputStream csvFile = new ByteArrayOutputStream();
    try (CSVPrinter csvPrinter = new CSVPrinter(new PrintWriter(csvFile), CSVFormat.DEFAULT)) {
      csvPrinter.printRecord(headerFields);
      userPage
          .getContent()
          .forEach(
              record -> {
                User user = record.user();
                String employee = user.getFirstName() + " " + user.getLastName();
                String emailAddress = user.getEmail().getEncrypted();
                StringJoiner cardsList = new StringJoiner(";");

                if (record.card() != null) {
                  record
                      .card()
                      .forEach(
                          card ->
                              cardsList.add(
                                  "**** "
                                      + card.card().getLastFour()
                                      + " "
                                      + card.allocationName()
                                      + " "));
                } else {
                  cardsList.add("No cards");
                }

                try {
                  csvPrinter.printRecord(
                      Arrays.asList(employee, cardsList.toString(), emailAddress));
                } catch (IOException e) {
                  throw new RuntimeException(e.getMessage());
                }
              });
      csvPrinter.flush();
    } catch (IOException e) {
      throw new RuntimeException(e.getMessage());
    }
    return csvFile.toByteArray();
  }
}
