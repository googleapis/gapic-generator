/* Copyright 2016 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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
import com.google.api.codegen.common.TargetLanguage;
import com.google.api.codegen.configgen.ProtoPageStreamingTransformer;
import com.google.api.codegen.configgen.ProtoPagingParameters;
import com.google.api.codegen.util.ProtoParser;
import com.google.api.tools.framework.model.Diag;
import com.google.api.tools.framework.model.DiagCollector;
import com.google.api.tools.framework.model.SimpleLocation;
import com.google.auto.value.AutoValue;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** PageStreamingConfig represents the page streaming configuration for a method. */
@AutoValue
public abstract class PageStreamingConfig {
  @AutoValue
  public abstract static class PagingFields {

    public abstract FieldModel getRequestTokenField();

    // This can be a required field once all APIs have this configured.
    @Nullable
    public abstract FieldModel getPageSizeField();

    public abstract FieldModel getResponseTokenField();

    public static PagingFields.Builder newBuilder() {
      return new AutoValue_PageStreamingConfig_PagingFields.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {

      public abstract PagingFields.Builder setRequestTokenField(FieldModel val);

      public abstract PagingFields.Builder setPageSizeField(FieldModel val);

      public abstract PagingFields.Builder setResponseTokenField(FieldModel val);

      public abstract PagingFields build();
    }
  }

  public abstract PagingFields getPagingFields();

  public abstract FieldConfig getResourcesFieldConfig();

  @Nullable
  public FieldModel getPageSizeField() {
    return getPagingFields().getPageSizeField();
  }

  public FieldModel getResponseTokenField() {
    return getPagingFields().getResponseTokenField();
  }

  public FieldModel getRequestTokenField() {
    return getPagingFields().getRequestTokenField();
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

  // TODO(andrealin): combine this with the protofile one, pass in resourcenametreatment as param
  /**
   * Creates an instance of PageStreamingConfig based on PageStreamingConfigProto, linking it up
   * with the provided method. On errors, null will be returned, and diagnostics are reported to the
   * diag collector.
   */
  @Nullable
  static PageStreamingConfig createPageStreamingFromGapicConfig(
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
          FieldConfigFactory.createMessageFieldConfig(
              messageConfigs,
              resourceNameConfigs,
              resourcesField,
              methodConfigProto.getResourceNameTreatment());
    }

    if (requestTokenField == null || responseTokenField == null || resourcesFieldConfig == null) {
      return null;
    }
    PagingFields pagingFields =
        PagingFields.newBuilder()
            .setPageSizeField(pageSizeField)
            .setRequestTokenField(requestTokenField)
            .setResponseTokenField(responseTokenField)
            .build();
    return new AutoValue_PageStreamingConfig(pagingFields, resourcesFieldConfig);
  }

  /**
   * Creates an instance of PageStreamingConfig based on PageStreamingConfigProto, linking it up
   * with the provided method. On errors, null will be returned, and diagnostics are reported to the
   * diag collector.
   */
  @Nullable
  static PageStreamingConfig createPageStreamingFromProtoFile(
      DiagCollector diagCollector,
      ResourceNameMessageConfigs messageConfigs,
      ImmutableMap<String, ResourceNameConfig> resourceNameConfigs,
      ProtoMethodModel method,
      PagingFields pagingFields,
      ProtoParser protoParser,
      String defaultPackageName) {
    FieldModel requestTokenField = pagingFields.getRequestTokenField();
    FieldModel responseTokenField = pagingFields.getResponseTokenField();

    FieldModel resourcesField = ProtoPageStreamingTransformer.getResourcesField(method);
    FieldConfig resourcesFieldConfig;

    if (resourcesField == null) {
      diagCollector.addDiag(
          Diag.error(
              SimpleLocation.TOPLEVEL,
              "Resources field missing for page streaming: method = %s, message type = %s",
              method.getFullName(),
              method.getOutputFullName()));
      resourcesFieldConfig = null;
    } else {
      ResourceNameTreatment resourceNameTreatment =
          GapicMethodConfig.defaultResourceNameTreatmentFromProto(
              method.getProtoMethod(), protoParser, defaultPackageName);
      resourcesFieldConfig =
          FieldConfigFactory.createMessageFieldConfig(
              messageConfigs, resourceNameConfigs, resourcesField, resourceNameTreatment);
    }

    if (requestTokenField == null || responseTokenField == null || resourcesFieldConfig == null) {
      return null;
    }
    return new AutoValue_PageStreamingConfig(pagingFields, resourcesFieldConfig);
  }

  /** package-private for use by {@link GapicMethodConfig}. */
  static PageStreamingConfig createPageStreamingConfig(
      TargetLanguage language,
      TransportProtocol transportProtocol,
      DiagCollector diagCollector,
      String defaultPackageName,
      ProtoMethodModel methodModel,
      MethodConfigProto methodConfigProto,
      ResourceNameMessageConfigs messageConfigs,
      ImmutableMap<String, ResourceNameConfig> resourceNameConfigs,
      ProtoParser protoParser) {
    // Let the GAPIC config define a page streaming config as an override.
    if (methodConfigProto.hasPageStreaming()) {
      return createPageStreamingFromGapicConfig(
          diagCollector, messageConfigs, resourceNameConfigs, methodConfigProto, methodModel);
    }

    // Toggle pagination based on presence of paging params.
    // See https://cloud.google.com/apis/design/design_patterns for API pagination pattern.
    ProtoPagingParameters pagingParams = new ProtoPagingParameters();
    ProtoField tokenField = methodModel.getInputField(pagingParams.getNameForPageToken());
    ProtoField pageSizeField = methodModel.getInputField(pagingParams.getNameForPageSize());
    if (pageSizeField == null) {
      // TODO: Conform to design doc spec, once approved, for using non-standard paging fields
      //       (such as max_results for page_size)
      if (language == TargetLanguage.JAVA && transportProtocol == TransportProtocol.HTTP) {
        pageSizeField = methodModel.getInputField(pagingParams.getNameForMaxResults());
      }
    }
    ProtoField responseTokenField =
        methodModel.getOutputField(pagingParams.getNameForNextPageToken());
    if (tokenField != null && responseTokenField != null && pageSizeField != null) {
      PagingFields pagingFields =
          PagingFields.newBuilder()
              .setResponseTokenField(responseTokenField)
              .setRequestTokenField(tokenField)
              .setPageSizeField(pageSizeField)
              .build();
      return PageStreamingConfig.createPageStreamingFromProtoFile(
          diagCollector,
          messageConfigs,
          resourceNameConfigs,
          methodModel,
          pagingFields,
          protoParser,
          defaultPackageName);
    }
    return null;
  }

  /** package-private for use by {@link GapicMethodConfig}. */
  static PageStreamingConfig createPageStreamingConfig(
      DiagCollector diagCollector,
      ProtoMethodModel methodModel,
      @Nonnull MethodConfigProto methodConfigProto,
      ResourceNameMessageConfigs messageConfigs,
      ImmutableMap<String, ResourceNameConfig> resourceNameConfigs) {
    if (!PageStreamingConfigProto.getDefaultInstance()
        .equals(methodConfigProto.getPageStreaming())) {
      return PageStreamingConfig.createPageStreamingFromGapicConfig(
          diagCollector, messageConfigs, resourceNameConfigs, methodConfigProto, methodModel);
    }
    return null;
  }
}
