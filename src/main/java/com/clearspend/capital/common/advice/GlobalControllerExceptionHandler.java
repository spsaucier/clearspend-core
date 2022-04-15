package com.clearspend.capital.common.advice;

import com.clearspend.capital.client.stripe.StripeClientException;
import com.clearspend.capital.common.error.AmountException;
import com.clearspend.capital.common.error.CurrencyMismatchException;
import com.clearspend.capital.common.error.DataAccessViolationException;
import com.clearspend.capital.common.error.FusionAuthException;
import com.clearspend.capital.common.error.IdMismatchException;
import com.clearspend.capital.common.error.InvalidKycDataException;
import com.clearspend.capital.common.error.InvalidRequestException;
import com.clearspend.capital.common.error.InvalidStateException;
import com.clearspend.capital.common.error.OperationDeclinedException;
import com.clearspend.capital.common.error.PermissionFailureException;
import com.clearspend.capital.common.error.ReLinkException;
import com.clearspend.capital.common.error.RecordNotFoundException;
import com.clearspend.capital.service.security.FailedPermissions;
import com.clearspend.capital.service.security.UserRolesAndPermissionsCache;
import com.clearspend.capital.service.type.StripeAccountFieldsToClearspendBusinessFields;
import com.clearspend.capital.service.type.StripePersonFieldsToClearspendOwnerFields;
import com.inversoft.error.Errors;
import com.stripe.exception.StripeException;
import com.stripe.model.StripeError;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
@Slf4j
public class GlobalControllerExceptionHandler {

  record ControllerError(String message, String param) {

    public ControllerError(String message) {
      this(message, "");
    }
  }

  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ExceptionHandler({
    AmountException.class,
    CurrencyMismatchException.class,
    IdMismatchException.class,
    InvalidRequestException.class,
    OperationDeclinedException.class
  })
  public @ResponseBody ControllerError handleCapitalException(Exception exception) {
    log.error(String.format("%s exception processing request", exception.getClass()), exception);
    return new ControllerError(exception.getMessage());
  }

  @ExceptionHandler({FusionAuthException.class})
  public ResponseEntity<Errors> handleLoginException(FusionAuthException exception) {
    Optional.ofNullable(exception.getCause()).ifPresent(e -> log.error(e.getMessage(), e));
    return new ResponseEntity<>(
        exception.getErrors(), HttpStatus.valueOf(exception.getHttpStatus()));
  }

  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ExceptionHandler({DataIntegrityViolationException.class, InvalidStateException.class})
  public @ResponseBody ControllerError handleDataIntegrityViolationException(Exception exception) {
    log.error(String.format("%s exception processing request", exception.getClass()), exception);
    Throwable root = getRootCause(exception);
    return new ControllerError(root.getMessage());
  }

  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ExceptionHandler({StripeClientException.class})
  public @ResponseBody ControllerError handleStripeClientException(Exception exception) {
    Throwable root = getRootCause(exception);
    log.warn(String.format("%s. Reson: %s", exception.getClass(), root.getMessage()), exception);

    StripeError stripeError = ((StripeException) root).getStripeError();
    String param = stripeError.getParam();

    if (StringUtils.isEmpty(param)) {

      return new ControllerError(root.getMessage());
    }

    if (param.contains("company") || param.contains("business_profile")) {
      param =
          StripeAccountFieldsToClearspendBusinessFields.fromStripeField(
              param.substring(param.indexOf("[") + 1, param.indexOf("]")));
    }
    if (param.contains("person")) {
      param =
          StripePersonFieldsToClearspendOwnerFields.fromStripeField(
              param.substring(param.indexOf("[") + 1, param.indexOf("]")));
    }

    return new ControllerError(root.getMessage(), param);
  }

  private static Throwable getRootCause(Throwable exception) {
    Throwable root = exception;
    while (root.getCause() != null) {
      root = root.getCause();
    }
    return root;
  }

  @ResponseStatus(HttpStatus.FORBIDDEN)
  @ExceptionHandler(DataAccessViolationException.class)
  public @ResponseBody ControllerError dataAccessViolationException(
      final DataAccessViolationException exception) {
    log.error(String.format("%s exception processing request", exception.getClass()), exception);
    return new ControllerError("");
  }

  @ResponseStatus(HttpStatus.FORBIDDEN)
  @ExceptionHandler(AccessDeniedException.class)
  public @ResponseBody ControllerError handleAccessDeniedException(
      final AccessDeniedException exception) {
    try {
      final PermissionFailureException ex =
          new PermissionFailureException(getFailedPermissions(), exception);
      log.error("%s exception processing request".formatted(ex.getClass().getName()), ex);
    } catch (Exception ex) {
      throw new RuntimeException("Error handling permission failure", ex);
    }
    return new ControllerError("");
  }

  private List<FailedPermissions> getFailedPermissions() {
    return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
        .flatMap(auth -> Optional.ofNullable(auth.getDetails()))
        .filter(details -> details instanceof UserRolesAndPermissionsCache)
        .map(details -> (UserRolesAndPermissionsCache) details)
        .map(UserRolesAndPermissionsCache::getFailedPermissions)
        .orElse(List.of());
  }

  @ResponseStatus(HttpStatus.NOT_FOUND)
  @ExceptionHandler({RecordNotFoundException.class})
  public @ResponseBody void handleRecordNotFoundException(RecordNotFoundException exception) {
    if (exception.isPrintStackTrace()) {
      log.error(String.format("%s exception processing request", exception.getClass()), exception);
    } else {
      log.error(
          String.format(
              "%s exception processing request: %s", exception.getClass(), exception.getMessage()));
    }
  }

  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  @ExceptionHandler(Exception.class)
  public @ResponseBody ControllerError handleException(Exception exception) {
    log.error(String.format("%s exception processing request", exception.getClass()), exception);
    return new ControllerError(exception.getMessage());
  }

  @ResponseStatus(HttpStatus.PRECONDITION_REQUIRED)
  @ExceptionHandler(ReLinkException.class)
  public void handleReLink() {}

  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ExceptionHandler(InvalidKycDataException.class)
  public @ResponseBody ControllerError handleKycErrors(InvalidKycDataException exception) {
    log.error(String.format("%s exception processing request", exception.getClass()), exception);
    return new ControllerError(exception.getMessage(), exception.getParam());
  }
}
