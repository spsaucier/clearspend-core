package com.tranwall.capital.controller;

import static com.tranwall.capital.controller.Common.USER_ID;
import static com.tranwall.capital.controller.Common.USER_NAME;

import com.tranwall.capital.common.typedid.data.BusinessId;
import com.tranwall.capital.common.typedid.data.CardId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.common.typedid.data.UserId;
import com.tranwall.capital.controller.type.Amount;
import com.tranwall.capital.controller.type.CurrentUser;
import com.tranwall.capital.controller.type.activity.AccountActivityResponse;
import com.tranwall.capital.controller.type.activity.CardAccountActivityRequest;
import com.tranwall.capital.controller.type.activity.PageRequest;
import com.tranwall.capital.controller.type.card.Card;
import com.tranwall.capital.controller.type.card.UserCardResponse;
import com.tranwall.capital.controller.type.user.CreateUserRequest;
import com.tranwall.capital.controller.type.user.CreateUserResponse;
import com.tranwall.capital.controller.type.user.User;
import com.tranwall.capital.controller.type.user.UserData;
import com.tranwall.capital.data.model.BusinessOwner;
import com.tranwall.capital.data.model.enums.UserType;
import com.tranwall.capital.service.AccountActivityFilterCriteria;
import com.tranwall.capital.service.AccountActivityService;
import com.tranwall.capital.service.BusinessOwnerService;
import com.tranwall.capital.service.BusinessProspectService;
import com.tranwall.capital.service.CardService;
import com.tranwall.capital.service.ReceiptService;
import com.tranwall.capital.service.UserService;
import com.tranwall.capital.service.UserService.CreateUserRecord;
import io.swagger.v3.oas.annotations.Parameter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
  private CreateUserResponse createUser(@RequestBody CreateUserRequest request) throws IOException {

    CreateUserRecord userServiceUser =
        userService.createUser(
            CurrentUser.get().businessId(),
            UserType.EMPLOYEE,
            request.getFirstName(),
            request.getLastName(),
            request.getAddress() != null ? request.getAddress().toAddress() : null,
            request.getEmail(),
            request.getPhone(),
            request.isGeneratePassword(),
            null);

    return new CreateUserResponse(userServiceUser.user().getId(), userServiceUser.password(), null);
  }

  @PostMapping("/bulk")
  private List<CreateUserResponse> bulkCreateUser(@RequestBody List<CreateUserRequest> request) {

    List<CreateUserResponse> response = new ArrayList<>(request.size());
    for (CreateUserRequest createUserRequest : request) {
      try {
        CreateUserRecord userServiceUser =
            userService.createUser(
                CurrentUser.get().businessId(),
                UserType.EMPLOYEE,
                createUserRequest.getFirstName(),
                createUserRequest.getLastName(),
                createUserRequest.getAddress() != null
                    ? createUserRequest.getAddress().toAddress()
                    : null,
                createUserRequest.getEmail(),
                createUserRequest.getPhone(),
                createUserRequest.isGeneratePassword(),
                null);
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
      @PathVariable(value = USER_ID)
          @Parameter(
              required = true,
              name = USER_ID,
              description = "ID of the user record.",
              example = "48104ecb-1343-4cc1-b6f2-e6cc88e9a80f")
          TypedId<UserId> userId) {
    return new User(userService.retrieveUser(userId));
  }

  @GetMapping
  public User currentUser() {
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
  private List<UserData> getUsersByUserName(
      @RequestParam(required = false, name = USER_NAME)
          @Parameter(name = USER_NAME, description = "Name of the user.", example = "Ada")
          String userName) {
    TypedId<BusinessId> businessId = CurrentUser.get().businessId();
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
      @RequestHeader(name = "userId") TypedId<UserId> userId) {
    return cardService.getUserCards(CurrentUser.get().businessId(), userId).stream()
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
      @RequestHeader(name = USER_ID) TypedId<UserId> userId,
      @PathVariable(value = "cardId")
          @Parameter(
              required = true,
              name = "cardId",
              description = "ID of the card record.",
              example = "48104ecb-1343-4cc1-b6f2-e6cc88e9a80f")
          TypedId<CardId> cardId,
      @Validated @RequestBody CardAccountActivityRequest request) {
    return accountActivityService.getCardAccountActivity(
        CurrentUser.get().businessId(),
        userId,
        cardId,
        new AccountActivityFilterCriteria(
            cardId,
            request.getType(),
            request.getFrom(),
            request.getTo(),
            PageRequest.toPageToken(request.getPageRequest())));
  }
}
