package com.clearspend.capital.common.advice;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Stopwatch;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractExecutionTimeLoggerAdvice {

  private static final ThreadLocal<Task> taskContext = new ThreadLocal<>();

  private final LoggingExecutionTimeProperties loggingExecutionTimeProperties;
  private final ObjectMapper objectMapper;

  /** Should cover all spring methods serving as an entry point for the tree recording */
  public Object logExecutionTimeAdviceEntry(ProceedingJoinPoint joinPoint) throws Throwable {
    // calling annotated method from another annotated method
    if (taskContext.get() != null) {
      return logExecutionTimeGeneric(joinPoint);
    }

    Task task = new Task(joinPoint);
    taskContext.set(task);

    try {
      task.startTimer();

      return joinPoint.proceed();
    } finally {
      task.stopTimer();
      taskContext.remove();

      if (task.getTotalTime() > loggingExecutionTimeProperties.getThreshold()) {
        switch (loggingExecutionTimeProperties.getLevel()) {
          case TRACE -> {
            if (log.isTraceEnabled()) log.trace(objectMapper.writeValueAsString(task));
          }
          case DEBUG -> {
            if (log.isDebugEnabled()) log.debug(objectMapper.writeValueAsString(task));
          }
          case INFO -> {
            if (log.isInfoEnabled()) log.info(objectMapper.writeValueAsString(task));
          }
          case WARN -> {
            if (log.isWarnEnabled()) log.warn(objectMapper.writeValueAsString(task));
          }
          case ERROR -> {
            if (log.isErrorEnabled()) log.error(objectMapper.writeValueAsString(task));
          }
        }
      }
    }
  }

  /**
   * Should cover all spring methods that we want to be recorded other than the ones covered by
   * logExecutionTimeAdviceEntry
   */
  protected Object logExecutionTimeGeneric(ProceedingJoinPoint joinPoint) throws Throwable {
    Task parentTask = taskContext.get();
    // if parent task is present - substitute with a new one and proceed further. Otherwise we
    // should skip logging since we are not running inside an annotated/traced tree call
    if (parentTask != null) {
      Task currentTask = new Task(joinPoint);
      parentTask.addSubtask(currentTask);
      taskContext.set(currentTask);

      try {
        currentTask.startTimer();
        return joinPoint.proceed();
      } finally {
        currentTask.stopTimer();
        taskContext.set(parentTask);
      }
    } else {
      return joinPoint.proceed();
    }
  }

  private static class Task {
    @Getter private final String name;

    @JsonIgnore private final Stopwatch stopwatch;

    @Getter
    @JsonInclude(Include.NON_EMPTY)
    private final List<Task> children;

    public Task(ProceedingJoinPoint joinPoint) {
      String className =
          joinPoint.getStaticPart().getSourceLocation().getWithinType().getSimpleName();
      String methodName = joinPoint.getStaticPart().getSignature().getName();

      name = "%s::%s".formatted(className, methodName);
      stopwatch = Stopwatch.createUnstarted();
      children = new ArrayList<>();
    }

    @JsonProperty("selfTime")
    public long getSelfTime() {
      long elapsed = stopwatch.elapsed(TimeUnit.MILLISECONDS);
      for (Task child : children) {
        elapsed -= child.getTotalTime();
      }

      return elapsed;
    }

    @JsonProperty("totalTime")
    public long getTotalTime() {
      return stopwatch.elapsed(TimeUnit.MILLISECONDS);
    }

    public void addSubtask(Task task) {
      children.add(task);
    }

    public void startTimer() {
      stopwatch.start();
    }

    public void stopTimer() {
      stopwatch.stop();
    }
  }
}
