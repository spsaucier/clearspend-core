package com.tranwall.capital.controller;

import static java.util.UUID.randomUUID;

import com.github.javafaker.Faker;
import com.tranwall.capital.controller.type.Address;
import com.tranwall.capital.data.model.enums.Country;
import com.tranwall.capital.util.PhoneUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ControllerHelper {

  private final Faker faker = new Faker();

  public String generateFirstName() {
    return faker.name().firstName();
  }

  public String generateLastName() {
    return faker.name().lastName();
  }

  public String generateEmail() {
    return randomUUID() + "@tranwall.com";
  }

  public String generatePhone() {
    return PhoneUtil.randomPhoneNumber();
  }

  public String generatePassword() {
    return faker.internet().password(10, 32, true, true, true);
  }

  public String generateBusinessName() {
    return faker.company().name();
  }

  public Address generateAddress() {
    return new Address(
        faker.address().streetAddress(),
        faker.address().secondaryAddress(),
        faker.address().city(),
        faker.address().state(),
        faker.address().zipCode(),
        Country.USA);
  }
}
