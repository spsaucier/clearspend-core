package com.clearspend.capital.service;

import com.clearspend.capital.common.error.RecordNotFoundException;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.data.model.User;
import com.clearspend.capital.data.model.business.BusinessOwner;
import com.clearspend.capital.data.model.business.BusinessProspect;
import com.clearspend.capital.data.model.business.TosAcceptance;
import com.clearspend.capital.data.model.enums.UserType;
import com.clearspend.capital.permissioncheck.annotations.OpenAccessAPI;
import com.clearspend.capital.service.BusinessProspectService.OnboardingBusinessProspectMethod;
import com.clearspend.capital.service.type.CurrentUser;
import com.google.common.annotations.VisibleForTesting;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TermsAndConditionsService {

  private final UserService userService;
  private final BusinessOwnerService businessOwnerService;
  private final BusinessService businessService;
  private final BusinessProspectService businessProspectService;
  private static final Pattern timestampPattern =
      Pattern.compile("[A-Za-z]+ [A-Za-z]+ [0-9 ]?\\d \\d{4} \\d{1,2}:\\d{1,2}:\\d{1,2}");

  public record TermsAndConditionsRecord(
      TypedId<?> userId,
      LocalDateTime acceptedTimestampByUser,
      boolean isAcceptedTermsAndConditions,
      LocalDateTime maxDocumentTimestamp) {}

  private LocalDateTime getDocumentTimestamp(String url) {
    DateTimeFormatter f = DateTimeFormatter.ofPattern("EEE MMM dd yyyy HH:mm:ss");
    String document = getUrlContents(url, 1000);
    Matcher matcher = timestampPattern.matcher(document.substring(0, 80));
    LocalDateTime dateTime = null;
    if (matcher.find()) {
      String documentTimestampString = matcher.group();
      dateTime = LocalDateTime.parse(documentTimestampString, f);
    }
    return dateTime;
  }

  /***
   * Fetches document timestamp from two urls and getting the latest from two
   * @return the most recent timestamp of the terms and privacy documents
   */
  @OpenAccessAPI(
      reviewer = "Craig Miller",
      explanation =
          "Terms & Conditions is a part of onboarding, and this just returns public info and when the user accepted them.")
  @OnboardingBusinessProspectMethod(
      reviewer = "George Buduianu",
      explanation = "This can be called during onboarding")
  public TermsAndConditionsRecord userAcceptedTermsAndConditions() {
    LocalDateTime privacyPolicyTimestamp =
        getDocumentTimestamp("https://www.clearspend.com/privacy");
    LocalDateTime termsTimestamp = getDocumentTimestamp("https://www.clearspend.com/terms");

    TosAcceptance tos;
    TypedId<?> id;

    try {
      User user = userService.retrieveUserForService(CurrentUser.getUserId());
      tos = user.getTosAcceptance();
      id = user.getId();
    } catch (RecordNotFoundException recordNotFoundException) {
      // If we don't have a user yet but user accepted the TOS at the start of onboarding.
      // Reason for this case, if the user don't finish prospect steps and close the page
      BusinessProspect businessProspect =
          businessProspectService.retrieveBusinessProspect(
              new TypedId<>(Objects.requireNonNull(CurrentUser.getUserId()).toUuid()));
      tos = businessProspect.getTosAcceptance();
      id = businessProspect.getBusinessOwnerId();
    }

    log.debug(
        "termsAndConditions : {}",
        Optional.ofNullable(tos).map(TosAcceptance::getDate).orElse(OffsetDateTime.MIN));

    LocalDateTime maxDocumentTimestamp = max(termsTimestamp, privacyPolicyTimestamp);
    boolean isAcceptedTermsAndConditions =
        Optional.ofNullable(tos)
            .map(
                d ->
                    OffsetDateTime.of(maxDocumentTimestamp, ZoneOffset.UTC)
                        .truncatedTo(ChronoUnit.MICROS)
                        .isBefore(d.getDate()))
            .orElse(false);

    return new TermsAndConditionsRecord(
        id,
        Optional.ofNullable(tos)
            .map(tosAcceptance -> tosAcceptance.getDate().toLocalDateTime())
            .orElse(null),
        isAcceptedTermsAndConditions,
        maxDocumentTimestamp);
  }

  @VisibleForTesting
  LocalDateTime max(LocalDateTime timeA, LocalDateTime timeB) {
    LocalDateTime max = Optional.ofNullable(timeB).orElse(timeA);
    if (timeA.isAfter(max)) {
      max = timeA;
    }
    return max;
  }

  @SneakyThrows
  private String getUrlContents(String theUrl, int maxChars) {
    // TODO use a fancier connection pool for this
    StringBuilder content = new StringBuilder();
    URL url = new URL(theUrl);
    URLConnection urlConnection = url.openConnection();
    BufferedReader bufferedReader =
        new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
    String line;
    while (((line = bufferedReader.readLine()) != null) && (content.length() < maxChars)) {
      content.append(line);
      content.append('\n');
    }
    bufferedReader.close();
    content.setLength(Math.min(maxChars, content.length()));
    return content.toString();
  }

  @Transactional
  @OpenAccessAPI(
      explanation =
          "This is a part of our onboarding, and it can only accept for the currently authenticated user.",
      reviewer = "Craig Miller")
  @OnboardingBusinessProspectMethod(
      reviewer = "George Buduianu",
      explanation = "This can to be called during onboarding")
  public void acceptTermsAndConditions(
      TypedId<UserId> userId, String clientIp, String clientUserAgent) {
    TosAcceptance tosAcceptance =
        new TosAcceptance(
            OffsetDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS),
            clientIp,
            clientUserAgent);
    try {
      User user = userService.retrieveUser(userId);
      if (UserType.BUSINESS_OWNER == user.getType()) {
        Optional<BusinessOwner> businessOwner =
            businessOwnerService.retrieveBusinessOwnerByEmail(user.getEmail().getEncrypted());
        if (businessOwner.isPresent()
            && BooleanUtils.isTrue(businessOwner.get().getRelationshipRepresentative())) {
          businessService.updateBusinessTosAcceptance(user.getBusinessId(), tosAcceptance);
        }
      }

      userService.acceptTermsAndConditions(userId, tosAcceptance);
    } catch (RecordNotFoundException userNotFound) {
      // If we don't have a user yet but user accepted the TOS at the start of onboarding.
      // Reason for this case, if the user don't finish prospect steps and close the page
      BusinessProspect businessProspect =
          businessProspectService.retrieveBusinessProspect(
              new TypedId<>(Objects.requireNonNull(userId).toUuid()));

      businessProspectService.acceptTermsAndConditions(businessProspect.getId(), tosAcceptance);
    }
  }
}
