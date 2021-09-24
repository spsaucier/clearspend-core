package com.tranwall.capital.clients.i2c;

import com.tranwall.capital.clients.i2c.request.GetCardStatusRequest;
import com.tranwall.capital.clients.i2c.request.GetCardStatusRequestRoot;
import com.tranwall.capital.clients.i2c.response.GetCardStatusResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class I2ClientTest {

    @Autowired
    private I2Client i2Client;

    @Test
    public void getCardStatusRequest() {
        //given
        GetCardStatusRequest request = GetCardStatusRequest.builder()
                .acquirer(getAcquirer())
                .card(Card.builder()
                        .number("9100100013092570")
                        .build())
                .build();

        //when
        GetCardStatusResponse response = i2Client.getCardStatusResponse(new GetCardStatusRequestRoot(request)).getResponse();

        //then
        assertThat(response).isNotNull();
    }

    private Acquirer getAcquirer() {
        return Acquirer.builder()
                .id("TranTest")
                .userId("TranTest")
                .password("754NM@l1LT9")
                .build();
    }
}
