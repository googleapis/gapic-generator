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
import com.google.api.codegen.discovery.Document;
import com.google.api.codegen.discovery.Schema;
import com.google.api.tools.framework.model.Diag;
import com.google.api.tools.framework.model.DiagCollector;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.SimpleLocation;
import com.google.auto.value.AutoValue;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import javax.annotation.Nullable;

/** PageStreamingConfig represents the page streaming configuration for a method. */
@AutoValue
public abstract class PageStreamingConfig {
  public abstract FieldType getRequestTokenField();

  @Nullable
  public abstract FieldType getPageSizeField();

  public abstract FieldType getResponseTokenField();

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
      ImmutableMap<String, ResourceNameConfig> resourceNameConfigs,
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
      resourcesFieldConfig =
          FieldConfig.createMessageFieldConfig(
              messageConfigs,
              resourceNameConfigs,
              resourcesField,
              methodConfigProto.getResourceNameTreatment());
    }

    if (requestTokenField == null || responseTokenField == null || resourcesFieldConfig == null) {
      return null;
    }
    return new AutoValue_PageStreamingConfig(
        new FieldType(requestTokenField),
        pageSizeField == null ? null : new FieldType(pageSizeField),
        new FieldType(responseTokenField),
        resourcesFieldConfig);
  }

  /**
   * Creates an instance of PageStreamingConfig based on Discovery Doc, linking it up with the
   * provided method. On errors, null will be returned, and diagnostics are reported to the diag
   * collector.
   *
   * @param method Method descriptor for the method to create config for.
   */
  @Nullable
  public static PageStreamingConfig createPageStreaming(
      DiagCollector diagCollector, com.google.api.codegen.discovery.Method method) {
    // TODO(andrealin): Put this in yaml file somewhere instead of hardcoding.
    String pageSizeFieldName = "maxResults";
    String requestTokenFieldName = "pageToken";
    String responseTokenFieldName = "nextPageToken";

    Schema requestTokenField = method.parameters().get(requestTokenFieldName);
    if (requestTokenField == null) {
      diagCollector.addDiag(
          Diag.error(
              SimpleLocation.TOPLEVEL,
              "Request field missing for page streaming: method = %s, message type = %s, field = %s",
              method.id(),
              method.id(),
              requestTokenFieldName));
    }

    Schema pageSizeField = method.parameters().get(pageSizeFieldName);
    if (!Strings.isNullOrEmpty(pageSizeFieldName)) {
      pageSizeField = method.parameters().get(pageSizeFieldName);
      if (pageSizeField == null) {
        diagCollector.addDiag(
            Diag.error(
                SimpleLocation.TOPLEVEL,
                "Request field missing for page streaming: method = %s, message type = %s, field = %s",
                method.id(),
                method.id(),
                pageSizeFieldName));
      }
    }

    Schema responseTokenField = null;
    if (method.response().hasProperty(responseTokenFieldName)) {
      responseTokenField = method.response().properties().get(responseTokenFieldName);
    } else if (!method.response().reference().isEmpty()) {
      Document document = method.getRootDocument();
      responseTokenField = document.schemas().get(method.response().reference());
    }

    if (responseTokenField == null) {
      diagCollector.addDiag(
          Diag.error(
              SimpleLocation.TOPLEVEL,
              "Response field missing for page streaming: method = %s, message type = %s, field = %s",
              method.id(),
              method.id(),
              responseTokenFieldName));
    }

    Schema resourcesField = method.response();
    FieldConfig resourcesFieldConfig;
    if (resourcesField == null) {
      diagCollector.addDiag(
          Diag.error(
              SimpleLocation.TOPLEVEL,
              "Resources field missing for page streaming: method = %s, message type = %s, field = %s",
              method.id(),
              method.id(),
              resourcesField.getIdentifier()));
      resourcesFieldConfig = null;
    } else {
      resourcesFieldConfig = FieldConfig.createFieldConfig(diagCollector, resourcesField);
    }

    if (requestTokenField == null || responseTokenField == null || resourcesFieldConfig == null) {
      return null;
    }
    return new AutoValue_PageStreamingConfig(
        new FieldType(requestTokenField),
        new FieldType(pageSizeField),
        new FieldType(responseTokenField),
        resourcesFieldConfig);
  }

  /** Returns whether there is a field for page size. */
  public boolean hasPageSizeField() {
    return getPageSizeField() != null && !getPageSizeField().isEmpty();
  }

  public FieldType getResourcesField() {
    return getResourcesFieldConfig().getField();
  }

  public String getResourcesFieldName() {
    return getResourcesField().getSimpleName();
  }
}
