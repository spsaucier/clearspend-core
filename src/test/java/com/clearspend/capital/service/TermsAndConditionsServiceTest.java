package com.clearspend.capital.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.data.model.User;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.transaction.Transactional;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;

@Slf4j
@Transactional
public class TermsAndConditionsServiceTest extends BaseCapitalTest {
  private final Resource termsResource;
  private final Resource privacyPolicyResource;
  @Autowired private TermsAndConditionsService termsAndConditionsService;
  private static final Pattern timestampPattern =
      Pattern.compile("[A-Za-z]+ [A-Za-z]+ [0-9 ]?\\d \\d{4} \\d{1,2}:\\d{1,2}:\\d{1,2}");
  @Autowired private UserService userService;
  @Autowired TestHelper testHelper;

  public TermsAndConditionsServiceTest(
      @Value("classpath:termsAndConditions/terms.html") @NonNull Resource termsResource,
      @Value("classpath:termsAndConditions/privacyPolicy.html") @NonNull
          Resource privacyPolicyResource) {
    this.termsResource = termsResource;
    this.privacyPolicyResource = privacyPolicyResource;
  }

  @Test
  @SneakyThrows
  void getCorrectDocumentTimestamp() {
    TestHelper.CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    File termsFile = termsResource.getFile();
    File privacyPolicyFile = privacyPolicyResource.getFile();
    String termsContent = readFileContent(termsFile);
    String privacyPolicyContent = readFileContent(privacyPolicyFile);
    Matcher termsMatcher = timestampPattern.matcher(termsContent);
    Matcher privacyPolicyMatcher = timestampPattern.matcher(privacyPolicyContent);
    DateTimeFormatter f = DateTimeFormatter.ofPattern("EEE MMM dd yyyy HH:mm:ss");
    LocalDateTime termsTimestamp = null;
    LocalDateTime privacyPolicyTimestamp = null;
    if (termsMatcher.find()) {
      termsTimestamp = LocalDateTime.parse(termsMatcher.group(), f);
    }
    if (privacyPolicyMatcher.find()) {
      privacyPolicyTimestamp = LocalDateTime.parse(privacyPolicyMatcher.group(), f);
    }
    LocalDateTime documentTimestamp =
        termsAndConditionsService.calculateMaxDate(termsTimestamp, privacyPolicyTimestamp);
    log.info("documentTimestamp: {}", documentTimestamp);
    User user = userService.retrieveUser(createBusinessRecord.user().getId());
    user.setTermsAndConditionsAcceptanceTimestamp(
        LocalDateTime.of(2015, Month.JULY, 29, 19, 30, 40));
    log.info("userAcceptanceTimestamp: {}", user.getTermsAndConditionsAcceptanceTimestamp());
    assertThat(user.getTermsAndConditionsAcceptanceTimestamp().isAfter(documentTimestamp))
        .isFalse();
    // User who hasn't accepted T&C
    user.setTermsAndConditionsAcceptanceTimestamp(
        LocalDateTime.of(1970, Month.SEPTEMBER, 12, 00, 00, 00));
    assertThat(documentTimestamp.isAfter(user.getTermsAndConditionsAcceptanceTimestamp())).isTrue();
  }

  private String readFileContent(File file) {
    StringBuilder content = new StringBuilder();
    try (BufferedReader br = new BufferedReader(new FileReader(file))) {
      String line;
      while ((line = br.readLine()) != null) {
        content.append(line + "\n");
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return content.substring(0, 100);
  }
}
