package com.tranwall.capital.client.alloy.request;

import java.util.Arrays;

public enum DocumentExtension {
  PNG("image/png"),
  PDF("application/pdf"),
  JPG("image/jpeg");

  private String contentType;

  DocumentExtension(String contentType) {
    this.contentType = contentType;
  }

  public String getContentType() {
    return contentType;
  }

  public static DocumentExtension getExtensionByContentType(String contentType) {
    return Arrays.stream(DocumentExtension.values())
        .filter(t -> t.getContentType().equals(contentType))
        .findAny()
        .orElse(PDF);
  }
}
