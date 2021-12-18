package com.tranwall.capital.client.clearbit;

import static org.assertj.core.api.Assertions.assertThat;

import com.tranwall.capital.BaseCapitalTest;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@RequiredArgsConstructor(onConstructor = @__({@Autowired}))
public class ClearbitClientTest extends BaseCapitalTest {

  private final ClearbitClient clearbitClient;

  @Test
  @Disabled
  void getOracleLogo() {
    String logo = clearbitClient.getLogo("oracle");

    assertThat(logo).isNotEmpty();
  }
}
