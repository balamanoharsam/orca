/*
 * Copyright 2017 Schibsted ASA.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
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

package com.netflix.spinnaker.orca.webhook.pipeline

import com.fasterxml.jackson.annotation.JsonFormat
import com.netflix.spinnaker.kork.exceptions.UserException
import com.netflix.spinnaker.orca.CancellableStage
import com.netflix.spinnaker.orca.pipeline.StageDefinitionBuilder
import com.netflix.spinnaker.orca.pipeline.TaskNode
import com.netflix.spinnaker.orca.pipeline.graph.StageGraphBuilder
import com.netflix.spinnaker.orca.pipeline.model.StageExecution
import com.netflix.spinnaker.orca.pipeline.tasks.WaitTask
import com.netflix.spinnaker.orca.webhook.config.WebhookProperties
import com.netflix.spinnaker.orca.webhook.service.WebhookService
import com.netflix.spinnaker.orca.webhook.tasks.CreateWebhookTask
import com.netflix.spinnaker.orca.webhook.tasks.MonitorWebhookTask
import com.netflix.spinnaker.orca.pipeline.tasks.artifacts.BindProducedArtifactsTask
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Component

import javax.annotation.Nonnull

@Slf4j
@Component
class WebhookStage implements StageDefinitionBuilder {

  @Autowired
  WebhookService webhookService

  @Autowired
  WebhookStage(WebhookService webhookService) {
    this.webhookService = webhookService
  }

  @Override
  void taskGraph(StageExecution stage, TaskNode.Builder builder) {
    StageData stageData = stage.mapTo(StageData)

    if (stageData.monitorOnly && !stageData.waitForCompletion) {
      throw new UserException("Can't specify monitorOnly = true and waitForCompletion = false at the same time")
    }

    if (!stageData.monitorOnly) {
      builder.withTask("createWebhook", CreateWebhookTask)
    }
    if (stageData.waitForCompletion) {
      if (stageData.waitBeforeMonitor > 0) {
        stage.context.putIfAbsent("waitTime", stageData.waitBeforeMonitor)
        builder.withTask("waitBeforeMonitorWebhook", WaitTask)
      }

      builder.withTask("monitorWebhook", MonitorWebhookTask)
    }
    if (stage.context.containsKey("expectedArtifacts")) {
      builder.withTask(BindProducedArtifactsTask.TASK_NAME, BindProducedArtifactsTask.class)
    }
  }

  @Override
  void onFailureStages(@Nonnull StageExecution stage, @Nonnull StageGraphBuilder graph) {
    new MonitorWebhookTask(webhookService).onCancel(stage)
  }

  static class StageData {
    // Inputs for webhook
    public String url
    public Object payload
    public Object customHeaders
    public List<Integer> failFastStatusCodes
    public Boolean waitForCompletion
    public WebhookProperties.StatusUrlResolution statusUrlResolution
    public String statusUrlJsonPath
    public Boolean monitorOnly

    @JsonFormat(with = [JsonFormat.Feature.ACCEPT_CASE_INSENSITIVE_PROPERTIES])
    public HttpMethod method = HttpMethod.POST

    // Inputs for monitor
    public String statusEndpoint
    public String statusJsonPath
    public String progressJsonPath
    public String cancelEndpoint
    @JsonFormat(with = [JsonFormat.Feature.ACCEPT_CASE_INSENSITIVE_PROPERTIES])
    public HttpMethod cancelMethod = HttpMethod.POST
    public Object cancelPayload
    public String successStatuses
    public String canceledStatuses
    public String terminalStatuses
    public List<Integer> retryStatusCodes

    public int waitBeforeMonitor

    // Outputs
    WebhookResponseStageData webhook
  }

  static class WebhookResponseStageData {
    String statusCode
    WebhookMonitorResponseStageData monitor
    String error

    // NOTE: The fields below exist in the context because they are inserted by the CreateWebhookTask but they aren't
    //       consumed by Spinnaker so they are commented out below - they are here for informational purposes only
    //String statusEndpoint
    //Integer statusCodeValue
    //Map body
  }

  static class WebhookMonitorResponseStageData {
    String error

    // NOTE: The fields below exist in the context because they are inserted by the MonitorWebhookTask but they aren't
    //       consumed by Spinnaker so they are commented out below - they are here for informational purposes only
    //Integer statusCodeValue
    //String statusCode
    //Map body
    //String progressMessage
    //Number percentComplete
  }
}
