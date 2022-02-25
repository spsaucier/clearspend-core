package com.clearspend.capital.common.advice;

import com.clearspend.capital.common.error.AmountException;
import com.clearspend.capital.common.error.CurrencyMismatchException;
import com.clearspend.capital.common.error.DataAccessViolationException;
import com.clearspend.capital.common.error.ForbiddenException;
import com.clearspend.capital.common.error.IdMismatchException;
import com.clearspend.capital.common.error.InsufficientFundsException;
import com.clearspend.capital.common.error.InvalidRequestException;
import com.clearspend.capital.common.error.InvalidStateException;
import com.clearspend.capital.common.error.LoginException;
import com.clearspend.capital.common.error.RecordNotFoundException;
import com.clearspend.capital.common.error.TwoFactorAuthenticationRequired;
import com.inversoft.error.Errors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
@Slf4j
public class GlobalControllerExceptionHandler {

  record ControllerError(String message) {}

  record TwoFactorAuthenticationStart(String twoFactorId) {}

  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ExceptionHandler({
    AmountException.class,
    CurrencyMismatchException.class,
    IdMismatchException.class,
    InsufficientFundsException.class,
    InvalidRequestException.class
  })
  public @ResponseBody ControllerError handleCapitalException(Exception exception) {
    log.error(String.format("%s exception processing request", exception.getClass()), exception);
    return new ControllerError(exception.getMessage());
  }

  @ExceptionHandler({LoginException.class})
  public ResponseEntity<Errors> handleLoginException(LoginException exception) {
    return new ResponseEntity<>(
        exception.getErrors(), HttpStatus.valueOf(exception.getHttpStatus()));
  }

  @ExceptionHandler({TwoFactorAuthenticationRequired.class})
  public ResponseEntity<TwoFactorAuthenticationStart> startTwoFactorAuthentication(
      TwoFactorAuthenticationRequired twoFactorStart) {
    return ResponseEntity.status(200)
        .body(new TwoFactorAuthenticationStart(twoFactorStart.getTwoFactorId()));
  }

  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ExceptionHandler({DataIntegrityViolationException.class, InvalidStateException.class})
  public @ResponseBody ControllerError handleDataIntegrityViolationException(
      DataIntegrityViolationException exception) {
    log.error(String.format("%s exception processing request", exception.getClass()), exception);
    return new ControllerError(exception.getMostSpecificCause().getMessage());
  }

  @ResponseStatus(HttpStatus.FORBIDDEN)
  @ExceptionHandler({DataAccessViolationException.class, ForbiddenException.class})
  public @ResponseBody ControllerError handleForbiddenException(ForbiddenException exception) {
    log.error(String.format("%s exception processing request", exception.getClass()), exception);
    return new ControllerError("");
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
}
