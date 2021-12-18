package com.clearspend.capital.crypto;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;

@Slf4j
@UtilityClass
public class PasswordUtil {

  public static final int MINIMUM_PASSWORD_LENGTH = 10;

  public static String generatePassword() {
    String upperCaseLetters = RandomStringUtils.random(MINIMUM_PASSWORD_LENGTH, 65, 90, true, true);
    String lowerCaseLetters =
        RandomStringUtils.random(MINIMUM_PASSWORD_LENGTH, 97, 122, true, true);
    String numbers = RandomStringUtils.randomNumeric(MINIMUM_PASSWORD_LENGTH);
    String specialChar = RandomStringUtils.random(MINIMUM_PASSWORD_LENGTH, 33, 47, false, false);
    String totalChars = RandomStringUtils.randomAlphanumeric(MINIMUM_PASSWORD_LENGTH);
    String combinedChars =
        upperCaseLetters
            .concat(lowerCaseLetters)
            .concat(numbers)
            .concat(specialChar)
            .concat(totalChars);

    List<Character> pwdChars =
        combinedChars.chars().mapToObj(c -> (char) c).collect(Collectors.toList());
    Collections.shuffle(pwdChars);

    return pwdChars.stream()
        .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)
        .substring(0, MINIMUM_PASSWORD_LENGTH);
  }
}
