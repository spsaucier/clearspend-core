package com.clearspend.capital.controller;

import com.clearspend.capital.common.typedid.data.BusinessId;
import com.clearspend.capital.common.typedid.data.CardId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.controller.type.CurrentUser;
import com.clearspend.capital.controller.type.PagedData;
import com.clearspend.capital.controller.type.card.CardDetailsResponse;
import com.clearspend.capital.controller.type.card.IssueCardRequest;
import com.clearspend.capital.controller.type.card.IssueCardResponse;
import com.clearspend.capital.controller.type.card.SearchCardData;
import com.clearspend.capital.controller.type.card.SearchCardRequest;
import com.clearspend.capital.controller.type.card.UpdateCardRequest;
import com.clearspend.capital.controller.type.card.limits.CurrencyLimit;
import com.clearspend.capital.service.BusinessService;
import com.clearspend.capital.service.CardFilterCriteria;
import com.clearspend.capital.service.CardService;
import com.clearspend.capital.service.ProgramService;
import io.swagger.v3.oas.annotations.Parameter;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
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
  private final ProgramService programService;
  private final BusinessService businessService;

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
                                programService.retrieveProgram(request.getProgramId()),
                                businessId,
                                request.getAllocationId(),
                                request.getUserId(),
                                request.getCurrency(),
                                request.getIsPersonal(),
                                businessLegalName,
                                CurrencyLimit.toMap(request.getLimits()),
                                request.getDisabledMccGroups(),
                                request.getDisabledTransactionChannels())
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
}