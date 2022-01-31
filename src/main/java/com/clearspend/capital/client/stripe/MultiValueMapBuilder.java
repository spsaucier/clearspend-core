package com.clearspend.capital.client.stripe;

import java.util.Arrays;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

public final class MultiValueMapBuilder {

  public static final String METADATA_KEY_FORMAT = "metadata[%s]";

  private final MultiValueMap<String, String> multiValueMap = new LinkedMultiValueMap<>();

  public static MultiValueMapBuilder builder() {
    return new MultiValueMapBuilder();
  }

  private MultiValueMapBuilder() {}

  public MultiValueMap<String, String> build() {
    return new LinkedMultiValueMap<>(multiValueMap);
  }

  public MultiValueMapBuilder add(String key, String... values) {
    multiValueMap.addAll(key, Arrays.stream(values).toList());
    return this;
  }

  public MultiValueMapBuilder add(String key, String value) {
    multiValueMap.add(key, value);
    return this;
  }

  public MultiValueMapBuilder addMetadata(StripeMetadataEntry entry, Object value) {
    return value != null ? addMetadata(entry, value.toString()) : this;
  }

  public MultiValueMapBuilder addMetadata(StripeMetadataEntry entry, String value) {
    if (value != null) {
      multiValueMap.add(METADATA_KEY_FORMAT.formatted(entry.getKey()), value);
    }
    return this;
  }
}
