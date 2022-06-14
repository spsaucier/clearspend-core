package com.clearspend.capital.service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.states.EnqueuedState;
import org.jobrunr.jobs.states.JobState;
import org.jobrunr.jobs.states.ScheduledState;
import org.jobrunr.storage.JobNotFoundException;
import org.jobrunr.storage.StorageProvider;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JobRunrService {

  private final StorageProvider storageProvider;

  OffsetDateTime getJobStartTime(UUID jobId) {
    OffsetDateTime scheduledTime = null;
    try {
      Job job = storageProvider.getJobById(jobId);
      JobState jobState = job.getJobState();
      if (jobState instanceof ScheduledState scheduledState) {
        scheduledTime = scheduledState.getScheduledAt().atOffset(ZoneOffset.UTC);
      } else if (jobState instanceof EnqueuedState enqueuedState) {
        scheduledTime = enqueuedState.getEnqueuedAt().atOffset(ZoneOffset.UTC);
      }
    } catch (JobNotFoundException e) {
      // NOP
    }

    return scheduledTime;
  }
}
