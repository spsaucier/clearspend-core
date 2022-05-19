package com.clearspend.capital.service;

import com.clearspend.capital.crypto.PasswordUtil;
import com.clearspend.capital.data.model.User;
import com.clearspend.capital.data.model.enums.UserType;
import com.clearspend.capital.data.repository.UserRepository;
import com.clearspend.capital.data.repository.business.BusinessRepository;
import com.clearspend.capital.service.FusionAuthService.CapitalChangePasswordReason;
import com.clearspend.capital.service.FusionAuthService.FusionAuthUserCreator;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/** Exists to solve a circular dependency between UserService and CardService. */
@Service
@RequiredArgsConstructor
public class UserWelcomeService {
  private final BusinessRepository businessRepository;
  private final UserRepository userRepository;
  private final CoreFusionAuthService fusionAuthService;
  private final TwilioService twilioService;
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
}
