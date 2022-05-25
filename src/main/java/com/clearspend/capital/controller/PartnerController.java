package com.clearspend.capital.controller;

import com.clearspend.capital.controller.type.partner.PartnerBusiness;
import com.clearspend.capital.service.PartnerService;
import com.clearspend.capital.service.UserService;
import com.clearspend.capital.service.type.CurrentUser;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/partner")
@RequiredArgsConstructor
public class PartnerController {

  private final PartnerService partnerService;
  private final UserService userService;

  @GetMapping("/businesses")
  public List<PartnerBusiness> getAllPartnerBusinesses() {
    return partnerService.getAllPartneredBusinessesForUser(
        userService.retrieveUser(CurrentUser.getUserId()));
  }

  @GetMapping("/pins")
  public List<PartnerBusiness> getPinnedBusinesses() {
    return partnerService.getAllPinnedBusinessesForUser(
        userService.retrieveUser(CurrentUser.getUserId()));
  }
}
