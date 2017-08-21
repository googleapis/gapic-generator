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
import com.google.api.codegen.discogapic.transformer.DiscoGapicNamer;
import com.google.api.codegen.discovery.Schema;
import com.google.api.codegen.discovery.Schema.Type;
import com.google.api.tools.framework.model.Diag;
import com.google.api.tools.framework.model.DiagCollector;
import com.google.api.tools.framework.model.SimpleLocation;
import com.google.auto.value.AutoValue;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import javax.annotation.Nullable;

/** PageStreamingConfig represents the page streaming configuration for a method. */
@AutoValue
public abstract class PageStreamingConfig {
  public abstract FieldModel getRequestTokenField();

  @Nullable
  public abstract FieldModel getPageSizeField();

  public abstract FieldModel getResponseTokenField();

  public abstract FieldConfig getResourcesFieldConfig();

  /**
   * Creates an instance of PageStreamingConfig based on PageStreamingConfigProto, linking it up
   * with the provided method. On errors, null will be returned, and diagnostics are reported to the
   * diag collector.
   */
  @Nullable
  static PageStreamingConfig createPageStreaming(
      DiagCollector diagCollector,
      ResourceNameMessageConfigs messageConfigs,
      ImmutableMap<String, ResourceNameConfig> resourceNameConfigs,
      MethodConfigProto methodConfigProto,
      MethodModel method) {
    PageStreamingConfigProto pageStreaming = methodConfigProto.getPageStreaming();
    String requestTokenFieldName = pageStreaming.getRequest().getTokenField();
    FieldModel requestTokenField = method.getInputField(requestTokenFieldName);
    if (requestTokenField == null) {
      diagCollector.addDiag(
          Diag.error(
              SimpleLocation.TOPLEVEL,
              "Request field missing for page streaming: method = %s, message type = %s, field = %s",
              method.getFullName(),
              method.getInputFullName(),
              requestTokenFieldName));
    }

    String pageSizeFieldName = pageStreaming.getRequest().getPageSizeField();
    FieldModel pageSizeField = null;
    if (!Strings.isNullOrEmpty(pageSizeFieldName)) {
      pageSizeField = method.getInputField(pageSizeFieldName);
      if (pageSizeField == null) {
        diagCollector.addDiag(
            Diag.error(
                SimpleLocation.TOPLEVEL,
                "Request field missing for page streaming: method = %s, message type = %s, field = %s",
                method.getFullName(),
                method.getInputFullName(),
                pageSizeFieldName));
      }
    }

    String responseTokenFieldName = pageStreaming.getResponse().getTokenField();
    FieldModel responseTokenField = method.getOutputField(responseTokenFieldName);
    if (responseTokenField == null) {
      diagCollector.addDiag(
          Diag.error(
              SimpleLocation.TOPLEVEL,
              "Response field missing for page streaming: method = %s, message type = %s, field = %s",
              method.getFullName(),
              method.getOutputFullName(),
              responseTokenFieldName));
    }

    String resourcesFieldName = pageStreaming.getResponse().getResourcesField();
    FieldModel resourcesField = method.getOutputField(resourcesFieldName);
    FieldConfig resourcesFieldConfig;

    if (resourcesField == null) {
      diagCollector.addDiag(
          Diag.error(
              SimpleLocation.TOPLEVEL,
              "Resources field missing for page streaming: method = %s, message type = %s, field = %s",
              method.getFullName(),
              method.getOutputFullName(),
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
        requestTokenField, pageSizeField, responseTokenField, resourcesFieldConfig);
  }

  /**
   * Creates an instance of PageStreamingConfig based on Discovery Doc, linking it up with the
   * provided method. On errors, null will be returned, and diagnostics are reported to the diag
   * collector.
   *
   * @param method Method descriptor for the method to create config for.
   */
  @Nullable
  // TODO(andrealin): Merge this function into the createPageStreaming(... Method protoMethod) function.
  static PageStreamingConfig createPageStreaming(
      DiagCollector diagCollector,
      com.google.api.codegen.discovery.Method method,
      DiscoGapicNamer discoGapicNamer) {
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
    Schema responseSchema = method.response().dereference();
    if (responseSchema.hasProperty(responseTokenFieldName)) {
      responseTokenField = responseSchema.properties().get(responseTokenFieldName);
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

    Schema responseField = method.response().dereference();
    Schema resourcesField = null;
    for (Schema property : responseField.properties().values()) {
      // Assume the List response has exactly one Array property.
      if (property.type().equals(Type.ARRAY)) {
        resourcesField = property;
        break;
      }
    }
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
      resourcesFieldConfig =
          FieldConfig.createFieldConfig(new DiscoveryField(resourcesField, discoGapicNamer));
    }

    if (requestTokenField == null || responseTokenField == null || resourcesFieldConfig == null) {
      return null;
    }
    return new AutoValue_PageStreamingConfig(
        new DiscoveryField(requestTokenField, discoGapicNamer),
        new DiscoveryField(pageSizeField, discoGapicNamer),
        new DiscoveryField(responseTokenField, discoGapicNamer),
        resourcesFieldConfig);
  }

  /** Returns whether there is a field for page size. */
  public boolean hasPageSizeField() {
    return getPageSizeField() != null;
  }

  public FieldModel getResourcesField() {
    return getResourcesFieldConfig().getField();
  }

  public String getResourcesFieldName() {
    return getResourcesField().getSimpleName();
  }
}
