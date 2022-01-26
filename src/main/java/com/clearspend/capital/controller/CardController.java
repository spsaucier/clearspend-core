package com.clearspend.capital.controller;

import com.clearspend.capital.client.stripe.StripeClient;
import com.clearspend.capital.common.typedid.data.CardId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.controller.type.PagedData;
import com.clearspend.capital.controller.type.card.CardDetailsResponse;
import com.clearspend.capital.controller.type.card.IssueCardRequest;
import com.clearspend.capital.controller.type.card.IssueCardResponse;
import com.clearspend.capital.controller.type.card.RevealCardRequest;
import com.clearspend.capital.controller.type.card.RevealCardResponse;
import com.clearspend.capital.controller.type.card.SearchCardData;
import com.clearspend.capital.controller.type.card.SearchCardRequest;
import com.clearspend.capital.controller.type.card.UpdateCardRequest;
import com.clearspend.capital.controller.type.card.limits.CurrencyLimit;
import com.clearspend.capital.data.model.enums.FundingType;
import com.clearspend.capital.data.model.enums.card.BinType;
import com.clearspend.capital.data.repository.CardRepositoryCustom.CardDetailsRecord;
import com.clearspend.capital.service.BusinessService;
import com.clearspend.capital.service.CardFilterCriteria;
import com.clearspend.capital.service.CardService;
import com.clearspend.capital.service.type.CurrentUser;
import io.swagger.v3.oas.annotations.Parameter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/cards")
@RequiredArgsConstructor
public class CardController {

  private final CardService cardService;
  private final BusinessService businessService;
  private final StripeClient stripeClient;

  @GetMapping("/{cardId}")
  private CardDetailsResponse getCard(
      @PathVariable(value = "cardId")
          @Parameter(
              required = true,
              name = "cardId",
              description = "ID of the allocation record.",
              example = "48104ecb-1343-4cc1-b6f2-e6cc88e9a80f")
          TypedId<CardId> cardId) {

    return CardDetailsResponse.of(cardService.getCard(CurrentUser.get().businessId(), cardId));
  }

  @PostMapping("")
  private List<IssueCardResponse> issueCard(@RequestBody @Validated IssueCardRequest request) {
    List<IssueCardResponse> issueCardResponseList = new ArrayList<>();
    TypedId<BusinessId> businessId = CurrentUser.get().businessId();
    String businessLegalName = businessService.retrieveBusiness(businessId).getLegalName();
    request
        .getCardType()
        .forEach(
            cardType ->
                issueCardResponseList.add(
                    new IssueCardResponse(
                        cardService
                            .issueCard(
                                request.getBinType() != null ? request.getBinType() : BinType.DEBIT,
                                request.getFundingType() != null
                                    ? request.getFundingType()
                                    : FundingType.POOLED,
                                cardType,
                                businessId,
                                request.getAllocationId(),
                                request.getUserId(),
                                request.getCurrency(),
                                request.getIsPersonal(),
                                businessLegalName,
                                CurrencyLimit.toMap(request.getLimits()),
                                request.getDisabledMccGroups(),
                                request.getDisabledTransactionChannels(),
                                request.getShippingAddress() != null
                                    ? request.getShippingAddress().toAddress()
                                    : null)
                            .card()
                            .getId(),
                        null)));

    return issueCardResponseList;
  }

  @PatchMapping("/{cardId}")
  private CardDetailsResponse updateCard(
      @PathVariable(value = "cardId")
          @Parameter(
              required = true,
              name = "cardId",
              description = "ID of the card record.",
              example = "48104ecb-1343-4cc1-b6f2-e6cc88e9a80f")
          TypedId<CardId> cardId,
      @RequestBody @Validated UpdateCardRequest request) {

    TypedId<BusinessId> businessId = CurrentUser.get().businessId();
    cardService.updateCard(
        businessId,
        cardId,
        CurrencyLimit.toMap(request.getLimits()),
        request.getDisabledMccGroups(),
        request.getDisabledTransactionChannels());

    return CardDetailsResponse.of(cardService.getCard(businessId, cardId));
  }

  @PostMapping("/search")
  private PagedData<SearchCardData> search(@Validated @RequestBody SearchCardRequest request) {
    // TODO: Implement proper security restrictions based on the outcome of CAP-202
    return PagedData.of(
        cardService.filterCards(new CardFilterCriteria(CurrentUser.get().businessId(), request)),
        SearchCardData::of);
  }

  @PostMapping("/reveal")
  private RevealCardResponse reveal(@RequestBody @Validated RevealCardRequest request) {
    CardDetailsRecord cardDetailsRecord =
        cardService.getCard(CurrentUser.get().businessId(), request.getCardId());
    String ephemeralKey =
        stripeClient.getEphemeralKey(cardDetailsRecord.card().getExternalRef(), request.getNonce());
    return new RevealCardResponse(cardDetailsRecord.card().getExternalRef(), ephemeralKey);
  }

  @PostMapping("/export-csv")
  private ResponseEntity<byte[]> exportCsv(@Validated @RequestBody SearchCardRequest request)
      throws IOException {
    byte[] csvFile =
        cardService.createCSVFile(new CardFilterCriteria(CurrentUser.get().businessId(), request));
    HttpHeaders headers = new HttpHeaders();
    headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=cards.csv");
    headers.set(HttpHeaders.CONTENT_TYPE, "text/csv");
    headers.set(HttpHeaders.CONTENT_LENGTH, String.valueOf(csvFile.length));
    return new ResponseEntity<>(csvFile, headers, HttpStatus.OK);
  }
}
