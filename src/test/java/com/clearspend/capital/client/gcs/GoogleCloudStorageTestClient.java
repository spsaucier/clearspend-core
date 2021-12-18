package com.clearspend.capital.client.gcs;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Slf4j
@Profile("test")
@Configuration
public class GoogleCloudStorageTestClient {

  @Autowired private final GoogleStorageProperties googleStorageProperties;
  private final Map<String, byte[]> receiptMap = new HashMap<>();

  public GoogleCloudStorageTestClient(GoogleStorageProperties googleStorageProperties) {
    this.googleStorageProperties = googleStorageProperties;
  }

  @Bean
  public GoogleCloudStorageClient googleCloudStorageClient() {
    return new GoogleCloudStorageClient(googleStorageProperties) {
      @Override
      public void writeFile(String path, byte[] receiptFile) throws IOException {
        receiptMap.put(path, receiptFile);
        log.info("(test) wrote: {} ({} bytes)", path, receiptFile.length);
      }

      @Override
      public byte[] readFile(String path) {
        byte[] bytes = receiptMap.get(path);
        log.info("(test) read: {} ({} bytes)", path, bytes.length);
        return bytes;
      }
    };
  }
}
