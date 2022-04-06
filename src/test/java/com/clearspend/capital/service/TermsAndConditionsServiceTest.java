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
import org.jetbrains.annotations.Nullable;
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
    testHelper.setCurrentUser(createBusinessRecord.user());

    LocalDateTime termsTimestamp = getTimestampFromResource(termsResource);
    LocalDateTime privacyPolicyTimestamp = getTimestampFromResource(privacyPolicyResource);
    LocalDateTime documentTimestamp =
        termsAndConditionsService.max(termsTimestamp, privacyPolicyTimestamp);
    log.info("documentTimestamp: {}", documentTimestamp);
    User user = userService.retrieveUser(createBusinessRecord.user().getId());

    testHelper.setCurrentUser(user);

    user.setTermsAndConditionsAcceptanceTimestamp(
        LocalDateTime.of(2015, Month.JULY, 29, 19, 30, 40));
    log.info("userAcceptanceTimestamp: {}", user.getTermsAndConditionsAcceptanceTimestamp());
    assertThat(user.getTermsAndConditionsAcceptanceTimestamp().isAfter(documentTimestamp))
        .isFalse();
    // User who hasn't accepted T&C
    user.setTermsAndConditionsAcceptanceTimestamp(
        LocalDateTime.of(1970, Month.SEPTEMBER, 12, 00, 00, 00));
    assertThat(documentTimestamp.isAfter(user.getTermsAndConditionsAcceptanceTimestamp())).isTrue();

    // User who just got created and hasn't accepted anything yet CAP-878
    User peon = testHelper.createUser(createBusinessRecord.business()).user();

    testHelper.setCurrentUser(peon);
    assertThat(peon.getTermsAndConditionsAcceptanceTimestamp()).isNull();
    assertThat(
            termsAndConditionsService
                .userAcceptedTermsAndConditions()
                .isAcceptedTermsAndConditions())
        .isFalse();
  }

  @Nullable
  private LocalDateTime getTimestampFromResource(Resource termsResource) throws IOException {
    DateTimeFormatter f = DateTimeFormatter.ofPattern("EEE MMM dd yyyy HH:mm:ss");
    File file = termsResource.getFile();
    String fileContent = readFileContent(file);
    Matcher matcher = timestampPattern.matcher(fileContent);
    LocalDateTime time = null;
    if (matcher.find()) {
      time = LocalDateTime.parse(matcher.group(), f);
    }
    return time;
  }

  @SneakyThrows
  private String readFileContent(File file) {
    StringBuilder content = new StringBuilder();
    try (BufferedReader br = new BufferedReader(new FileReader(file))) {
      String line;
      while ((line = br.readLine()) != null) {
        content.append(line + "\n");
      }
    }
    return content.substring(0, 100);
  }
}
