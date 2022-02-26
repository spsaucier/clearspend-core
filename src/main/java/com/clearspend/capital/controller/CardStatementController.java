package com.clearspend.capital.controller;

import com.clearspend.capital.controller.type.activity.CardStatementRequest;
import com.clearspend.capital.service.CardStatementService;
import com.clearspend.capital.service.CardStatementService.CardStatementRecord;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/card-statement")
@RequiredArgsConstructor
public class CardStatementController {

  private final CardStatementService cardStatementService;

  @PostMapping("")
  ResponseEntity<byte[]> cardStatement(@Validated @RequestBody CardStatementRequest request)
      throws IOException {

    CardStatementRecord result = cardStatementService.generatePdf(request);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_PDF);

    headers.setContentDisposition(
        ContentDisposition.builder("attachment").filename(result.fileName()).build());

    return new ResponseEntity<>(result.pdf(), headers, HttpStatus.OK);
  }
}
