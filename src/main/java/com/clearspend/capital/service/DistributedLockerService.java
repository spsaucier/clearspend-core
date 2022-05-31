package com.clearspend.capital.service;

import com.clearspend.capital.common.data.model.TypedObject;
import com.clearspend.capital.permissioncheck.annotations.OpenAccessAPI;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DistributedLockerService {

  private final RedissonClient redissonClient;

  @OpenAccessAPI(
      reviewer = "vakimov",
      explanation = "Utility method that doesn't work with any data")
  public void doWithLock(String lockId, Runnable runnable) {
    doWithLock(
        lockId,
        () -> {
          runnable.run();
          return null;
        });
  }

  @OpenAccessAPI(
      reviewer = "vakimov",
      explanation = "Utility method that doesn't work with any data")
  public <T> T doWithLock(String lockId, Supplier<T> supplier) {
    RLock lock = redissonClient.getFairLock(lockId);
    lock.lock();
    try {
      return supplier.get();
    } finally {
      lock.unlock();
    }
  }

  @OpenAccessAPI(
      reviewer = "vakimov",
      explanation = "Utility method that doesn't work with any data")
  public <T> void doWithLock(TypedObject<T> object, Runnable runnable) {
    doWithLock(object.getId().toString(), runnable);
  }

  @OpenAccessAPI(
      reviewer = "vakimov",
      explanation = "Utility method that doesn't work with any data")
  public <T, O> O doWithLock(TypedObject<T> object, Supplier<O> supplier) {
    return doWithLock(object.getId().toString(), supplier);
  }
}
