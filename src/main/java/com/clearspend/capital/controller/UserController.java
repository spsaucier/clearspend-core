package com.clearspend.capital.controller;

import com.clearspend.capital.common.typedid.data.AccountActivityId;
import com.clearspend.capital.common.typedid.data.CardId;
import com.clearspend.capital.common.typedid.data.ReceiptId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.controller.type.Amount;
import com.clearspend.capital.controller.type.PagedData;
import com.clearspend.capital.controller.type.activity.AccountActivityRequest;
import com.clearspend.capital.controller.type.activity.AccountActivityResponse;
import com.clearspend.capital.controller.type.activity.UpdateAccountActivityRequest;
import com.clearspend.capital.controller.type.card.ActivateCardRequest;
import com.clearspend.capital.controller.type.card.Card;
import com.clearspend.capital.controller.type.card.CardAccount;
import com.clearspend.capital.controller.type.card.CardAndAccount;
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
import com.clearspend.capital.data.repository.CardRepositoryCustom.CardDetailsRecord;
import com.clearspend.capital.service.AccountActivityFilterCriteria;
import com.clearspend.capital.service.AccountActivityService;
import com.clearspend.capital.service.AccountService;
import com.clearspend.capital.service.AllocationService;
import com.clearspend.capital.service.BusinessOwnerService;
import com.clearspend.capital.service.BusinessProspectService;
import com.clearspend.capital.service.BusinessProspectService.OnboardingBusinessProspectMethod;
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
import org.springframework.dao.DataIntegrityViolationException;
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

  private final AccountService accountService;
  private final AccountActivityService accountActivityService;
  private final AllocationService allocationService;
  private final BusinessOwnerService businessOwnerService;
  private final BusinessProspectService businessProspectService;
  private final CardService cardService;
  private final ReceiptService receiptService;
  private final UserService userService;

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
    try {
      request.setBusinessId(CurrentUser.getBusinessId());
      request.setUserId(userId);
      final CreateUpdateUserRecord updateUserRecord = userService.updateUser(request);
      return new UpdateUserResponse(updateUserRecord.user().getId(), null);
    } catch (IllegalArgumentException e) {
      throw new DataIntegrityViolationException(e.getMessage(), e);
    }
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

  @OnboardingBusinessProspectMethod(
      explanation =
          """
              This one is messed up. Yes, we have a CurrentUser,
              which means we have a valid JWT. However, the user
              in question is not 'real' yet if this is called during
              the onboarding process, ie they are not in our Postgres DB.
              That means we cannot evaluate permissions against them, hence
              the RestrictedApi approach.
              """,
      reviewer = "Craig Miller")
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
    return cardService.getCardsForCurrentUser().stream().map(CardDetailsResponse::of).toList();
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
    CardDetailsRecord userCardRecord = cardService.getMyCard(cardId);

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
    return new Card(
        cardService.blockCard(
            cardService.retrieveCard(CurrentUser.getBusinessId(), cardId),
            request.getStatusReason()));
  }

  @PatchMapping("/cards/{cardId}/activate")
  Card activateMyCard(
      @PathVariable(value = "cardId")
          @Parameter(
              required = true,
              name = "cardId",
              description = "ID of the card record.",
              example = "48104ecb-1343-4cc1-b6f2-e6cc88e9a80f")
          TypedId<CardId> cardId,
      @Validated @RequestBody ActivateCardRequest request) {
    return new Card(
        cardService.activateMyCard(
            cardService.getMyCardByIdAndLastFour(cardId, request.getLastFour()),
            request.getStatusReason()));
  }

  @PatchMapping("/cards/activate")
  Card activateMyCards(@Validated @RequestBody ActivateCardRequest request) {
    return new Card(
        cardService.activateMyCards(
            cardService.getMyUnactivatedCardsByLastFour(request.getLastFour()),
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

    return new Card(
        cardService.unblockCard(
            cardService.retrieveCard(CurrentUser.getBusinessId(), cardId),
            request.getStatusReason()));
  }

  @PatchMapping("/cards/{cardId}/cancel")
  Card cancelCard(
      @PathVariable(value = "cardId")
          @Parameter(
              required = true,
              name = "cardId",
              description = "ID of the card record.",
              example = "48104ecb-1343-4cc1-b6f2-e6cc88e9a80f")
          TypedId<CardId> cardId,
      @Validated @RequestBody UpdateCardStatusRequest request) {
    return new Card(
        cardService.cancelCard(
            cardService.retrieveCard(CurrentUser.getBusinessId(), cardId),
            request.getStatusReason()));
  }

  @PatchMapping("/cards/{cardId}/unlink")
  CardAndAccount unlinkCard(
      @PathVariable(value = "cardId")
          @Parameter(
              required = true,
              name = "cardId",
              description = "ID of the card record.",
              example = "48104ecb-1343-4cc1-b6f2-e6cc88e9a80f")
          final TypedId<CardId> cardId) {
    return CardAndAccount.of(
        cardService.unlinkCard(cardService.retrieveCard(CurrentUser.getBusinessId(), cardId)));
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
    return cardService
        .getCardAccounts(cardService.retrieveCard(CurrentUser.getBusinessId(), cardId), type)
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
    return new Card(
        cardService.updateCardAccount(
            cardService.retrieveCard(CurrentUser.getBusinessId(), cardId),
            allocationService.getSingleAllocation(
                CurrentUser.getBusinessId(), request.getAllocationId())));
  }

  @PostMapping("/cards/{cardId}/account-activity")
  PagedData<AccountActivityResponse> getCardAccountActivity(
      @PathVariable(value = "cardId")
          @Parameter(
              required = true,
              name = "cardId",
              description = "ID of the card record",
              example = "48104ecb-1343-4cc1-b6f2-e6cc88e9a80f")
          TypedId<CardId> cardId,
      @Validated @RequestBody AccountActivityRequest request) {
    // We get the Card ID from both the AccountActivityRequest as well as
    // the Path Variable. We're currently taking the first, starting with
    // the value found in the AccountActivityRequest object.

    Page<AccountActivity> accountActivities =
        accountActivityService.find(
            CurrentUser.get().businessId(),
            new AccountActivityFilterCriteria(
                CurrentUser.get().businessId(),
                request.getAllocationId(),
                request.getUserId(),
                request.getCardId() != null ? request.getCardId() : cardId,
                request.getTypes(),
                request.getSearchText(),
                request.getFrom(),
                request.getTo(),
                request.getStatuses(),
                request.getFilterAmount() == null ? null : request.getFilterAmount().getMin(),
                request.getFilterAmount() == null ? null : request.getFilterAmount().getMax(),
                request.getCategories(),
                request.getWithReceipt(),
                request.getWithoutReceipt(),
                request.getSyncStatuses(),
                request.getMissingExpenseCategory(),
                PageRequest.toPageToken(request.getPageRequest())));

    return PagedData.of(accountActivities, AccountActivityResponse::new);
  }

  @Deprecated()
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

    final AccountActivityFilterCriteria criteria =
        new AccountActivityFilterCriteria(
            cardId, type != null ? List.of(type) : List.of(), dateFrom, dateTo, pageRequest);

    Page<AccountActivity> accountActivity =
        accountActivityService
            .getCardAccountActivity(
                currentUser.businessId(), currentUser.userId(), cardId, criteria)
            .activityPage();

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

    return new AccountActivityResponse(
        accountActivityService.getAccountActivity(accountActivityId));
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

    return new AccountActivityResponse(
        accountActivityService.updateAccountActivity(
            accountActivityService.getAccountActivity(accountActivityId),
            request.getNotes(),
            request.getExpenseCategoryId(),
            request.getSupplierId(),
            request.getSupplierName()));
  }

  @PostMapping("/account-activity/{accountActivityId}/unlock")
  AccountActivityResponse unlockAccountActivity(
      @PathVariable(value = "accountActivityId")
          @Parameter(
              required = true,
              name = "accountActivityId",
              description = "ID of the account activity record.",
              example = "48104ecb-1343-4cc1-b6f2-e6cc88e9a80f")
          TypedId<AccountActivityId> accountActivityId) {
    return new AccountActivityResponse(
        accountActivityService.unlockAccountActivityForSync(
            CurrentUser.getBusinessId(), accountActivityId));
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
    receiptService.linkReceipt(
        receiptService.getReceipt(receiptId),
        accountActivityService.getAccountActivity(accountActivityId));
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
        receiptService.getReceipt(receiptId),
        accountActivityService.getAccountActivity(accountActivityId));
  }

  @GetMapping("/receipts")
  List<Receipt> getReceipts() {
    CurrentUser currentUser = CurrentUser.get();

    return receiptService.getReceiptsForCurrentUser().stream().map(Receipt::of).toList();
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

    receiptService.deleteReceipt(receiptService.getReceipt(receiptId));
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
