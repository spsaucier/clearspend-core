package com.clearspend.capital.client.firebase;

import com.google.firebase.messaging.FirebaseMessaging;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("test")
public class FirebaseTestConfiguration {
  @Bean
  public FirebaseMessaging firebaseMessaging() {
    return Mockito.mock(FirebaseMessaging.class);
  }
}
