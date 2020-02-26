/*
 * Copyright 2017 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.spinnaker.orca;

import com.netflix.spinnaker.orca.pipeline.model.StageExecution;
import java.time.Duration;

/**
 * A retryable task defines its backoff period (the period between delays) and its timeout (the
 * total period of the task)
 */
public interface RetryableTask extends Task {
  long getBackoffPeriod();

  long getTimeout();

  default long getDynamicTimeout(StageExecution stage) {
    return getTimeout();
  }

  default long getDynamicBackoffPeriod(Duration taskDuration) {
    return getBackoffPeriod();
  }

  default long getDynamicBackoffPeriod(StageExecution stage, Duration taskDuration) {
    return getDynamicBackoffPeriod(taskDuration);
  }
}
