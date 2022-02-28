package com.clearspend.capital.controller;

import com.clearspend.capital.common.typedid.data.AccountActivityId;
import com.clearspend.capital.common.typedid.data.CardId;
import com.clearspend.capital.common.typedid.data.ReceiptId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.controller.type.Amount;
import com.clearspend.capital.controller.type.PagedData;
import com.clearspend.capital.controller.type.activity.AccountActivityResponse;
import com.clearspend.capital.controller.type.activity.UpdateAccountActivityRequest;
import com.clearspend.capital.controller.type.card.ActivateCardRequest;
import com.clearspend.capital.controller.type.card.Card;
import com.clearspend.capital.controller.type.card.CardAccount;
import com.clearspend.capital.controller.type.card.CardDetailsResponse;
import com.clearspend.capital.controller.type.card.UpdateCardAccountRequest;
import com.clearspend.capital.controller.type.card.UpdateCardStatusRequest;
import com.clearspend.capital.controller.type.common.PageRequest;
import com.clearspend.capital.controller.type.receipt.Receipt;
import com.clearspend.capital.controller.type.user.CreateUserRequest;
import com.clearspend.capital.controller.type.user.CreateUserResponse;
import com.clearspend.capital.controller.type.user.SearchUserRequest;
import com.clearspend.capital.controller.type.user.UpdateUserRequest;
import com.clearspend.capital.controller.type.user.UpdateUserResponse;
import com.clearspend.capital.controller.type.user.User;
import com.clearspend.capital.controller.type.user.UserPageData;
import com.clearspend.capital.data.model.AccountActivity;
import com.clearspend.capital.data.model.business.BusinessOwner;
import com.clearspend.capital.data.model.enums.AccountActivityType;
import com.clearspend.capital.data.model.enums.AccountType;
import com.clearspend.capital.data.model.enums.UserType;
import com.clearspend.capital.data.model.enums.card.CardStatus;
import com.clearspend.capital.data.repository.CardRepositoryCustom.CardDetailsRecord;
import com.clearspend.capital.service.AccountActivityFilterCriteria;
import com.clearspend.capital.service.AccountActivityService;
import com.clearspend.capital.service.BusinessOwnerService;
import com.clearspend.capital.service.BusinessProspectService;
import com.clearspend.capital.service.CardService;
import com.clearspend.capital.service.ReceiptService;
import com.clearspend.capital.service.UserFilterCriteria;
import com.clearspend.capital.service.UserService;
import com.clearspend.capital.service.UserService.CreateUpdateUserRecord;
import com.clearspend.capital.service.type.CurrentUser;
import io.swagger.v3.oas.annotations.Parameter;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

  private final AccountActivityService accountActivityService;
  private final CardService cardService;
  private final ReceiptService receiptService;
  private final UserService userService;
  private final BusinessProspectService businessProspectService;
  private final BusinessOwnerService businessOwnerService;

  @PostMapping("")
  CreateUserResponse createUser(@RequestBody CreateUserRequest request) {

    CreateUpdateUserRecord userServiceUser =
        userService.createUser(
            CurrentUser.get().businessId(),
            UserType.EMPLOYEE,
            request.getFirstName(),
            request.getLastName(),
            request.getAddress() != null ? request.getAddress().toAddress() : null,
            request.getEmail(),
            request.getPhone());

    return new CreateUserResponse(userServiceUser.user().getId(), userServiceUser.password(), null);
  }

  @PatchMapping("/{userId}")
  UpdateUserResponse updateUser(
      @PathVariable(value = Common.USER_ID)
          @Parameter(
              required = true,
              name = Common.USER_ID,
              description = "ID of the user record.",
              example = "48104ecb-1343-4cc1-b6f2-e6cc88e9a80f")
          TypedId<UserId> userId,
      @RequestBody UpdateUserRequest request) {

    final CreateUpdateUserRecord updateUserRecord =
        userService.updateUser(
            CurrentUser.get().businessId(),
            userId,
            request.getFirstName(),
            request.getLastName(),
            request.getAddress() != null ? request.getAddress().toAddress() : null,
            request.getEmail(),
            request.getPhone(),
            request.isGeneratePassword());

    return new UpdateUserResponse(updateUserRecord.user().getId(), null);
  }

  @PostMapping("/bulk")
  List<CreateUserResponse> bulkCreateUser(@RequestBody List<CreateUserRequest> request) {

    List<CreateUserResponse> response = new ArrayList<>(request.size());
    for (CreateUserRequest createUserRequest : request) {
      CreateUpdateUserRecord userServiceUser =
          userService.createUser(
              CurrentUser.get().businessId(),
              UserType.EMPLOYEE,
              createUserRequest.getFirstName(),
              createUserRequest.getLastName(),
              createUserRequest.getAddress() != null
                  ? createUserRequest.getAddress().toAddress()
                  : null,
              createUserRequest.getEmail(),
              createUserRequest.getPhone());
      response.add(
          new CreateUserResponse(userServiceUser.user().getId(), userServiceUser.password(), null));
    }

    return response;
  }

  @GetMapping("/{userId}")
  User getUser(
      @PathVariable(value = Common.USER_ID)
          @Parameter(
              required = true,
              name = Common.USER_ID,
              description = "ID of the user record.",
              example = "48104ecb-1343-4cc1-b6f2-e6cc88e9a80f")
          TypedId<UserId> userId) {
    return new User(userService.retrieveUser(userId));
  }

  @GetMapping
  User currentUser() {
    CurrentUser currentUser = CurrentUser.get();
    return switch (currentUser.userType()) {
      case EMPLOYEE -> new User(userService.retrieveUser(currentUser.userId()));
      case BUSINESS_OWNER -> {
        Optional<BusinessOwner> businessOwner =
            businessOwnerService.retrieveBusinessOwnerNotThrowingException(
                new TypedId<>(currentUser.userId().toUuid()));
        if (businessOwner.isPresent()) {
          yield new User(businessOwner.get());
        }
        yield new User(
            businessProspectService.retrieveBusinessProspect(
                new TypedId<>(currentUser.userId().toUuid())));
      }
    };
  }

  @GetMapping(value = "/list")
  List<User> getUsersByUserName() {
    TypedId<BusinessId> businessId = CurrentUser.get().businessId();
    return userService.retrieveUsersForBusiness(businessId).stream().map(User::new).toList();
  }

  @PostMapping(value = "/search")
  PagedData<UserPageData> retrieveUsersPageData(@Validated @RequestBody SearchUserRequest request) {
    TypedId<BusinessId> businessId = CurrentUser.get().businessId();
    var userPage = userService.retrieveUserPage(businessId, new UserFilterCriteria(request));

    return PagedData.of(userPage, user -> new UserPageData(user.user(), user.card()));
  }

  @GetMapping("/cards")
  List<CardDetailsResponse> getUserCards() {
    CurrentUser currentUser = CurrentUser.get();

    return cardService.getUserCards(currentUser.businessId(), currentUser.userId()).stream()
        .map(CardDetailsResponse::of)
        .toList();
  }

  @GetMapping("/cards/{cardId}")
  CardDetailsResponse getUserCard(
      @PathVariable(value = "cardId")
          @Parameter(
              required = true,
              name = "cardId",
              description = "ID of the card record.",
              example = "48104ecb-1343-4cc1-b6f2-e6cc88e9a80f")
          TypedId<CardId> cardId) {
    CurrentUser currentUser = CurrentUser.get();
    CardDetailsRecord userCardRecord =
        cardService.getUserCard(currentUser.businessId(), currentUser.userId(), cardId);

    return CardDetailsResponse.of(userCardRecord);
  }

  @PatchMapping("/cards/{cardId}/block")
  Card blockCard(
      @PathVariable(value = "cardId")
          @Parameter(
              required = true,
              name = "cardId",
              description = "ID of the card record.",
              example = "48104ecb-1343-4cc1-b6f2-e6cc88e9a80f")
          TypedId<CardId> cardId,
      @Validated @RequestBody UpdateCardStatusRequest request) {
    CurrentUser currentUser = CurrentUser.get();

    return new Card(
        cardService.updateCardStatus(
            currentUser.businessId(),
            currentUser.userId(),
            currentUser.userType(),
            cardId,
            CardStatus.INACTIVE,
            request.getStatusReason(),
            false));
  }

  @PatchMapping("/cards/{cardId}/activate")
  Card activateCard(
      @PathVariable(value = "cardId")
          @Parameter(
              required = true,
              name = "cardId",
              description = "ID of the card record.",
              example = "48104ecb-1343-4cc1-b6f2-e6cc88e9a80f")
          TypedId<CardId> cardId,
      @Validated @RequestBody ActivateCardRequest request) {
    CurrentUser currentUser = CurrentUser.get();

    return new Card(
        cardService.activateCard(
            currentUser.businessId(),
            currentUser.userId(),
            currentUser.userType(),
            cardId,
            request.getLastFour(),
            request.getStatusReason()));
  }

  @PatchMapping("/cards/activate")
  Card activateCards(@Validated @RequestBody ActivateCardRequest request) {
    CurrentUser currentUser = CurrentUser.get();

    return new Card(
        cardService.activateCards(
            currentUser.businessId(),
            currentUser.userId(),
            currentUser.userType(),
            request.getLastFour(),
            request.getStatusReason()));
  }

  @PatchMapping("/cards/{cardId}/unblock")
  Card unblockCard(
      @PathVariable(value = "cardId")
          @Parameter(
              required = true,
              name = "cardId",
              description = "ID of the card record.",
              example = "48104ecb-1343-4cc1-b6f2-e6cc88e9a80f")
          TypedId<CardId> cardId,
      @Validated @RequestBody UpdateCardStatusRequest request) {
    CurrentUser currentUser = CurrentUser.get();

    return new Card(
        cardService.updateCardStatus(
            currentUser.businessId(),
            currentUser.userId(),
            currentUser.userType(),
            cardId,
            CardStatus.ACTIVE,
            request.getStatusReason(),
            false));
    // update card currently cannot distinguish between activation which happened
    // due to unfreeze event vs initial physical card activation
    // last variable is needed to pick the right notification template

  }

  @PatchMapping("/cards/{cardId}/retire")
  Card retireCard(
      @PathVariable(value = "cardId")
          @Parameter(
              required = true,
              name = "cardId",
              description = "ID of the card record.",
              example = "48104ecb-1343-4cc1-b6f2-e6cc88e9a80f")
          TypedId<CardId> cardId,
      @Validated @RequestBody UpdateCardStatusRequest request) {
    CurrentUser currentUser = CurrentUser.get();

    return new Card(
        cardService.updateCardStatus(
            currentUser.businessId(),
            currentUser.userId(),
            currentUser.userType(),
            cardId,
            CardStatus.CANCELLED,
            request.getStatusReason(),
            false));
  }

  @GetMapping("/cards/{cardId}/accounts")
  List<CardAccount> getCardAccounts(
      @PathVariable(value = "cardId")
          @Parameter(
              required = true,
              name = "cardId",
              description = "ID of the card record.",
              example = "48104ecb-1343-4cc1-b6f2-e6cc88e9a80f")
          TypedId<CardId> cardId,
      @RequestParam(required = false) AccountType type) {
    CurrentUser currentUser = CurrentUser.get();

    return cardService
        .getCardAccounts(currentUser.businessId(), currentUser.userId(), cardId, type)
        .stream()
        .map(
            e ->
                new CardAccount(
                    e.getAllocationId(), e.getId(), e.getType(), Amount.of(e.getLedgerBalance())))
        .toList();
  }

  @PatchMapping("/cards/{cardId}/account")
  Card updateCardAccount(
      @PathVariable(value = "cardId")
          @Parameter(
              required = true,
              name = "cardId",
              description = "ID of the card record.",
              example = "48104ecb-1343-4cc1-b6f2-e6cc88e9a80f")
          TypedId<CardId> cardId,
      @Validated @RequestBody UpdateCardAccountRequest request) {
    CurrentUser currentUser = CurrentUser.get();

    return new Card(
        cardService.updateCardAccount(
            currentUser.businessId(),
            currentUser.userId(),
            cardId,
            request.getAllocationId(),
            request.getAccountId()));
  }

  @GetMapping("/cards/{cardId}/account-activity")
  PagedData<AccountActivityResponse> getCardAccountActivity(
      @PathVariable(value = "cardId")
          @Parameter(
              required = true,
              name = "cardId",
              description = "ID of the card record.",
              example = "48104ecb-1343-4cc1-b6f2-e6cc88e9a80f")
          TypedId<CardId> cardId,
      @RequestParam(required = false) AccountActivityType type,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          OffsetDateTime dateFrom,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          OffsetDateTime dateTo,
      PageRequest pageRequest) {
    CurrentUser currentUser = CurrentUser.get();

    Page<AccountActivity> accountActivity =
        accountActivityService.getCardAccountActivity(
            currentUser.businessId(),
            currentUser.userId(),
            cardId,
            new AccountActivityFilterCriteria(
                cardId, type != null ? List.of(type) : List.of(), dateFrom, dateTo, pageRequest));

    return PagedData.of(accountActivity, AccountActivityResponse::new);
  }

  @GetMapping("/account-activity/{accountActivityId}")
  AccountActivityResponse getAccountActivity(
      @PathVariable(value = "accountActivityId")
          @Parameter(
              required = true,
              name = "accountActivityId",
              description = "ID of the account activity record.",
              example = "48104ecb-1343-4cc1-b6f2-e6cc88e9a80f")
          TypedId<AccountActivityId> accountActivityId) {
    CurrentUser currentUser = CurrentUser.get();

    return new AccountActivityResponse(
        accountActivityService.getUserAccountActivity(
            currentUser.businessId(), currentUser.userId(), accountActivityId));
  }

  @PatchMapping("/account-activity/{accountActivityId}")
  AccountActivityResponse updateAccountActivity(
      @PathVariable(value = "accountActivityId")
          @Parameter(
              required = true,
              name = "accountActivityId",
              description = "ID of the account activity record.",
              example = "48104ecb-1343-4cc1-b6f2-e6cc88e9a80f")
          TypedId<AccountActivityId> accountActivityId,
      @Validated @RequestBody UpdateAccountActivityRequest request) {
    CurrentUser currentUser = CurrentUser.get();

    return new AccountActivityResponse(
        accountActivityService.updateAccountActivity(
            currentUser.businessId(),
            currentUser.userId(),
            accountActivityId,
            request.getNotes(),
            request.getIconRef()));
  }

  @PostMapping("/account-activity/{accountActivityId}/receipts/{receiptId}/link")
  void linkReceipt(
      @PathVariable(value = "accountActivityId")
          @Parameter(
              required = true,
              name = "accountActivityId",
              description = "ID of the account activity record.",
              example = "48104ecb-1343-4cc1-b6f2-e6cc88e9a80f")
          TypedId<AccountActivityId> accountActivityId,
      @PathVariable(value = "receiptId")
          @Parameter(
              required = true,
              name = "receiptId",
              description = "ID of the receipt record.",
              example = "48104ecb-1343-4cc1-b6f2-e6cc88e9a80f")
          TypedId<ReceiptId> receiptId) {
    CurrentUser currentUser = CurrentUser.get();

    receiptService.linkReceipt(
        currentUser.businessId(), currentUser.userId(), receiptId, accountActivityId);
  }

  @PostMapping("/account-activity/{accountActivityId}/receipts/{receiptId}/unlink")
  void unlinkReceipt(
      @PathVariable(value = "accountActivityId")
          @Parameter(
              required = true,
              name = "accountActivityId",
              description = "ID of the account activity record.",
              example = "48104ecb-1343-4cc1-b6f2-e6cc88e9a80f")
          TypedId<AccountActivityId> accountActivityId,
      @PathVariable(value = "receiptId")
          @Parameter(
              required = true,
              name = "receiptId",
              description = "ID of the receipt record.",
              example = "48104ecb-1343-4cc1-b6f2-e6cc88e9a80f")
          TypedId<ReceiptId> receiptId) {
    CurrentUser currentUser = CurrentUser.get();

    receiptService.unlinkReceipt(
        currentUser.businessId(), currentUser.userId(), receiptId, accountActivityId);
  }

  @GetMapping("/receipts")
  List<Receipt> getReceipts() {
    CurrentUser currentUser = CurrentUser.get();

    return receiptService.getReceipts(currentUser.businessId(), currentUser.userId()).stream()
        .map(Receipt::of)
        .toList();
  }

  @DeleteMapping("/receipts/{receiptId}/delete")
  void deleteReceipt(
      @PathVariable(value = "receiptId")
          @Parameter(
              required = true,
              name = "receiptId",
              description = "ID of the receipt record.",
              example = "48104ecb-1343-4cc1-b6f2-e6cc88e9a80f")
          TypedId<ReceiptId> receiptId) {
    CurrentUser currentUser = CurrentUser.get();

    receiptService.deleteReceipt(currentUser.businessId(), currentUser.userId(), receiptId);
  }

  @PatchMapping("/{userId}/archive")
  boolean archiveUser(
      @PathVariable(value = Common.USER_ID)
          @Parameter(
              required = true,
              name = Common.USER_ID,
              description = "ID of the user record.",
              example = "48104ecb-1343-4cc1-b6f2-e6cc88e9a80f")
          TypedId<UserId> userId) {

    return userService.archiveUser(CurrentUser.get().businessId(), userId);
  }

  @PostMapping(value = "/export-csv")
  ResponseEntity<byte[]> exportCsv(@Validated @RequestBody SearchUserRequest request)
      throws IOException {
    TypedId<BusinessId> businessId = CurrentUser.get().businessId();

    // export must return all records, regardless if pagination is set in "view records" mode
    request.setPageRequest(new PageRequest(0, Integer.MAX_VALUE));

    byte[] csvFile = userService.createCSVFile(businessId, new UserFilterCriteria(request));
    HttpHeaders headers = new HttpHeaders();
    headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=employees.csv");
    headers.set(HttpHeaders.CONTENT_TYPE, "text/csv");
    headers.set(HttpHeaders.CONTENT_LENGTH, String.valueOf(csvFile.length));
    return new ResponseEntity<>(csvFile, headers, HttpStatus.OK);
  }
}
