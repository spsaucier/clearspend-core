package com.tranwall.capital.client.i2c;

import static org.assertj.core.api.Assertions.assertThat;

import com.tranwall.capital.BaseCapitalTest;
import com.tranwall.capital.client.i2c.response.AddStakeholderResponse;
import com.tranwall.capital.client.i2c.response.GetCardStatusResponse;
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
    // given
    Card card = Card.builder().number("9100100013092570").build();

    // when
    GetCardStatusResponse response = i2Client.getCardStatus(card);

    // then
    assertThat(response).isNotNull();
  }

  private Acquirer getAcquirer() {
    return Acquirer.builder().id("TW0001").userId("TWTest").password("{TWTest_Temp@/110}").build();
  }

  @Test
  public void testAddAccounholder() {
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
    assertThat(response).isNotNull();
  }
}
