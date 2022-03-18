package com.clearspend.capital.common.advice;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

@Aspect
@Component
@ConditionalOnExpression(
    "#{${logging.execution-time.enabled} and '${logging.execution-time.mode}' == 'all'}")
public class AllExecutionTimeLoggerAdvice extends AbstractExecutionTimeLoggerAdvice {

  public AllExecutionTimeLoggerAdvice(
      LoggingExecutionTimeProperties loggingExecutionTimeProperties, ObjectMapper objectMapper) {
    super(loggingExecutionTimeProperties, objectMapper);
  }

  @Override
  @Around(
      "(within(com.clearspend.capital..*) || execution(public !void org.springframework.data.repository.Repository+.*(..)))"
          + "&& !within(com.clearspend.capital.*)"
          + "&& !within(com.clearspend.capital.common.advice..*)"
          + "&& !within(com.clearspend.capital.configuration..*)"
          + "&& !within(com.clearspend.capital.crypto..*)"
          + "&& !within(com.clearspend..*Properties*)")
  public Object logExecutionTimeAdviceEntry(ProceedingJoinPoint joinPoint) throws Throwable {
    return super.logExecutionTimeAdviceEntry(joinPoint);
  }

  @Override
  protected Object logExecutionTimeGeneric(ProceedingJoinPoint joinPoint) throws Throwable {
    return super.logExecutionTimeGeneric(joinPoint);
  }
}
