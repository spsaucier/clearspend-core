package com.clearspend.capital;

import org.testcontainers.containers.GenericContainer;

public class DoNothingContainer<T extends GenericContainer<T>> extends GenericContainer<T> {
  @Override
  public void start() {}
}
