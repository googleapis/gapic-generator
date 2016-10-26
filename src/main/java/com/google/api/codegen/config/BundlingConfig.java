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
import com.google.api.tools.framework.model.Diag;
import com.google.api.tools.framework.model.DiagCollector;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.FieldSelector;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.SimpleLocation;
import com.google.common.collect.ImmutableList;
import javax.annotation.Nullable;

/** BundlingConfig represents the bundling configuration for a method. */
public class BundlingConfig {
  private final int elementCountThreshold;
  private final long requestByteThreshold;
  private final int elementCountLimit;
  private final long requestByteLimit;
  private final long delayThresholdMillis;
  private final Field bundledField;
  private final ImmutableList<FieldSelector> discriminatorFields;
  private final Field subresponseField;

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

    if (bundledFieldName == null) {
      return null;
    }

    return new BundlingConfig(
        elementCountThreshold,
        requestByteThreshold,
        elementCountLimit,
        requestByteLimit,
        delayThresholdMillis,
        bundledField,
        discriminatorsBuilder.build(),
        subresponseField);
  }

  private BundlingConfig(
      int elementCountThreshold,
      long requestByteThreshold,
      int elementCountLimit,
      long requestByteLimit,
      long delayThresholdMillis,
      Field bundledField,
      ImmutableList<FieldSelector> discriminatorFields,
      Field subresponseField) {
    this.elementCountThreshold = elementCountThreshold;
    this.requestByteThreshold = requestByteThreshold;
    this.elementCountLimit = elementCountLimit;
    this.requestByteLimit = requestByteLimit;
    this.delayThresholdMillis = delayThresholdMillis;
    this.bundledField = bundledField;
    this.discriminatorFields = discriminatorFields;
    this.subresponseField = subresponseField;
  }

  public int getElementCountThreshold() {
    return elementCountThreshold;
  }

  public long getRequestByteThreshold() {
    return requestByteThreshold;
  }

  public int getElementCountLimit() {
    return elementCountLimit;
  }

  public long getRequestByteLimit() {
    return requestByteLimit;
  }

  public long getDelayThresholdMillis() {
    return delayThresholdMillis;
  }

  public Field getBundledField() {
    return bundledField;
  }

  public ImmutableList<FieldSelector> getDiscriminatorFields() {
    return discriminatorFields;
  }

  public boolean hasSubresponseField() {
    return subresponseField != null;
  }

  @Nullable
  public Field getSubresponseField() {
    return subresponseField;
  }
}
