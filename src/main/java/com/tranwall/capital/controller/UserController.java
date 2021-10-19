package com.tranwall.capital.controller;

import com.tranwall.capital.common.typedid.data.BusinessId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.common.typedid.data.UserId;
import com.tranwall.capital.controller.type.user.CreateUserRequest;
import com.tranwall.capital.controller.type.user.CreateUserResponse;
import com.tranwall.capital.controller.type.user.User;
import com.tranwall.capital.data.model.enums.UserType;
import com.tranwall.capital.service.UserService;
import com.tranwall.capital.service.UserService.CreateUserRecord;
import io.swagger.annotations.ApiParam;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

  private final UserService userService;

  @PostMapping("")
  private CreateUserResponse createUser(
      @RequestHeader(name = "businessId") TypedId<BusinessId> businessId,
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
      @RequestHeader(name = "businessId") TypedId<BusinessId> businessId,
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
}
