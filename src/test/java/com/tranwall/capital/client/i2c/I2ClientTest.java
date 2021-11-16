package com.tranwall.capital.client.i2c;

import static org.assertj.core.api.Assertions.assertThat;

import com.tranwall.capital.BaseCapitalTest;
import com.tranwall.capital.client.i2c.response.AddCardResponse;
import com.tranwall.capital.client.i2c.response.AddStakeholderResponse;
import com.tranwall.capital.data.model.enums.CardStatus;
import com.tranwall.capital.data.model.enums.CardType;
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
}
