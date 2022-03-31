package com.clearspend.capital.service;

import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.data.model.User;
import com.clearspend.capital.data.repository.UserRepository;
import com.clearspend.capital.service.type.CurrentUser;
import com.google.common.annotations.VisibleForTesting;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
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
  public TermsAndConditionsRecord userAcceptedTermsAndConditions() {
    LocalDateTime privacyPolicyTimestamp =
        getDocumentTimestamp("https://www.clearspend.com/privacy");
    LocalDateTime termsTimestamp = getDocumentTimestamp("https://www.clearspend.com/terms");

    User user = userService.retrieveUserForService(CurrentUser.getUserId());
    log.debug("termsAndConditions : {}", user.getTermsAndConditionsAcceptanceTimestamp());

    LocalDateTime maxDocumentTimestamp = max(termsTimestamp, privacyPolicyTimestamp);
    boolean isAcceptedTermsAndConditions =
        user.getTermsAndConditionsAcceptanceTimestamp().isAfter(maxDocumentTimestamp);

    return new TermsAndConditionsRecord(
        user.getId(),
        user.getTermsAndConditionsAcceptanceTimestamp(),
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

  public void acceptTermsAndConditions() {
    userService.acceptTermsAndConditions(CurrentUser.getUserId());
  }
}
