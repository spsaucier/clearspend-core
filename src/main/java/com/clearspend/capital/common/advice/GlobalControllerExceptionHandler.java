package com.clearspend.capital.common.advice;

import com.clearspend.capital.common.error.AmountException;
import com.clearspend.capital.common.error.CurrencyMismatchException;
import com.clearspend.capital.common.error.ForbiddenException;
import com.clearspend.capital.common.error.IdMismatchException;
import com.clearspend.capital.common.error.InsufficientFundsException;
import com.clearspend.capital.common.error.InvalidRequestException;
import com.clearspend.capital.common.error.RecordNotFoundException;
import com.clearspend.capital.common.error.TypeMismatchException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
@Slf4j
public class GlobalControllerExceptionHandler {

  record ControllerError(String message) {}

  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ExceptionHandler({
    AmountException.class,
    CurrencyMismatchException.class,
    IdMismatchException.class,
    InsufficientFundsException.class,
    InvalidRequestException.class,
    TypeMismatchException.class
  })
  public @ResponseBody ControllerError handleCapitalException(Exception exception) {
    log.error(String.format("%s exception processing request", exception.getClass()), exception);
    return new ControllerError(exception.getMessage());
  }

  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ExceptionHandler(DataIntegrityViolationException.class)
  public @ResponseBody ControllerError handleDataIntegrityViolationException(
      DataIntegrityViolationException exception) {
    log.error(String.format("%s exception processing request", exception.getClass()), exception);
    return new ControllerError(exception.getMostSpecificCause().getMessage());
  }

  @ResponseStatus(HttpStatus.UNAUTHORIZED)
  @ExceptionHandler(ForbiddenException.class)
  public @ResponseBody ControllerError handleForbiddenException(ForbiddenException exception) {
    log.error(String.format("%s exception processing request", exception.getClass()), exception);
    return new ControllerError("");
  }

  @ResponseStatus(HttpStatus.NO_CONTENT)
  @ExceptionHandler({RecordNotFoundException.class})
  public @ResponseBody void handleRecordNotFoundException(Exception exception) {
    log.error(String.format("%s exception processing request", exception.getClass()), exception);
  }

  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  @ExceptionHandler(Exception.class)
  public @ResponseBody ControllerError handleException(Exception exception) {
    log.error(String.format("%s exception processing request", exception.getClass()), exception);
    return new ControllerError(exception.getMessage());
  }
}
