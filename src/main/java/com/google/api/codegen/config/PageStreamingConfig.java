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

import com.google.api.codegen.MethodConfigProto;
import com.google.api.codegen.PageStreamingConfigProto;
import com.google.api.codegen.ResourceNameTreatment;
import com.google.api.tools.framework.model.Diag;
import com.google.api.tools.framework.model.DiagCollector;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.SimpleLocation;
import com.google.auto.value.AutoValue;
import com.google.common.base.Strings;
import javax.annotation.Nullable;

/** PageStreamingConfig represents the page streaming configuration for a method. */
@AutoValue
public abstract class PageStreamingConfig {
  public abstract Field getRequestTokenField();

  @Nullable
  public abstract Field getPageSizeField();

  public abstract Field getResponseTokenField();

  public abstract FieldConfig getResourcesFieldConfig();

  /**
   * Creates an instance of PageStreamingConfig based on PageStreamingConfigProto, linking it up
   * with the provided method. On errors, null will be returned, and diagnostics are reported to the
   * diag collector.
   */
  @Nullable
  public static PageStreamingConfig createPageStreaming(
      DiagCollector diagCollector,
      ResourceNameMessageConfigs messageConfigs,
      MethodConfigProto methodConfigProto,
      Method method) {
    PageStreamingConfigProto pageStreaming = methodConfigProto.getPageStreaming();
    String requestTokenFieldName = pageStreaming.getRequest().getTokenField();
    Field requestTokenField =
        method.getInputType().getMessageType().lookupField(requestTokenFieldName);
    if (requestTokenField == null) {
      diagCollector.addDiag(
          Diag.error(
              SimpleLocation.TOPLEVEL,
              "Request field missing for page streaming: method = %s, message type = %s, field = %s",
              method.getFullName(),
              method.getInputType().getMessageType().getFullName(),
              requestTokenFieldName));
    }

    String pageSizeFieldName = pageStreaming.getRequest().getPageSizeField();
    Field pageSizeField = null;
    if (!Strings.isNullOrEmpty(pageSizeFieldName)) {
      pageSizeField = method.getInputType().getMessageType().lookupField(pageSizeFieldName);
      if (pageSizeField == null) {
        diagCollector.addDiag(
            Diag.error(
                SimpleLocation.TOPLEVEL,
                "Request field missing for page streaming: method = %s, message type = %s, field = %s",
                method.getFullName(),
                method.getInputType().getMessageType().getFullName(),
                pageSizeFieldName));
      }
    }

    String responseTokenFieldName = pageStreaming.getResponse().getTokenField();
    Field responseTokenField =
        method.getOutputType().getMessageType().lookupField(responseTokenFieldName);
    if (responseTokenField == null) {
      diagCollector.addDiag(
          Diag.error(
              SimpleLocation.TOPLEVEL,
              "Response field missing for page streaming: method = %s, message type = %s, field = %s",
              method.getFullName(),
              method.getOutputType().getMessageType().getFullName(),
              responseTokenFieldName));
    }

    String resourcesFieldName = pageStreaming.getResponse().getResourcesField();
    Field resourcesField = method.getOutputMessage().lookupField(resourcesFieldName);
    FieldConfig resourcesFieldConfig;

    if (resourcesField == null) {
      diagCollector.addDiag(
          Diag.error(
              SimpleLocation.TOPLEVEL,
              "Resources field missing for page streaming: method = %s, message type = %s, field = %s",
              method.getFullName(),
              method.getOutputType().getMessageType().getFullName(),
              resourcesFieldName));
      resourcesFieldConfig = null;
    } else {
      if (methodConfigProto.getResourceNameTreatment() == ResourceNameTreatment.STATIC_TYPES
          && messageConfigs != null
          && messageConfigs.fieldHasResourceName(resourcesField)) {
        resourcesFieldConfig =
            FieldConfig.createFieldConfig(
                resourcesField,
                ResourceNameTreatment.STATIC_TYPES,
                messageConfigs.getFieldResourceName(resourcesField));
      } else {
        resourcesFieldConfig = FieldConfig.createDefaultFieldConfig(resourcesField);
      }
    }

    if (requestTokenField == null || responseTokenField == null || resourcesFieldConfig == null) {
      return null;
    }
    return new AutoValue_PageStreamingConfig(
        requestTokenField, pageSizeField, responseTokenField, resourcesFieldConfig);
  }

  /** Returns whether there is a field for page size. */
  public boolean hasPageSizeField() {
    return getPageSizeField() != null;
  }

  public Field getResourcesField() {
    return getResourcesFieldConfig().getField();
  }

  public String getResourcesFieldName() {
    return getResourcesField().getSimpleName();
  }
}
