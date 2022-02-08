package com.clearspend.capital.client.gcs;

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
      public void writeFile(String path, byte[] receiptFile) {
        receiptMap.put(path, receiptFile);
        log.info("(test) wrote: {} ({} bytes)", path, receiptFile.length);
      }

      @Override
      public byte[] readFile(String path) {
        byte[] bytes = receiptMap.get(path);
        log.info("(test) read: {} ({} bytes)", path, bytes.length);
        return bytes;
      }

      @Override
      public boolean deleteFile(String path) {
        return !path.contains("false");
      }

      @Override
      public void writeOnboardFile(String path, byte[] onboardFile) {
        writeFile(path, onboardFile);
      }

      @Override
      public byte[] readOnboardFile(String path) {
        return readFile(path);
      }
    };
  }
}
