package com.clearspend.capital.controller;

import com.clearspend.capital.client.stripe.StripeClient;
import com.clearspend.capital.common.error.InvalidRequestException;
import com.clearspend.capital.common.typedid.data.CardId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.controller.type.PagedData;
import com.clearspend.capital.controller.type.allocation.AllocationFundsManagerResponse;
import com.clearspend.capital.controller.type.card.Card;
import com.clearspend.capital.controller.type.card.CardAllocationDetails;
import com.clearspend.capital.controller.type.card.CardAllocationSpendControls;
import com.clearspend.capital.controller.type.card.CardDetailsResponse;
import com.clearspend.capital.controller.type.card.EphemeralKeyRequest;
import com.clearspend.capital.controller.type.card.IssueCardRequest;
import com.clearspend.capital.controller.type.card.IssueCardResponse;
import com.clearspend.capital.controller.type.card.RevealCardRequest;
import com.clearspend.capital.controller.type.card.RevealCardResponse;
import com.clearspend.capital.controller.type.card.SearchCardData;
import com.clearspend.capital.controller.type.card.SearchCardRequest;
import com.clearspend.capital.controller.type.card.UpdateCardSpendControlsRequest;
import com.clearspend.capital.controller.type.common.PageRequest;
import com.clearspend.capital.controller.type.user.UserData;
import com.clearspend.capital.data.model.enums.FundingType;
import com.clearspend.capital.data.repository.CardRepositoryCustom.CardDetailsRecord;
import com.clearspend.capital.service.BusinessService;
import com.clearspend.capital.service.CardFilterCriteria;
import com.clearspend.capital.service.CardService;
import com.clearspend.capital.service.SecuredRolesAndPermissionsService;
import com.clearspend.capital.service.UserService;
import com.clearspend.capital.service.type.CurrentUser;
import com.stripe.model.EphemeralKey;
import io.swagger.v3.oas.annotations.Parameter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/cards")
@RequiredArgsConstructor
public class CardController {

  private final CardService cardService;
  private final BusinessService businessService;
  private final UserService userService;
  private final StripeClient stripeClient;

  private final SecuredRolesAndPermissionsService securedRolesAndPermissionsService;

  @GetMapping("/{cardId}")
  CardDetailsResponse getCard(
      @PathVariable(value = "cardId")
          @Parameter(
              required = true,
              name = "cardId",
              description = "ID of the card record.",
              example = "48104ecb-1343-4cc1-b6f2-e6cc88e9a80f")
          TypedId<CardId> cardId) {

    return CardDetailsResponse.of(cardService.getCard(CurrentUser.getActiveBusinessId(), cardId));
  }

  @PatchMapping("/{cardId}/reassign/{userId}")
  Card reassignCardOwner(
      @PathVariable("cardId")
          @Parameter(
              required = true,
              name = "cardId",
              description = "ID of the card to reassign",
              example = "48104ecb-1343-4cc1-b6f2-e6cc88e9a80f")
          final TypedId<CardId> cardId,
      @PathVariable("userId")
          @Parameter(
              required = true,
              name = "cardId",
              description = "ID of the user to assign to the card",
              example = "48104ecb-1343-4cc1-b6f2-e6cc88e9a80f")
          final TypedId<UserId> userId) {
    return new Card(
        cardService.reassignCard(
            cardService.retrieveCard(CurrentUser.getActiveBusinessId(), cardId), userId));
  }

  @PostMapping("")
  List<IssueCardResponse> issueCard(@RequestBody @Validated final IssueCardRequest request) {
    final TypedId<BusinessId> businessId = CurrentUser.getActiveBusinessId();
    final String businessLegalName = businessService.getBusiness(businessId, true).getLegalName();

    // Keeping this at the controller level so as not to break some great test logic we have in
    // place for individual cards
    if (request.getFundingType() == FundingType.INDIVIDUAL) {
      throw new InvalidRequestException("Individual card issuing is not currently supported");
    }

    if (userService.retrieveUser(request.getUserId()).isArchived()) {
      throw new InvalidRequestException("User has been archived");
    }

    return request.getCardType().stream()
        .map(
            cardType ->
                new IssueCardResponse(
                    cardService.issueCard(businessId, cardType, request).card().getId(), null))
        .toList();
  }

  @PostMapping("/{cardId}/allocations")
  CardDetailsResponse addAllocationsToCard(
      @PathVariable
          @Parameter(
              required = true,
              name = "cardId",
              description = "ID of the card record.",
              example = "48104ecb-1343-4cc1-b6f2-e6cc88e9a80f")
          final TypedId<CardId> cardId,
      @RequestBody @Validated final List<CardAllocationSpendControls> allocationsAndSpendControls) {
    final TypedId<BusinessId> businessId = CurrentUser.getActiveBusinessId();
    cardService.addAllocationsToCard(
        cardService.retrieveCard(businessId, cardId), allocationsAndSpendControls);

    return CardDetailsResponse.of(cardService.getCard(businessId, cardId));
  }

