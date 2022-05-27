package com.clearspend.capital.service.type;

public record CurrentJob() {

  static ThreadLocal<Boolean> scheduledJob = ThreadLocal.withInitial(() -> false);

  public static void setScheduledJob(boolean value) {
    scheduledJob.set(value);
  }

  public static Boolean getScheduledJob() {
    return scheduledJob.get();
  }
}
