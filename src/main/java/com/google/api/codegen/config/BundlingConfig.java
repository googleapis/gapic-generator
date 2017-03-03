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

import com.google.api.codegen.BundlingConfigProto;
import com.google.api.codegen.BundlingDescriptorProto;
import com.google.api.codegen.BundlingSettingsProto;
import com.google.api.codegen.FlowControlLimitExceededBehavior;
import com.google.api.tools.framework.model.Diag;
import com.google.api.tools.framework.model.DiagCollector;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.FieldSelector;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.SimpleLocation;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import javax.annotation.Nullable;

/** BundlingConfig represents the bundling configuration for a method. */
@AutoValue
public abstract class BundlingConfig {

  /**
   * Creates an instance of BundlingConfig based on BundlingConfigProto, linking it up with the
   * provided method. On errors, null will be returned, and diagnostics are reported to the diag
   * collector.
   */
  @Nullable
  public static BundlingConfig createBundling(
      DiagCollector diagCollector, BundlingConfigProto bundlingConfig, Method method) {

    BundlingDescriptorProto bundleDescriptor = bundlingConfig.getBundleDescriptor();
    String bundledFieldName = bundleDescriptor.getBundledField();
    Field bundledField = method.getInputType().getMessageType().lookupField(bundledFieldName);
    if (bundledField == null) {
      diagCollector.addDiag(
          Diag.error(
              SimpleLocation.TOPLEVEL,
              "Bundled field missing for bundle config: method = %s, message type = %s, field = %s",
              method.getFullName(),
              method.getInputType().getMessageType().getFullName(),
              bundledFieldName));
    }

    ImmutableList.Builder<FieldSelector> discriminatorsBuilder = ImmutableList.builder();
    for (String discriminatorName : bundleDescriptor.getDiscriminatorFieldsList()) {
      FieldSelector selector =
          FieldSelector.resolve(method.getInputType().getMessageType(), discriminatorName);
      if (selector == null) {
        diagCollector.addDiag(
            Diag.error(
                SimpleLocation.TOPLEVEL,
                "Discriminator field missing for bundle config: method = %s, message type = %s, "
                    + "field = %s",
                method.getFullName(),
                method.getInputType().getMessageType().getFullName(),
                discriminatorName));
      }
      discriminatorsBuilder.add(selector);
    }

    String subresponseFieldName = bundleDescriptor.getSubresponseField();
    Field subresponseField;
    if (!subresponseFieldName.isEmpty()) {
      subresponseField = method.getOutputType().getMessageType().lookupField(subresponseFieldName);
    } else {
      subresponseField = null;
    }

    BundlingSettingsProto bundlingSettings = bundlingConfig.getThresholds();
    int elementCountThreshold = bundlingSettings.getElementCountThreshold();
    long requestByteThreshold = bundlingSettings.getRequestByteThreshold();
    int elementCountLimit = bundlingSettings.getElementCountLimit();
    long requestByteLimit = bundlingSettings.getRequestByteLimit();
    long delayThresholdMillis = bundlingConfig.getThresholds().getDelayThresholdMillis();
    Long flowControlElementLimit =
        (long) bundlingConfig.getThresholds().getFlowControlElementLimit();
    if (flowControlElementLimit == 0) {
      flowControlElementLimit = null;
    }
    Long flowControlByteLimit = (long) bundlingConfig.getThresholds().getFlowControlByteLimit();
    if (flowControlByteLimit == 0) {
      flowControlByteLimit = null;
    }
    FlowControlLimitExceededBehavior flowControlLimitExceededBehavior =
        bundlingConfig.getThresholds().getFlowControlLimitExceededBehavior();

    if (bundledFieldName == null) {
      return null;
    }

    return new AutoValue_BundlingConfig(
        elementCountThreshold,
        requestByteThreshold,
        elementCountLimit,
        requestByteLimit,
        delayThresholdMillis,
        bundledField,
        discriminatorsBuilder.build(),
        subresponseField,
        flowControlElementLimit,
        flowControlByteLimit,
        flowControlLimitExceededBehavior);
  }

  public abstract int getElementCountThreshold();

  public abstract long getRequestByteThreshold();

  public abstract int getElementCountLimit();

  public abstract long getRequestByteLimit();

  public abstract long getDelayThresholdMillis();

  public abstract Field getBundledField();

  public abstract ImmutableList<FieldSelector> getDiscriminatorFields();

  @Nullable
  public abstract Field getSubresponseField();

  @Nullable
  public abstract Long getFlowControlElementLimit();

  @Nullable
  public abstract Long getFlowControlByteLimit();

  public abstract FlowControlLimitExceededBehavior getFlowControlLimitExceededBehavior();

  public boolean hasSubresponseField() {
    return getSubresponseField() != null;
  }
}
