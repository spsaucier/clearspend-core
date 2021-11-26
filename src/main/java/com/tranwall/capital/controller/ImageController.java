package com.tranwall.capital.controller;

import com.tranwall.capital.common.typedid.data.ReceiptId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.controller.type.CurrentUser;
import com.tranwall.capital.controller.type.receipt.CreateReceiptRequest;
import com.tranwall.capital.controller.type.receipt.CreateReceiptResponse;
import com.tranwall.capital.service.ReceiptService;
import io.swagger.annotations.ApiParam;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

// NOTE: This controller is served from a different set of pods from our regular production traffic
// to account for the fact that images are quite large and would tend to mess up the heap. As such
// this controller only handles the store/create and get/fetch operations for binary content. Any
// operation should be served from regular controllers
@RestController
@RequestMapping("/images")
@RequiredArgsConstructor
public class ImageController {

  private final ReceiptService receiptService;

  @PostMapping("/receipts")
  private CreateReceiptResponse storeReceiptImage(
      @RequestParam("receipt") @ApiParam("receipt") MultipartFile receiptFile,
      @Validated @RequestBody CreateReceiptRequest request)
      throws IOException {
    CurrentUser currentUser = CurrentUser.get();
    return new CreateReceiptResponse(
        receiptService
            .storeReceiptImage(
                currentUser.businessId(),
                currentUser.userId(),
                request.getAmount().toAmount(),
                receiptFile.getBytes())
            .getId());
  }

  @GetMapping("/receipts/{receiptId}")
  private ResponseEntity<Resource> getReceiptImage(
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
    byte[] receiptImage =
        receiptService.getReceiptImage(currentUser.businessId(), currentUser.userId(), receiptId);
    return ResponseEntity.ok()
        .headers(headers)
        .contentLength(receiptImage.length)
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .body(new InputStreamResource(new ByteArrayInputStream(receiptImage)));
  }
}
