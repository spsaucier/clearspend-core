package com.clearspend.capital.controller;

import com.clearspend.capital.controller.type.CurrentUser;
import com.clearspend.capital.controller.type.review.SoftFailRequiredDocumentsResponse;
import com.clearspend.capital.service.SoftFailService;
import com.clearspend.capital.service.SoftFailService.RequiredDocumentsForManualReview;
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
@RequestMapping("/manual-review")
@RequiredArgsConstructor
public class SoftFailController {

  private final SoftFailService softFailService;

  @GetMapping
  public SoftFailRequiredDocumentsResponse getRequiredDocumentsForManualReview() {
    RequiredDocumentsForManualReview requestedDocumentsByAlloy =
        softFailService.getDocumentsForManualReview(CurrentUser.get().businessId());
    return new SoftFailRequiredDocumentsResponse(requestedDocumentsByAlloy);
  }

  @PostMapping
  public String uploadManualReviewDocuments(
      @RequestParam(name = "documentList") List<MultipartFile> files) {

    softFailService.uploadManualReviewDocuments(files);

    return "Files successfully sent for review.";
  }
}
