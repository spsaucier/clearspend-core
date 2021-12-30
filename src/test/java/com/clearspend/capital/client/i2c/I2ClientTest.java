package com.clearspend.capital.client.i2c;

import static org.assertj.core.api.Assertions.assertThat;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.client.i2c.response.AddCardResponse;
import com.clearspend.capital.client.i2c.response.AddStakeholderResponse;
import com.clearspend.capital.client.i2c.response.CreditFundsResponse;
import com.clearspend.capital.client.i2c.response.ShareFundsResponse;
import com.clearspend.capital.data.model.enums.card.CardStatus;
import com.clearspend.capital.data.model.enums.card.CardType;
import java.math.BigDecimal;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@Disabled
public class I2ClientTest extends BaseCapitalTest {

  private static final String programId = "tranwall_virtual";
  private static final String cardBin = "601999";

  @Autowired private I2Client i2Client;

  @Test
  public void getCardStatusRequest() {
    // when
    CardStatus response = i2Client.getCardStatus("110651106363");

    // then
    assertThat(response).isNotNull();
  }

  @Test
  public void testAddStakeholder() {
    // given
    StakeholderInfo request =
        StakeholderInfo.builder()
            .programId(programId)
            .cardBin(cardBin)
            .stakeholderName("Test Me")
            .build();

    // when
    AddStakeholderResponse response = i2Client.addStakeholder("Test me");

    // then
    assertThat(response.getI2cStakeholderRef()).isNotNull();
    assertThat(response.getI2cAccountRef()).isNotNull();
  }

  @Test
  public void testAddCard() {
    // when
    AddCardResponse response = i2Client.addCard(CardType.VIRTUAL, "Virtual CardOwner");

    // then
    assertThat(response.getI2cCardRef()).isNotNull();
    assertThat(response.getCardNumber()).isNotNull();
  }

  @Test
  public void testCreditFunds() {
    // when
    BigDecimal amount = new BigDecimal("1231.33");
    CreditFundsResponse response = i2Client.creditFunds("110650933818", amount);

    // then
    assertThat(response.getBalance()).isGreaterThanOrEqualTo(amount);
  }

  @Test
  public void testShareFunds() {
    // given
    BigDecimal shareAmount = new BigDecimal("5.12");

    // when
    ShareFundsResponse response = i2Client.shareFunds("110650933818", "110650933949", shareAmount);

    // then
    assertThat(response.getFromCardBalance()).isNotNull();
    assertThat(response.getToCardBalance()).isNotNull();
  }
}
