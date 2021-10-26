package com.tranwall.capital.controller;

import com.tranwall.capital.common.typedid.data.AdjustmentId;
import com.tranwall.capital.common.typedid.data.BusinessId;
import com.tranwall.capital.common.typedid.data.CardId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.common.typedid.data.UserId;
import com.tranwall.capital.controller.type.Amount;
import com.tranwall.capital.controller.type.activity.AccountActivityResponse;
import com.tranwall.capital.controller.type.activity.CardAccountActivityRequest;
import com.tranwall.capital.controller.type.activity.PageRequest;
import com.tranwall.capital.controller.type.card.Card;
import com.tranwall.capital.controller.type.card.UserCardResponse;
import com.tranwall.capital.controller.type.receipt.CreateReceiptRequest;
import com.tranwall.capital.controller.type.receipt.CreateReceiptResponse;
import com.tranwall.capital.controller.type.user.CreateUserRequest;
import com.tranwall.capital.controller.type.user.CreateUserResponse;
import com.tranwall.capital.controller.type.user.User;
import com.tranwall.capital.controller.type.user.UserData;
import com.tranwall.capital.data.model.enums.UserType;
import com.tranwall.capital.service.AccountActivityFilterCriteria;
import com.tranwall.capital.service.AccountActivityService;
import com.tranwall.capital.service.CardService;
import com.tranwall.capital.service.ReceiptService;
import com.tranwall.capital.service.UserService;
import com.tranwall.capital.service.UserService.CreateUserRecord;
import io.swagger.annotations.ApiParam;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

  public static final String USER_NAME = "userName";
  public static final String BUSINESS_ID = "businessId";
  private final AccountActivityService accountActivityService;
  private final CardService cardService;
  private final ReceiptService receiptService;
  private final UserService userService;

  @PostMapping("")
  private CreateUserResponse createUser(
      @RequestHeader(name = BUSINESS_ID) TypedId<BusinessId> businessId,
      @RequestBody CreateUserRequest request)
      throws IOException {

    CreateUserRecord userServiceUser =
        userService.createUser(
            businessId,
            UserType.EMPLOYEE,
            request.getFirstName(),
            request.getLastName(),
            request.getAddress().toAddress(),
            request.getEmail(),
            request.getPhone(),
            request.isGeneratePassword());

    return new CreateUserResponse(userServiceUser.user().getId(), userServiceUser.password(), null);
  }

  @PostMapping("/bulk")
  private List<CreateUserResponse> bulkCreateUser(
      @RequestHeader(name = BUSINESS_ID) TypedId<BusinessId> businessId,
      @RequestBody List<CreateUserRequest> request) {

    List<CreateUserResponse> response = new ArrayList<>(request.size());
    for (CreateUserRequest createUserRequest : request) {
      try {
        CreateUserRecord userServiceUser =
            userService.createUser(
                businessId,
                UserType.EMPLOYEE,
                createUserRequest.getFirstName(),
                createUserRequest.getLastName(),
                createUserRequest.getAddress().toAddress(),
                createUserRequest.getEmail(),
                createUserRequest.getPhone(),
                createUserRequest.isGeneratePassword());
        response.add(
            new CreateUserResponse(
                userServiceUser.user().getId(), userServiceUser.password(), null));
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    return response;
  }

  @GetMapping("/{userId}")
  private User getUser(
      @PathVariable(value = "userId")
          @ApiParam(
              required = true,
              name = "userId",
              value = "ID of the user record.",
              example = "48104ecb-1343-4cc1-b6f2-e6cc88e9a80f")
          TypedId<UserId> userId) {
    return new User(userService.retrieveUser(userId));
  }

  @GetMapping(value = "/list")
  private List<UserData> getUsersByUserName(
      @RequestHeader(name = BUSINESS_ID) TypedId<BusinessId> businessId,
      @RequestParam(required = false, name = USER_NAME)
          @ApiParam(
              required = false,
              name = USER_NAME,
              value = "Name of the user.",
              example = "Ada")
          String userName) {
    List<UserData> userDataList;
    if (userName == null) {
      userDataList =
          userService.retrieveUsersForBusiness(businessId).stream()
              .map(UserData::new)
              .collect(Collectors.toList());
      return userDataList;
    } else {
      userDataList =
          userService.retrieveUsersForBusiness(businessId).stream()
              .filter(
                  user ->
                      user.getFirstName()
                              .toString()
                              .toLowerCase(Locale.ROOT)
                              .contains(userName.toLowerCase(Locale.ROOT))
                          || user.getLastName()
                              .toString()
                              .toLowerCase(Locale.ROOT)
                              .contains(userName.toLowerCase(Locale.ROOT)))
              .map(UserData::new)
              .collect(Collectors.toList());
    }
    return userDataList;
  }

  @GetMapping("/cards")
  private List<UserCardResponse> getUserCards(
      @RequestHeader(name = BUSINESS_ID) TypedId<BusinessId> businessId,
      @RequestHeader(name = "userId") TypedId<UserId> userId) {
    return cardService.getUserCards(businessId, userId).stream()
        .map(
            userCardRecord ->
                new UserCardResponse(
                    new Card(userCardRecord.card()),
                    Amount.of(userCardRecord.account().getLedgerBalance()),
                    Amount.of(userCardRecord.account().getAvailableBalance()),
                    userCardRecord.allocation().getName()))
        .toList();
  }

  @PostMapping("/cards/{cardId}/account-activity")
  private Page<AccountActivityResponse> getCardAccountActivity(
      @RequestHeader(name = BUSINESS_ID) TypedId<BusinessId> businessId,
      @RequestHeader(name = "userId") TypedId<UserId> userId,
      @PathVariable(value = "cardId")
          @ApiParam(
              required = true,
              name = "cardId",
              value = "ID of the card record.",
              example = "48104ecb-1343-4cc1-b6f2-e6cc88e9a80f")
          TypedId<CardId> cardId,
      @Validated @RequestBody CardAccountActivityRequest request) {
    return accountActivityService.getCardAccountActivity(
        businessId,
        userId,
        cardId,
        new AccountActivityFilterCriteria(
            cardId,
            request.getType(),
            request.getFrom(),
            request.getTo(),
            PageRequest.toPageToken(request.getPageRequest())));
  }

  @PostMapping("/cards/{cardId}/adjustments/{adjustmentId}/receipts")
  private CreateReceiptResponse createReceipt(
      @RequestHeader(name = BUSINESS_ID) TypedId<BusinessId> businessId,
      @RequestHeader(name = "userId") TypedId<UserId> userId,
      @PathVariable(value = "cardId")
          @ApiParam(
              required = true,
              name = "cardId",
              value = "ID of the card record.",
              example = "48104ecb-1343-4cc1-b6f2-e6cc88e9a80f")
          TypedId<CardId> cardId,
      @PathVariable(value = "adjustmentId")
          @ApiParam(
              required = true,
              name = "adjustmentId",
              value = "ID of the adjustment record.",
              example = "48104ecb-1343-4cc1-b6f2-e6cc88e9a80f")
          TypedId<AdjustmentId> adjustmentId,
      @RequestParam("receipt") @ApiParam("receipt") MultipartFile receiptFile,
      @Validated @RequestBody CreateReceiptRequest request) {
    return new CreateReceiptResponse(
        receiptService
            .createReceipt(
                businessId,
                userId,
                cardId,
                adjustmentId,
                request.getAmount().toAmount(),
                receiptFile)
            .getId());
  }
}
