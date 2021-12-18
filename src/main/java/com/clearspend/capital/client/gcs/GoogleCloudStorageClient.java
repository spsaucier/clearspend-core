package com.clearspend.capital.client.gcs;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.BucketInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.common.collect.Lists;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Profile("!test")
@Component
public class GoogleCloudStorageClient {

  private Bucket bucket;

  public GoogleCloudStorageClient(GoogleStorageProperties googleStorageProperties) {
    if (!googleStorageProperties.isEnabled()) {
      return;
    }

    // Instantiates a client
    try {
      GoogleCredentials credentials =
          GoogleCredentials.fromStream(
                  new ByteArrayInputStream(googleStorageProperties.getCredentials().getBytes()))
              .createScoped(Lists.newArrayList("https://www.googleapis.com/auth/cloud-platform"));
      Storage storage =
          StorageOptions.newBuilder().setCredentials(credentials).build().getService();

      if (googleStorageProperties.isCreateBucket()) {
        if (!storage.get(googleStorageProperties.getReceiptBucketName()).exists()) {
          // Attempts to create the bucket
          bucket = storage.create(BucketInfo.of(googleStorageProperties.getReceiptBucketName()));

          System.out.printf("Bucket %s created.%n", bucket.getName());
        } else {
          bucket = storage.get(googleStorageProperties.getReceiptBucketName());
        }
      }
    } catch (Exception e) {
      log.error("failed to connect to Google bucket");
      // TODO(kuchlein): determine if this is a legit result. Only really works if we're not running
      //  tests otherwise this just causes the tests to never complete
      // System.exit(-1);
    }
  }

  public void writeFile(String path, byte[] receiptFile) throws IOException {
    Blob blob = bucket.create(path, receiptFile);
    log.info("wrote: {} ({} bytes)", blob.getName(), blob.getSize());
  }

  public byte[] readFile(String path) {
    log.info("reading from {}", path);
    byte[] bytes = bucket.get(path).getContent();
    log.info("read: {} ({} bytes)", path, bytes.length);

    return bytes;
  }
}
