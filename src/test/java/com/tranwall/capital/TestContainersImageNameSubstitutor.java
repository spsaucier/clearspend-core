package com.tranwall.capital;

import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.ImageNameSubstitutor;

public class TestContainersImageNameSubstitutor extends ImageNameSubstitutor {

  private static final String OS_ARCH = System.getProperty("os.arch");

  @Override
  public DockerImageName apply(DockerImageName original) {
    if (isArm64V8Architecture()) {
      return DockerImageName.parse(
          switch (original.asCanonicalNameString()) {
            case "fusionauth/fusionauth-app:1.30.2" -> "jerryhopper/fusionauth-app";
            case "postgres:13.4-alpine" -> "arm64v8/postgres";
            default -> original.asCanonicalNameString();
          });
    } else {
      return original;
    }
  }

  @Override
  protected String getDescription() {
    // used in logs
    return "Test containers image name substitutor for Tim Kuchlein and his new shiny Mac :)";
  }

  private boolean isArm64V8Architecture() {
    // TODO: A better way to detect it maybe?
    return "aarch64".equals(OS_ARCH);
  }
}
