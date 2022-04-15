package com.clearspend.capital.common.error;

import com.clearspend.capital.service.security.FailedPermissions;
import com.clearspend.capital.service.type.CurrentUser;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.springframework.security.access.AccessDeniedException;

public class PermissionFailureException extends RuntimeException {
  private static String createMessage(final List<FailedPermissions> failedPermissions) {
    final String messages =
        IntStream.range(0, failedPermissions.size())
            .mapToObj(index -> failedPermissions.get(index).composeFailureMessage(index))
            .collect(Collectors.joining("\n"));
    final String currentUser =
        Optional.ofNullable(CurrentUser.get())
            .map(Object::toString)
            .orElse("No Authentication Available");
    return """
            Access Denied: Permission Evaluation Failures
            %s
            %s
            """
        .formatted(currentUser, messages)
        .trim();
  }

  public PermissionFailureException(
      final List<FailedPermissions> failedPermissions, final AccessDeniedException cause) {
    super(createMessage(failedPermissions), cause);
  }
}
