package com.clearspend.capital;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.LoggerFactory;

public class TestAppender extends ListAppender<ILoggingEvent> implements AutoCloseable {

  private final Logger logger;
  private final Level preTestRootLogLevel;
  private final Level preTestClassLogLevel;
  private final Logger rootLogger;

  private TestAppender(Class<?> clazz, Level testLogLevel) {
    rootLogger = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
    this.preTestRootLogLevel = rootLogger.getLevel();
    rootLogger.setLevel(testLogLevel);

    logger = (Logger) LoggerFactory.getLogger(clazz);
    preTestClassLogLevel = logger.getLevel();
    logger.setLevel(testLogLevel);
    logger.addAppender(this);
  }

  public boolean contains(String string, Level level) {
    return this.list.stream()
        .anyMatch(event -> event.toString().contains(string) && event.getLevel().equals(level));
  }

  public int countEventsForLogger(String loggerName) {
    return (int)
        this.list.stream().filter(event -> event.getLoggerName().contains(loggerName)).count();
  }

  public List<ILoggingEvent> search(String string) {
    return this.list.stream()
        .filter(event -> event.toString().contains(string))
        .collect(Collectors.toList());
  }

  public int getSize() {
    return this.list.size();
  }

  public List<ILoggingEvent> getLoggedEvents() {
    return Collections.unmodifiableList(this.list);
  }

  public static TestAppender watching(Class<?> clazz, Level testLogLevel) {
    TestAppender testAppender = new TestAppender(clazz, testLogLevel);
    testAppender.start();
    return testAppender;
  }

  @Override
  public void close() {
    this.stop();
    logger.detachAppender(this);
    rootLogger.setLevel(preTestRootLogLevel);
    logger.setLevel(preTestClassLogLevel);
  }
}
