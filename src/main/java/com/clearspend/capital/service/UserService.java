package com.clearspend.capital.service;

import com.clearspend.capital.client.stripe.StripeClient;
import com.clearspend.capital.common.data.model.Address;
import com.clearspend.capital.common.error.InvalidRequestException;
import com.clearspend.capital.common.error.RecordNotFoundException;
import com.clearspend.capital.common.error.Table;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.controller.type.user.UpdateUserRequest;
import com.clearspend.capital.crypto.HashUtil;
import com.clearspend.capital.crypto.PasswordUtil;
import com.clearspend.capital.crypto.data.model.embedded.NullableEncryptedPhoneWithHash;
import com.clearspend.capital.crypto.data.model.embedded.RequiredEncryptedEmailWithHash;
import com.clearspend.capital.crypto.data.model.embedded.RequiredEncryptedNameWithHash;
import com.clearspend.capital.crypto.data.model.embedded.WithEncryptedString;
import com.clearspend.capital.data.model.User;
import com.clearspend.capital.data.model.business.TosAcceptance;
import com.clearspend.capital.data.model.enums.UserType;
import com.clearspend.capital.data.model.enums.card.CardStatusReason;
import com.clearspend.capital.data.repository.UserRepository;
import com.clearspend.capital.data.repository.UserRepositoryCustom.FilteredUserWithCardListRecord;
import com.clearspend.capital.permissioncheck.annotations.SqlPermissionAPI;
import com.clearspend.capital.service.FusionAuthService.FusionAuthUserCreator;
import com.clearspend.capital.service.FusionAuthService.FusionAuthUserModifier;
import com.google.errorprone.annotations.RestrictedApi;
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
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PostFilter;
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

  public @interface FusionAuthUserLookup {
    String reviewer();

    String explanation();
  }

  public @interface TestDataUserOp {

    String reviewer();

    String explanation();
  }

  private final UserRepository userRepository;

  private final CoreFusionAuthService fusionAuthService;
  private final TwilioService twilioService;
  private final BusinessOwnerService businessOwnerService;
  private final CardService cardService;
  private final StripeClient stripeClient;

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
            new RequiredEncryptedNameWithHash(firstName),
            new RequiredEncryptedNameWithHash(lastName),
            new RequiredEncryptedEmailWithHash(email));
    if (phone != null) {
      user.setPhone(new NullableEncryptedPhoneWithHash(phone));
    }
    user.setId(userId);
    user.setAddress(address);
    user.setSubjectRef(subjectRef);
    user.setTosAcceptance(tosAcceptance);

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
            new RequiredEncryptedNameWithHash(firstName),
            new RequiredEncryptedNameWithHash(lastName),
            new RequiredEncryptedEmailWithHash(email));
    if (phone != null) {
      user.setPhone(new NullableEncryptedPhoneWithHash(phone));
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

  @FusionAuthUserModifier(
      reviewer = "jscarbor",
      explanation = "Keeping User and FusionAuth records in sync by way of UserService")
  @Transactional
  @PreAuthorize(
      "(isSelfOwned(#updateUserRequest)"
          + " and #updateUserRequest.email == null"
          + " and #updateUserRequest.firstName == null"
          + " and #updateUserRequest.lastName == null"
          + ")"
          + " or hasRootPermission(#updateUserRequest, 'MANAGE_USERS')")
  public CreateUpdateUserRecord updateUser(@NonNull final UpdateUserRequest updateUserRequest) {

    User user = retrieveUser(updateUserRequest.getUserId());
    if (user.isArchived()) {
      throw new InvalidRequestException("User has been archived");
    }
    if (!updateUserRequest.getBusinessId().equals(user.getBusinessId())) {
      throw new InvalidRequestException("Invalid Business ID");
    }
    if (isChanged(updateUserRequest.getFirstName(), user.getFirstName())) {
      user.setFirstName(new RequiredEncryptedNameWithHash(updateUserRequest.getFirstName()));
    }
    if (isChanged(updateUserRequest.getLastName(), user.getLastName())) {
      user.setLastName(new RequiredEncryptedNameWithHash(updateUserRequest.getLastName()));
    }

    String oldEmail = null;
    if (isChanged(updateUserRequest.getEmail(), user.getEmail())) {
      // Ensure that this email address does NOT already exist within the database.
      RequiredEncryptedEmailWithHash newEmail =
          new RequiredEncryptedEmailWithHash(updateUserRequest.getEmail());
      Optional<User> duplicate = userRepository.findByEmailHash(newEmail.getHash());
      if (duplicate.isPresent() && !duplicate.get().getId().equals(updateUserRequest.getUserId())) {
        throw new InvalidRequestException("A user with that email address already exists");
      }
      oldEmail = user.getEmail().getEncrypted();
      user.setEmail(newEmail);
    }
    if (isChanged(updateUserRequest.getPhone(), user.getPhone())) {
      user.setPhone(new NullableEncryptedPhoneWithHash(updateUserRequest.getPhone()));
    }
    final Address updatedAddress =
        Optional.ofNullable(updateUserRequest.getAddress())
            .map(com.clearspend.capital.controller.type.Address::toAddress)
            .orElse(null);
    if (user.getAddress() == null) {
      // If the current address is null change it to the new thing
      user.setAddress(updatedAddress);
    } else if (isChanged(updatedAddress, user.getAddress())) {
      user.setAddress(updatedAddress);
    }

    // The only things that can change for FusionAuth are email, password, and type.
    // The other two don't change with this call
    if (oldEmail != null && user.getSubjectRef() != null) {
      fusionAuthService.updateUser(user, null);
    }

    String password = null;
    if (updateUserRequest.isGeneratePassword()) {
      password = PasswordUtil.generatePassword();
      user.setSubjectRef(
          fusionAuthService
              .updateUser(
                  updateUserRequest.getBusinessId(),
                  user.getId(),
                  updateUserRequest.getEmail(),
                  password,
                  user.getType(),
                  user.getSubjectRef())
              .toString());
      twilioService.sendNotificationEmail(
          user.getEmail().getEncrypted(),
          String.format("Hello from ClearSpend, your new password is %s", password));
    }

    if (user.getType().equals(UserType.BUSINESS_OWNER)) {
      businessOwnerService.updateBusinessOwnerAndStripePerson(user);
    }

    if (user.getExternalRef() != null) {
      stripeClient.updateIndividualCardholder(user);
    }

    CreateUpdateUserRecord response =
        new CreateUpdateUserRecord(userRepository.save(user), password);

    // Tell the user at the old address that something changed (so they might fix it if necessary)
    String finalOldEmail = oldEmail;
    Optional.ofNullable(oldEmail)
        .ifPresent(
            e ->
                twilioService.sendUserDetailsUpdatedEmail(
                    finalOldEmail, user.getFirstName().getEncrypted()));

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

  @PostAuthorize("hasPermissionAnyAllocation(returnObject, 'READ|VIEW_OWN|CUSTOMER_SERVICE')")
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
      allowlistAnnotations = {LoginUserOp.class, FusionAuthUserLookup.class})
  public Optional<User> retrieveUserBySubjectRef(String subjectRef) {
    return userRepository.findBySubjectRef(subjectRef);
  }

  @PostFilter("hasPermissionAnyAllocation(filterObject, 'READ|VIEW_OWN|CUSTOMER_SERVICE')")
  public List<User> retrieveUsersForBusiness(TypedId<BusinessId> businessId) {
    return userRepository.findByBusinessId(businessId);
  }

  Optional<User> retrieveUserByEmail(String email) {
    return userRepository.findByEmailHash(HashUtil.calculateHash(email));
  }

  @SqlPermissionAPI
  public Page<FilteredUserWithCardListRecord> retrieveUserPage(
      TypedId<BusinessId> businessId, UserFilterCriteria userFilterCriteria) {
    return userRepository.find(businessId, userFilterCriteria);
  }

  @Transactional
  @PreAuthorize("hasRootPermission(#businessId, 'MANAGE_USERS|CUSTOMER_SERVICE')")
  public boolean archiveUser(final TypedId<BusinessId> businessId, final TypedId<UserId> userId) {
    final User user =
        userRepository
            .findByBusinessIdAndId(businessId, userId)
            .orElseThrow(() -> new RecordNotFoundException(Table.USER, userId));
    if (user.isArchived()) {
      throw new InvalidRequestException("User is already archived");
    }

    cardService
        .getNotCancelledCardsForUser(businessId, userId)
        // We do not want to risk a DB rollback once a card is cancelled in Stripe, so doing it
        // this way ensures a committed transaction for each card that is cancelled
        .forEach(card -> cardService.cancelCard(card, CardStatusReason.USER_ARCHIVED));

    user.setArchived(true);
    userRepository.save(user);

    // If it has a subject ref, that means the user is in FusionAuth and we need to remove it
    Optional.ofNullable(user.getSubjectRef())
        .filter(StringUtils::isNotBlank)
        .ifPresent(s -> fusionAuthService.deleteUser(user));
    return true;
  }

  @SqlPermissionAPI
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

  void acceptTermsAndConditions(TypedId<UserId> userId, TosAcceptance tosAcceptance) {
    User user = retrieveUserForService(userId);
    user.setTosAcceptance(tosAcceptance);
    userRepository.save(user);
    userRepository.flush();
  }
}
