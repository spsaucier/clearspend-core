package com.clearspend.capital.service;

import com.clearspend.capital.client.stripe.StripeClient;
import com.clearspend.capital.common.data.model.Address;
import com.clearspend.capital.common.error.InvalidRequestException;
import com.clearspend.capital.common.error.RecordNotFoundException;
import com.clearspend.capital.common.error.Table;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.crypto.HashUtil;
import com.clearspend.capital.crypto.PasswordUtil;
import com.clearspend.capital.crypto.data.model.embedded.NullableEncryptedStringWithHash;
import com.clearspend.capital.crypto.data.model.embedded.RequiredEncryptedStringWithHash;
import com.clearspend.capital.crypto.data.model.embedded.WithEncryptedString;
import com.clearspend.capital.data.model.User;
import com.clearspend.capital.data.model.business.TosAcceptance;
import com.clearspend.capital.data.model.enums.UserType;
import com.clearspend.capital.data.repository.UserRepository;
import com.clearspend.capital.data.repository.UserRepositoryCustom.FilteredUserWithCardListRecord;
import com.clearspend.capital.data.repository.business.BusinessRepository;
import com.clearspend.capital.service.FusionAuthService.CapitalChangePasswordReason;
import com.clearspend.capital.service.FusionAuthService.FusionAuthUserCreator;
import com.clearspend.capital.service.FusionAuthService.FusionAuthUserModifier;
import com.google.errorprone.annotations.RestrictedApi;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;
import javax.annotation.Nullable;
import javax.transaction.Transactional;
import javax.validation.constraints.NotNull;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

  public @interface LoginUserOp {

    String reviewer();

    String explanation();
  }

  public @interface TestDataUserOp {

    String reviewer();

    String explanation();
  }

  private final BusinessRepository businessRepository;
  private final UserRepository userRepository;

  private final FusionAuthService fusionAuthService;
  private final TwilioService twilioService;
  private final BusinessOwnerService businessOwnerService;
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
  User sendWelcomeEmailIfNeeded(User user) {
    if (StringUtils.isEmpty(user.getSubjectRef())) {
      String password = PasswordUtil.generatePassword();
      user.setSubjectRef(
          fusionAuthService
              .createUser(
                  user.getBusinessId(),
                  user.getId(),
                  user.getEmail().getEncrypted(),
                  password,
                  UserType.EMPLOYEE,
                  Optional.of(CapitalChangePasswordReason.Validation))
              .toString());

      user = userRepository.save(user);

      twilioService.sendUserAccountCreatedEmail(
          user.getEmail().getEncrypted(),
          user.getFirstName().getEncrypted(),
          businessRepository.getById(user.getBusinessId()).getLegalName(),
          password);
    }
    return user;
  }

  public record CreateUpdateUserRecord(User user, String password) {}

  /** Creates a user for an existing fusion auth user, happens during initial onboarding */
  @Transactional
  User createUserForFusionAuthUser(
      @NonNull TypedId<UserId> userId,
      TypedId<BusinessId> businessId,
      UserType type,
      String firstName,
      String lastName,
      @Nullable Address address,
      String email,
      String phone,
      @NonNull String subjectRef,
      TosAcceptance tosAcceptance) {
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
    user.setTermsAndConditionsAcceptanceTimestamp(tosAcceptance.getDate().toLocalDateTime());

    user = userRepository.save(user);
    userRepository.flush();
    return user;
  }

  /** Creates a new user without a corresponding fusion auth user, for regular employee */
  @Transactional
  @PreAuthorize("hasRootPermission(#businessId, 'MANAGE_USERS')")
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
    if (userRepository.findByEmailHash(HashUtil.calculateHash(email)).isPresent()) {
      throw new InvalidRequestException("A user with that email address already exists");
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
  @RestrictedApi(
      link =
          "https://tranwall.atlassian.net/wiki/spaces/CAP/pages/2088828965/Dev+notes+Service+method+security",
      explanation =
          "This method is exclusively used by the TestDataController in order to generate test users",
      allowlistAnnotations = {TestDataUserOp.class})
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

  /**
   * Make changes to the user
   *
   * @param businessId must match the user being modified
   * @param userId the user to modify
   * @param firstName the new first name
   * @param lastName the new last name
   * @param address the new address
   * @param email the new email address
   * @param phone the new phone number
   * @param generatePassword true to generate a new password (and send an email)
   * @return the new User and password
   */
  @FusionAuthUserModifier(
      reviewer = "jscarbor",
      explanation = "Keeping User and FusionAuth records in sync by way of UserService")
  @Transactional
  @PreAuthorize("isSelf(#userId) or hasRootPermission(#businessId, 'MANAGE_USERS')")
  public CreateUpdateUserRecord updateUser(
      @NotNull TypedId<BusinessId> businessId,
      @NotNull TypedId<UserId> userId,
      @Nullable String firstName,
      @Nullable String lastName,
      @Nullable Address address,
      @Nullable String email,
      @Nullable String phone,
      boolean generatePassword) {

    User user = retrieveUser(userId);
    if (!businessId.equals(user.getBusinessId())) {
      throw new IllegalArgumentException("businessId");
    }
    if (isChanged(firstName, user.getFirstName())) {
      user.setFirstName(new RequiredEncryptedStringWithHash(firstName));
    }
    if (isChanged(lastName, user.getLastName())) {
      user.setLastName(new RequiredEncryptedStringWithHash(lastName));
    }
    // TODO CAP-727 forbid changing email
    if (isChanged(email, user.getEmail())) {
      // Ensure that this email address does NOT already exist within the database.
      RequiredEncryptedStringWithHash newHash = new RequiredEncryptedStringWithHash(email);
      Optional<User> duplicate = userRepository.findByEmailHash(HashUtil.calculateHash(email));
      if (duplicate.isPresent() && !duplicate.get().getId().equals(userId)) {
        throw new InvalidRequestException("A user with that email address already exists");
      }
      user.setEmail(newHash);
    }
    if (isChanged(phone, user.getPhone())) {
      user.setPhone(new NullableEncryptedStringWithHash(phone));
    }
    if (isChanged(address, user.getAddress())) {
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

    if (user.getType().equals(UserType.BUSINESS_OWNER)) {
      businessOwnerService.updateBusinessOwnerAndStripePerson(user);
    } else if (user.getExternalRef() != null) {
      stripeClient.updateCardholder(user);
    }

    CreateUpdateUserRecord response =
        new CreateUpdateUserRecord(userRepository.save(user), password);
    twilioService.sendUserDetailsUpdatedEmail(
        user.getEmail().getEncrypted(), user.getFirstName().getEncrypted());
    return response;
  }

  private static boolean isChanged(Object newAddress, Object oldAddress) {
    return newAddress != null && !newAddress.equals(oldAddress);
  }

  private static boolean isChanged(String newValue, WithEncryptedString oldValue) {
    return StringUtils.isNotEmpty(newValue) && !oldValue.getEncrypted().equals(newValue);
  }

  @PostAuthorize("isSelf(returnObject.id) or hasRootPermission(returnObject, 'VIEW_OWN')")
  public User retrieveUser(TypedId<UserId> userId) {
    return retrieveUserForService(userId);
  }

  User retrieveUserForService(final TypedId<UserId> userId) {
    return userRepository
        .findById(userId)
        .orElseThrow(() -> new RecordNotFoundException(Table.USER, userId));
  }

  @RestrictedApi(
      link =
          "https://tranwall.atlassian.net/wiki/spaces/CAP/pages/2088828965/Dev+notes+Service+method+security",
      explanation =
          "This method is used to retrieve user data during the login flow, and no SecurityContext will be available.",
      allowlistAnnotations = {LoginUserOp.class})
  public Optional<User> retrieveUserBySubjectRef(String subjectRef) {
    return userRepository.findBySubjectRef(subjectRef);
  }

  @PreAuthorize(
      "hasRootPermission(#businessId, 'VIEW_OWN') or hasGlobalPermission('CUSTOMER_SERVICE')")
  public List<User> retrieveUsersForBusiness(TypedId<BusinessId> businessId) {
    return userRepository.findByBusinessId(businessId);
  }

  Optional<User> retrieveUserByEmail(String email) {
    return userRepository.findByEmailHash(HashUtil.calculateHash(email));
  }

  @PreAuthorize("hasRootPermission(#businessId, 'VIEW_OWN')")
  public Page<FilteredUserWithCardListRecord> retrieveUserPage(
      TypedId<BusinessId> businessId, UserFilterCriteria userFilterCriteria) {
    return userRepository.find(businessId, userFilterCriteria);
  }

  @PreAuthorize("hasRootPermission(#businessId, 'MANAGE_USERS')")
  public boolean archiveUser(TypedId<BusinessId> businessId, TypedId<UserId> userId) {
    User user =
        userRepository
            .findByBusinessIdAndId(businessId, userId)
            .orElseThrow(() -> new RecordNotFoundException(Table.USER, userId));
    user.setArchived(true);
    return userRepository.save(user).isArchived();
  }

  @PreAuthorize("hasRootPermission(#businessId, 'VIEW_OWN')")
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

  void acceptTermsAndConditions(TypedId<UserId> userId) {
    User user = retrieveUserForService(userId);
    user.setTermsAndConditionsAcceptanceTimestamp(LocalDateTime.now(ZoneOffset.UTC));
    userRepository.save(user);
    userRepository.flush();
  }
}
