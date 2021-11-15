package com.tranwall.capital.client.i2c;

import com.tranwall.capital.client.i2c.response.AddStakeholderResponse;
import java.util.UUID;
import lombok.NonNull;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Profile("test")
@Component
public class I2CMockClient extends I2Client {

  public I2CMockClient(I2ClientProperties properties) {
    super(null, properties, null);
  }

  @Override
  public AddStakeholderResponse addStakeholder(@NonNull String name, String parentStakeholderId) {
    AddStakeholderResponse response = new AddStakeholderResponse();
    response.setStakeholderId(UUID.randomUUID().toString());

    return response;
  }
}
