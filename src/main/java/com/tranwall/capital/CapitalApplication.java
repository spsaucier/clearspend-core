package com.tranwall.capital;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients
@SpringBootApplication
public class CapitalApplication {

  public static void main(String[] args) {
    SpringApplication.run(CapitalApplication.class, args);
  }
}
