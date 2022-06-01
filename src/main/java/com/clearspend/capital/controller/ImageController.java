package com.clearspend.capital.controller;

import com.clearspend.capital.common.typedid.data.ReceiptId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.controller.type.receipt.CreateReceiptResponse;
import com.clearspend.capital.data.model.Receipt;
import com.clearspend.capital.service.ReceiptService;
import com.clearspend.capital.service.type.CurrentUser;
import io.swagger.annotations.ApiParam;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

// NOTE: This controller is served from a different set of pods from our regular production traffic
// to account for the fact that images are quite large and would tend to mess up the heap. As such
// this controller only handles the store/create and get/fetch operations for binary content. Any
// operation should be served from regular controllers
@RestController
@RequestMapping("/images")
@RequiredArgsConstructor
@Slf4j
public class ImageController {

  private final ReceiptService receiptService;

  @PostMapping(value = "/receipts", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  CreateReceiptResponse storeReceiptImage(@RequestPart("receipt") MultipartFile receiptFile)
      throws IOException {
    CurrentUser currentUser = CurrentUser.get();
    final Receipt receipt =
        receiptService.storeReceiptImage(
            CurrentUser.getActiveBusinessId(),
            currentUser.userId(),
            receiptFile.getBytes(),
            receiptFile.getContentType());
    log.info("Stored receipt: {}", receipt);
    return new CreateReceiptResponse(receipt.getId());
  }

  @GetMapping("/receipts/{receiptId}")
  ResponseEntity<Resource> getReceiptImage(
      @PathVariable(value = "receiptId")
          @ApiParam(
              required = true,
              name = "receiptId",
              value = "ID of the receipt record.",
              example = "48104ecb-1343-4cc1-b6f2-e6cc88e9a80f")
          TypedId<ReceiptId> receiptId) {

    HttpHeaders headers = new HttpHeaders();
    headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=receipt.jpg");
    headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
    headers.add("Pragma", "no-cache");
    headers.add("Expires", "0");

    CurrentUser currentUser = CurrentUser.get();
    Receipt receipt = receiptService.getReceipt(receiptId);
    if (StringUtils.isNotEmpty(receipt.getContentType())) {
      headers.add(
          HttpHeaders.CONTENT_TYPE,
          String.valueOf(MediaType.parseMediaType(receipt.getContentType())));
      log.info("headers: {}", headers);
    }

    byte[] receiptImage = receiptService.getReceiptImage(receipt);
    log.info(
        "returning image: businessIid {}, userId {}, receiptId {} ({} bytes)",
        CurrentUser.getActiveBusinessId(),
        currentUser.userId(),
        receiptId,
        receiptImage.length);

    return ResponseEntity.ok()
        .headers(headers)
        .contentLength(receiptImage.length)
        .contentType(
            StringUtils.isNotEmpty(receipt.getContentType())
                ? MediaType.parseMediaType(receipt.getContentType())
                : MediaType.APPLICATION_OCTET_STREAM)
        .body(new ByteArrayResource(receiptImage));
  }
}
