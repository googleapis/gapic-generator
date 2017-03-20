/* Copyright 2016 Google Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.api.codegen.config;

import com.google.api.codegen.BatchingConfigProto;
import com.google.api.codegen.BatchingDescriptorProto;
import com.google.api.codegen.BatchingSettingsProto;
import com.google.api.tools.framework.model.Diag;
import com.google.api.tools.framework.model.DiagCollector;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.FieldSelector;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.SimpleLocation;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import javax.annotation.Nullable;

/** BatchingConfig represents the batching configuration for a method. */
@AutoValue
public abstract class BatchingConfig {

  /**
   * Creates an instance of BatchingConfig based on BatchingConfigProto, linking it up with the
   * provided method. On errors, null will be returned, and diagnostics are reported to the diag
   * collector.
   */
  @Nullable
  public static BatchingConfig createBatching(
      DiagCollector diagCollector, BatchingConfigProto batchingConfig, Method method) {

    BatchingDescriptorProto batchDescriptor = batchingConfig.getBatchDescriptor();
    String batchedFieldName = batchDescriptor.getBatchedField();
    Field batchedField = method.getInputType().getMessageType().lookupField(batchedFieldName);
    if (batchedField == null) {
      diagCollector.addDiag(
          Diag.error(
              SimpleLocation.TOPLEVEL,
              "Batched field missing for batch config: method = %s, message type = %s, field = %s",
              method.getFullName(),
              method.getInputType().getMessageType().getFullName(),
              batchedFieldName));
    }

    ImmutableList.Builder<FieldSelector> discriminatorsBuilder = ImmutableList.builder();
    for (String discriminatorName : batchDescriptor.getDiscriminatorFieldsList()) {
      FieldSelector selector =
          FieldSelector.resolve(method.getInputType().getMessageType(), discriminatorName);
      if (selector == null) {
        diagCollector.addDiag(
            Diag.error(
                SimpleLocation.TOPLEVEL,
                "Discriminator field missing for batch config: method = %s, message type = %s, "
                    + "field = %s",
                method.getFullName(),
                method.getInputType().getMessageType().getFullName(),
                discriminatorName));
      }
      discriminatorsBuilder.add(selector);
    }

    String subresponseFieldName = batchDescriptor.getSubresponseField();
    Field subresponseField;
    if (!subresponseFieldName.isEmpty()) {
      subresponseField = method.getOutputType().getMessageType().lookupField(subresponseFieldName);
    } else {
      subresponseField = null;
    }

    BatchingSettingsProto batchingSettings = batchingConfig.getThresholds();
    int elementCountThreshold = batchingSettings.getElementCountThreshold();
    long requestByteThreshold = batchingSettings.getRequestByteThreshold();
    int elementCountLimit = batchingSettings.getElementCountLimit();
    long requestByteLimit = batchingSettings.getRequestByteLimit();
    long delayThresholdMillis = batchingConfig.getThresholds().getDelayThresholdMillis();
    Long flowControlElementLimit =
        (long) batchingConfig.getThresholds().getFlowControlElementLimit();
    if (flowControlElementLimit == 0) {
      flowControlElementLimit = null;
    }
    Long flowControlByteLimit = (long) batchingConfig.getThresholds().getFlowControlByteLimit();
    if (flowControlByteLimit == 0) {
      flowControlByteLimit = null;
    }
    FlowControlLimitConfig flowControlLimitConfig =
        FlowControlLimitConfig.fromProto(
            batchingConfig.getThresholds().getFlowControlLimitExceededBehavior());

    if (batchedFieldName == null) {
      return null;
    }

    return new AutoValue_BatchingConfig(
        elementCountThreshold,
        requestByteThreshold,
        elementCountLimit,
        requestByteLimit,
        delayThresholdMillis,
        batchedField,
        discriminatorsBuilder.build(),
        subresponseField,
        flowControlElementLimit,
        flowControlByteLimit,
        flowControlLimitConfig);
  }

  public abstract int getElementCountThreshold();

  public abstract long getRequestByteThreshold();

  public abstract int getElementCountLimit();

  public abstract long getRequestByteLimit();

  public abstract long getDelayThresholdMillis();

  public abstract Field getBatchedField();

  public abstract ImmutableList<FieldSelector> getDiscriminatorFields();

  @Nullable
  public abstract Field getSubresponseField();

  public boolean hasSubresponseField() {
    return getSubresponseField() != null;
  }

  @Nullable
  public abstract Long getFlowControlElementLimit();

  @Nullable
  public abstract Long getFlowControlByteLimit();

  public abstract FlowControlLimitConfig getFlowControlLimitConfig();
}
