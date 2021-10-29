package com.tranwall.capital.controller;

import com.tranwall.capital.common.typedid.data.BusinessId;
import com.tranwall.capital.common.typedid.data.CardId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.controller.type.card.Card;
import com.tranwall.capital.controller.type.card.IssueCardRequest;
import com.tranwall.capital.controller.type.card.IssueCardResponse;
import com.tranwall.capital.service.BusinessService;
import com.tranwall.capital.service.CardService;
import com.tranwall.capital.service.ProgramService;
import io.swagger.v3.oas.annotations.Parameter;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/cards")
@RequiredArgsConstructor
public class CardController {

  private final CardService cardService;
  private final ProgramService programService;
  private final BusinessService businessService;

  @PostMapping("")
  private List<IssueCardResponse> issueCard(
      @RequestHeader(name = "businessId") TypedId<BusinessId> businessId,
      @RequestBody @Validated IssueCardRequest request) {
    List<IssueCardResponse> issueCardResponseList = new ArrayList<>();
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
                                cardType,
                                request.getIsPersonal(),
                                businessService.getBusiness(businessId).business().getLegalName())
                            .getId(),
                        null)));

    return issueCardResponseList;
  }

  @GetMapping("/{cardId}")
  private Card getCard(
      @RequestHeader(name = "businessId") TypedId<BusinessId> businessId,
      @PathVariable(value = "cardId")
          @Parameter(
              required = true,
              name = "cardId",
              description = "ID of the allocation record.",
              example = "48104ecb-1343-4cc1-b6f2-e6cc88e9a80f")
          TypedId<CardId> cardId) {
    return new Card(cardService.getCard(businessId, cardId).card());
  }
}
