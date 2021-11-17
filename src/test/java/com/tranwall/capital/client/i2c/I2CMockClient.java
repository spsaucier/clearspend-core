package com.tranwall.capital.client.i2c;

import com.github.javafaker.Faker;
import com.tranwall.capital.client.i2c.response.AddCardResponse;
import com.tranwall.capital.client.i2c.response.AddStakeholderResponse;
import com.tranwall.capital.client.i2c.response.CreditFundsResponse;
import com.tranwall.capital.client.i2c.response.ShareFundsResponse;
import com.tranwall.capital.data.model.enums.CardType;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.NonNull;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Profile("test")
@Component
public class I2CMockClient extends I2Client {
  private final Faker faker = new Faker();

  public I2CMockClient(I2ClientProperties properties) {
    super(null, properties, null);
  }

  @Override
  public AddStakeholderResponse addStakeholder(@NonNull String name, String parentStakeholderId) {
    AddStakeholderResponse response = new AddStakeholderResponse();
    response.setI2cStakeholderRef(UUID.randomUUID().toString());
    response.setI2cAccountRef(UUID.randomUUID().toString());

    return response;
  }

  @Override
  public AddCardResponse addCard(CardType cardType, String nameOnCard) {
    AddCardResponse response = new AddCardResponse();

    response.setI2cCardRef(UUID.randomUUID().toString());
    response.setCardNumber(
        CardNumber.builder()
            .number(faker.numerify("####"))
            .expiryDate(faker.numerify("1#202#"))
            .build());

    return response;
  }

  @Override
  public CreditFundsResponse creditFunds(String i2cAccountRef, BigDecimal amount) {
    CreditFundsResponse response = new CreditFundsResponse();
    response.setBalance(amount);
    response.setLedgerBalance(amount);

    return response;
  }

  @Override
  public ShareFundsResponse shareFunds(
      String fromI2cAccountRef, String toI2cAccountRef, BigDecimal amount) {

    ShareFundsResponse response = new ShareFundsResponse();
    response.setFromCardBalance(new BigDecimal(faker.numerify("1##.##")));
    response.setToCardBalance(new BigDecimal(faker.numerify("1##.##")));

    return response;
  }
}
