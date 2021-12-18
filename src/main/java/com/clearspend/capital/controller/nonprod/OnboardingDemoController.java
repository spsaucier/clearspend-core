package com.clearspend.capital.controller.nonprod;

import com.clearspend.capital.crypto.HashUtil;
import com.clearspend.capital.data.repository.BusinessOwnerRepository;
import com.clearspend.capital.data.repository.BusinessProspectRepository;
import com.clearspend.capital.data.repository.BusinessRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Profile("!prod")
@RestController
@RequestMapping("/non-production")
@RequiredArgsConstructor
public class OnboardingDemoController {
  private final BusinessProspectRepository prospectRepository;
  private final BusinessOwnerRepository businessOwnerRepository;
  private final BusinessRepository businessRepository;
  private final io.fusionauth.client.FusionAuthClient fusionAuthClient;

  @Transactional
  @DeleteMapping("/onboarding/{email}")
  public String deleteEmail(@PathVariable String email) {
    prospectRepository
        .findByEmailHash(HashUtil.calculateHash(email))
        .ifPresent(
            p -> {
              if (businessRepository.existsById(p.getBusinessId())) {
                businessRepository.deleteById(p.getBusinessId());
              }

              if (businessOwnerRepository.existsById(p.getBusinessOwnerId())) {
                businessOwnerRepository.deleteById(p.getBusinessOwnerId());
              }

              if (p.getSubjectRef() != null) {
                fusionAuthClient.deleteUser(UUID.fromString(p.getSubjectRef()));
              }

              if (prospectRepository.existsById(p.getId())) {
                prospectRepository.deleteById(p.getId());
              }
            });
    return "OK";
  }
}
