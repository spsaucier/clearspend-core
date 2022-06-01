package com.clearspend.capital.controller;

import com.clearspend.capital.controller.type.review.ApplicationReviewRequirements;
import com.clearspend.capital.service.ApplicationReviewService;
import com.clearspend.capital.service.type.CurrentUser;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/application-review")
@RequiredArgsConstructor
public class ApplicationReviewController {

  private final ApplicationReviewService applicationReviewService;

  @GetMapping("/requirement")
  ApplicationReviewRequirements getApplicationReviewRequirements() {
    return applicationReviewService.getStripeApplicationRequirements(
        CurrentUser.getActiveBusinessId());
  }

  @PostMapping("/document")
  String uploadDocuments(@RequestParam(name = "documentList") List<MultipartFile> files) {

    applicationReviewService.uploadStripeRequiredDocuments(
        CurrentUser.getActiveBusinessId(), files);

    return "Files successfully sent for review.";
  }
}
