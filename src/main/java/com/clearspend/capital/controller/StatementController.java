package com.clearspend.capital.controller;

import com.clearspend.capital.controller.type.activity.BusinessStatementRequest;
import com.clearspend.capital.controller.type.activity.CardStatementRequest;
import com.clearspend.capital.data.repository.CardRepositoryCustom;
import com.clearspend.capital.service.CardService;
import com.clearspend.capital.service.StatementService;
import com.clearspend.capital.service.StatementService.StatementRecord;
import com.clearspend.capital.service.type.CurrentUser;
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
@RequestMapping("/statements")
@RequiredArgsConstructor
public class StatementController {

  private final StatementService statementService;
  private final CardService cardService;

  @PostMapping("card")
  ResponseEntity<byte[]> cardStatement(@Validated @RequestBody CardStatementRequest request) {

    final CardRepositoryCustom.CardDetailsRecord card =
        cardService.getCard(CurrentUser.getActiveBusinessId(), request.getCardId());
    StatementRecord result = statementService.generateCardStatementPdf(request, card);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_PDF);

    headers.setContentDisposition(
        ContentDisposition.builder("attachment").filename(result.fileName()).build());

    return new ResponseEntity<>(result.pdf(), headers, HttpStatus.OK);
  }

  @PostMapping("business")
  ResponseEntity<byte[]> businessStatement(
      @Validated @RequestBody BusinessStatementRequest request) {

    StatementRecord result =
        statementService.generateBusinessStatementPdf(
            CurrentUser.getActiveBusinessId(), request.getStartDate(), request.getEndDate());

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_PDF);

    headers.setContentDisposition(
        ContentDisposition.builder("attachment").filename(result.fileName()).build());

    return new ResponseEntity<>(result.pdf(), headers, HttpStatus.OK);
  }
}
