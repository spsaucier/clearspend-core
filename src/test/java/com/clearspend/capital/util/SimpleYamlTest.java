package com.clearspend.capital.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

public class SimpleYamlTest {

  @SneakyThrows
  @Test
  void basicTestForFileInProjectRoot() {
    SimpleYaml y =
        new SimpleYaml(new BufferedInputStream(new FileInputStream("docker-compose.yml")));
    assertThat(((String) y.get("services.fusionauth.image")).contains("fusionauth/")).isTrue();
  }
}
