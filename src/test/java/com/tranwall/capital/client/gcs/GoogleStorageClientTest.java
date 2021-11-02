package com.tranwall.capital.client.gcs;

import com.tranwall.capital.BaseCapitalTest;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class GoogleStorageClientTest extends BaseCapitalTest {

  @Autowired private GoogleCloudStorageClient client;

  @Test
  @SneakyThrows
  public void hello_world_success() {
    client.writeFile("/hello", "world".getBytes());
  }
}
