package com.tranwall.capital.controller;

import com.tranwall.capital.common.typedid.data.BusinessId;
import com.tranwall.capital.common.typedid.data.CardId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.controller.type.card.Card;
import com.tranwall.capital.controller.type.card.IssueCardRequest;
import com.tranwall.capital.controller.type.card.IssueCardResponse;
import com.tranwall.capital.service.CardService;
import com.tranwall.capital.service.ProgramService;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
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

  @PostMapping("")
  private IssueCardResponse issueCard(
      @RequestHeader(name = "businessId") TypedId<BusinessId> businessId,
      @RequestBody IssueCardRequest request) {

    com.tranwall.capital.data.model.Card card =
        cardService.issueCard(
            request.getBin(),
            programService.retrieveProgram(request.getProgramId()),
            businessId,
            request.getAllocationId(),
            request.getUserId(),
            request.getCurrency(),
            request.getCardType());

    return new IssueCardResponse(card.getId(), null);
  }

  @GetMapping("/{cardId}")
  private Card getCard(
      @RequestHeader(name = "businessId") TypedId<BusinessId> businessId,
      @PathVariable(value = "cardId")
          @ApiParam(
              required = true,
              name = "cardId",
              value = "ID of the allocation record.",
              example = "48104ecb-1343-4cc1-b6f2-e6cc88e9a80f")
          TypedId<CardId> cardId) {
    return new Card(cardService.getCard(businessId, cardId).card());
  }
}
