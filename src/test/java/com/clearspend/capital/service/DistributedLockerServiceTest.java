package com.clearspend.capital.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.util.function.ThrowableFunctions;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class DistributedLockerServiceTest extends BaseCapitalTest {

  @Autowired private DistributedLockerService lockerService;

  @Test
  @SneakyThrows
  void concurrent_SameLockShouldBeSequenced() {
    assertConcurrentExecution("lock", "lock", false);
  }

  @Test
  @SneakyThrows
  void concurrent_DifferentLocksShouldBeParalleled() {
    assertConcurrentExecution("lock1", "lock2", true);
  }

  @SneakyThrows
  private void assertConcurrentExecution(
      String firstLockId, String secondLockId, boolean assertConcurrent) {
    CyclicBarrier barrier = new CyclicBarrier(2);
    AtomicBoolean runningThreadFlag = new AtomicBoolean(false);
    AtomicBoolean concurrentThreadDetected = new AtomicBoolean(false);

    Thread thread1 =
        new Thread(
            () ->
                ThrowableFunctions.sneakyThrows(
                    () -> {
                      barrier.await();
                      lockerService.doWithLock(
                          firstLockId,
                          () ->
                              ThrowableFunctions.sneakyThrows(
                                  () -> {
                                    concurrentThreadDetected.compareAndSet(
                                        false, !runningThreadFlag.compareAndSet(false, true));
                                    Thread.sleep(1000);
                                    runningThreadFlag.set(false);
                                  }));
                    }));

    Thread thread2 =
        new Thread(
            () ->
                ThrowableFunctions.sneakyThrows(
                    () -> {
                      barrier.await();
                      lockerService.doWithLock(
                          secondLockId,
                          () ->
                              ThrowableFunctions.sneakyThrows(
                                  () -> {
                                    concurrentThreadDetected.compareAndSet(
                                        false, !runningThreadFlag.compareAndSet(false, true));
                                    Thread.sleep(500);
                                    runningThreadFlag.set(false);
                                  }));
                    }));

    thread1.start();
    thread2.start();

    thread2.join();
    thread1.join();

    assertThat(concurrentThreadDetected.get()).isEqualTo(assertConcurrent);
  }
}
