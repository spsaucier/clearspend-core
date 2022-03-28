package com.clearspend.capital.service;

import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.controller.type.termsAndConditions.TermsAndConditionsResponse;
import com.clearspend.capital.data.model.User;
import com.clearspend.capital.data.repository.UserRepository;
import com.clearspend.capital.service.type.CurrentUser;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TermsAndConditionsService {

  private final UserService userService;
  private final UserRepository userRepository;
  private static final Pattern timestampPattern =
      Pattern.compile("[A-Za-z]+ [A-Za-z]+ [0-9 ]?\\d \\d{4} \\d{1,2}:\\d{1,2}:\\d{1,2}");

  public record TermsAndConditionsRecord(
      TypedId<UserId> userId,
      LocalDateTime acceptedTimestampByUser,
      boolean isAcceptedTermsAndConditions,
      LocalDateTime maxDocumentTimestamp) {}

  /***
   * Fetches document timestamp from two urls and getting the latest from two
   * @return
   */
  public TermsAndConditionsResponse getTermsAndConditionsTimestampDetails() {
    String terms = getUrlContents("https://www.clearspend.com/terms");
    String privacyPolicy = getUrlContents("https://www.clearspend.com/privacy");
    Matcher termsMatcher = timestampPattern.matcher(terms.substring(0, 80));
    Matcher privacyPolicyMatcher = timestampPattern.matcher(privacyPolicy.substring(0, 80));
    DateTimeFormatter f = DateTimeFormatter.ofPattern("EEE MMM dd yyyy HH:mm:ss");
    LocalDateTime termsTimestamp = null;
    LocalDateTime privacyPolicyTimestamp = null;
    if (termsMatcher.find()) {
      String extractTermsTimestamp = termsMatcher.group();
      termsTimestamp = LocalDateTime.parse(extractTermsTimestamp, f);
    }
    if (privacyPolicyMatcher.find()) {
      String extractPolicyTimestamp = privacyPolicyMatcher.group();
      privacyPolicyTimestamp = LocalDateTime.parse(extractPolicyTimestamp, f);
    }
    return TermsAndConditionsResponse.of(
        compareDocumentTimestampWithUserAcceptanceTimestamp(
            calculateMaxTimestamp(termsTimestamp, privacyPolicyTimestamp)));
  }

  public LocalDateTime calculateMaxTimestamp(
      LocalDateTime termsTimestamp, LocalDateTime privacyPolicyTimestamp) {
    LocalDateTime maxDocumentTimestamp;
    if (termsTimestamp.isAfter(privacyPolicyTimestamp)) {
      maxDocumentTimestamp = termsTimestamp;
    } else {
      maxDocumentTimestamp = privacyPolicyTimestamp;
    }
    return maxDocumentTimestamp;
  }

  /**
   * Checks the document timestamp with user's accepted timestamp
   *
   * @param maxDocumentTimestamp
   * @return
   */
  public TermsAndConditionsRecord compareDocumentTimestampWithUserAcceptanceTimestamp(
      LocalDateTime maxDocumentTimestamp) {
    User user = userService.retrieveUserForService(CurrentUser.getUserId());
    user.getTermsAndConditionsAcceptanceTimestamp();
    log.debug("termsAndConditions : {}", user.getTermsAndConditionsAcceptanceTimestamp());
    boolean isAcceptedTermsAndConditions = false;
    if (user.getTermsAndConditionsAcceptanceTimestamp().isAfter(maxDocumentTimestamp)) {
      isAcceptedTermsAndConditions = true;
    }
    return new TermsAndConditionsRecord(
        user.getId(),
        user.getTermsAndConditionsAcceptanceTimestamp(),
        isAcceptedTermsAndConditions,
        maxDocumentTimestamp);
  }

  public String getUrlContents(String theUrl) {
    StringBuilder content = new StringBuilder();
    try {
      URL url = new URL(theUrl);
      URLConnection urlConnection = url.openConnection();
      BufferedReader bufferedReader =
          new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
      String line;
      while ((line = bufferedReader.readLine()) != null) {
        content.append(line + "\n");
      }
      bufferedReader.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
    return content.toString();
  }

  public void acceptTermsAndConditionsTimestamp() {
    User user = userService.retrieveUserForService(CurrentUser.getUserId());
    user.setTermsAndConditionsAcceptanceTimestamp(LocalDateTime.now());
    userRepository.save(user);
    userRepository.flush();
    log.debug("User: {}", user);
  }
}