  @DeleteMapping("/{cardId}/allocations")
  CardDetailsResponse removeAllocationsFromCard(
      @PathVariable
          @Parameter(
              required = true,
              name = "cardId",
              description = "ID of the card record.",
              example = "48104ecb-1343-4cc1-b6f2-e6cc88e9a80f")
          final TypedId<CardId> cardId,
      @RequestBody @Validated List<CardAllocationDetails> allocationsToRemove) {
    final TypedId<BusinessId> businessId = CurrentUser.getActiveBusinessId();
    cardService.removeAllocationsFromCard(
        cardService.retrieveCard(businessId, cardId), allocationsToRemove);

    return CardDetailsResponse.of(cardService.getCard(businessId, cardId));
  }

  @PatchMapping("/{cardId}/controls")
  CardDetailsResponse updateCardSpendControls(
      @PathVariable(value = "cardId")
          @Parameter(
              required = true,
              name = "cardId",
              description = "ID of the card record.",
              example = "48104ecb-1343-4cc1-b6f2-e6cc88e9a80f")
          final TypedId<CardId> cardId,
      @RequestBody @Validated final UpdateCardSpendControlsRequest request) {

    final TypedId<BusinessId> businessId = CurrentUser.getActiveBusinessId();
    cardService.updateCardSpendControls(cardService.retrieveCard(businessId, cardId), request);

    return CardDetailsResponse.of(cardService.getCard(businessId, cardId));
  }

  @PostMapping("/search")
  PagedData<SearchCardData> search(@Validated @RequestBody SearchCardRequest request) {
    return PagedData.of(cardService.filterCards(CardFilterCriteria.fromSearchRequest(request)));
  }

  @PostMapping("/reveal")
  RevealCardResponse reveal(@RequestBody @Validated RevealCardRequest request) {
    CardDetailsRecord cardDetailsRecord =
        cardService.getCard(CurrentUser.getActiveBusinessId(), request.getCardId());
    String ephemeralKey =
        stripeClient.getEphemeralKey(cardDetailsRecord.card().getExternalRef(), request.getNonce());
    return new RevealCardResponse(cardDetailsRecord.card().getExternalRef(), ephemeralKey);
  }

  @PostMapping("/export-csv")
  ResponseEntity<byte[]> exportCsv(@Validated @RequestBody SearchCardRequest request) {

    // export must return all records, regardless if pagination is set in "view records" mode
    request.setPageRequest(new PageRequest(0, Integer.MAX_VALUE));

    byte[] csvFile = cardService.createCSVFile(CardFilterCriteria.fromSearchRequest(request));
    HttpHeaders headers = new HttpHeaders();
    headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=cards.csv");
    headers.set(HttpHeaders.CONTENT_TYPE, "text/csv");
    headers.set(HttpHeaders.CONTENT_LENGTH, String.valueOf(csvFile.length));
    return new ResponseEntity<>(csvFile, headers, HttpStatus.OK);
  }

  @RequestMapping(
      value = "/ephemeral-key",
      method = RequestMethod.POST,
      produces = "application/json")
  String ephemeralKey(@RequestBody @Validated EphemeralKeyRequest request) {
    CardDetailsRecord cardDetailsRecord =
        cardService.getCard(CurrentUser.getActiveBusinessId(), request.getCardId());
    EphemeralKey stripeEphemeralKey =
        stripeClient.getEphemeralKeyObjectForCard(
            cardDetailsRecord.card().getExternalRef(), request.getApiVersion());
    return stripeEphemeralKey.getRawJson();
  }

  @GetMapping("/{cardId}/funds-managers")
  AllocationFundsManagerResponse getAllocationManagersForAllocation(
      @PathVariable(value = "cardId")
          @Parameter(
              required = true,
              name = "cardId",
              description = "ID of the card record.",
              example = "48104ecb-1343-4cc1-b6f2-e6cc88e9a80f")
          TypedId<CardId> cardId) {
    return new AllocationFundsManagerResponse(
        securedRolesAndPermissionsService
            .getManagerRolesAndPermissionsForAllocation(
                cardService.retrieveCard(CurrentUser.getActiveBusinessId(), cardId))
            .values()
            .stream()
            .map(
                userRolesAndPermissions ->
                    new UserData(
                        userRolesAndPermissions.userId(),
                        userRolesAndPermissions.userType(),
                        userRolesAndPermissions.firstName(),
                        userRolesAndPermissions.lastName(),
                        false))
            .toList());
  }
}
