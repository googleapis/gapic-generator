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
package com.google.api.codegen.transformer;

import com.google.api.codegen.config.FieldConfig;
import com.google.api.codegen.config.MethodContext;
import com.google.api.codegen.config.ResourceNameOneofConfig;
import com.google.api.codegen.config.ResourceNameType;

public class DefaultFeatureConfig implements FeatureConfig {

  @Override
  public boolean resourceNameTypesEnabled() {
    return false;
  }

  @Override
  public boolean resourceNameProtoAccessorsEnabled() {
    return false;
  }

  @Override
  public boolean useResourceNameFormatOption(FieldConfig fieldConfig) {
    return resourceNameTypesEnabled() && fieldConfig != null && fieldConfig.useResourceNameType();
  }

  @Override
  public boolean useResourceNameFormatOptionInSample(
      MethodContext context, FieldConfig fieldConfig) {
    boolean hasResourceNameFormatOption =
        resourceNameTypesEnabled()
            && fieldConfig != null
            && (fieldConfig.useResourceNameType() || fieldConfig.useResourceNameTypeInSampleOnly());

    if (!hasResourceNameFormatOption) {
      return false;
    }

    // For an any resource name, we choose a random single resource name defined in the API for
    // sample generation. If there are no single resource names at all in the API, we set this
    // value to false and use a string literal to instantiate a resource name string.
    boolean apiHasSingleResources =
        context.getProductConfig().getSingleResourceNameConfigs().iterator().hasNext();
    if (fieldConfig.getResourceNameType() == ResourceNameType.ANY && !apiHasSingleResources) {
      return false;
    }

    // TODO: support creating resource name strings in tests and samples using creation methods
    // in the new multi-pattern resource classes.
    //
    // This check has to be here for the following purposes:
    // - make generating new APIs with multi-pattern resource but no gapic config v2 for Java work
    //   (including java_library_no_gapic_config baseline test).
    //   We can remove the check for this case after we implement generating samples and tests
    //   using the new multi-pattern resource class for Java
    // - make generating new APIs with multi-pattern resource but no gapic config v2 for C# work
    //   Note such a case does not exist in production as all multi-pattern resource support
    //   for C# will only be implemented in the micro-generator, but for the time being bazel
    //   and artman tests still use gapic-generator to generate C# GAPICs.
    //
    // Note This check is not needed for generating Java gapics with gapic config v2, because for
    // those
    // we can put multi-pattern resource name in deprecated_collections in gapic config v2 so that
    // the generator can create resource name strings in the old wayg.
    boolean requiresMultiPatternResourceSupport =
        fieldConfig.getResourceNameType() == ResourceNameType.ONEOF
            && ((ResourceNameOneofConfig) fieldConfig.getResourceNameConfig())
                .getSingleResourceNameConfigs()
                .isEmpty();

    return !requiresMultiPatternResourceSupport;
  }

  @Override
  public boolean useResourceNameFormatOptionInSampleOnly(FieldConfig fieldConfig) {
    return resourceNameTypesEnabled()
        && fieldConfig != null
        && fieldConfig.useResourceNameTypeInSampleOnly();
  }

  @Override
  public boolean useResourceNameProtoAccessor(FieldConfig fieldConfig) {
    return resourceNameProtoAccessorsEnabled() && useResourceNameFormatOption(fieldConfig);
  }

  @Override
  public boolean useResourceNameConverters(FieldConfig fieldConfig) {
    return !resourceNameProtoAccessorsEnabled() && useResourceNameFormatOption(fieldConfig);
  }

  @Override
  public boolean useResourceNameConvertersInSampleOnly(
      MethodContext context, FieldConfig fieldConfig) {
    return !resourceNameProtoAccessorsEnabled()
        && useResourceNameFormatOptionInSampleOnly(fieldConfig);
  }

  @Override
  public boolean useInheritanceForOneofs() {
    return false;
  }

  @Override
  public boolean enableMixins() {
    return false;
  }

  @Override
  public boolean enableGrpcStreaming() {
    return true;
  }

  @Override
  public boolean enableStringFormatFunctions() {
    return true;
  }

  @Override
  public boolean enableRawOperationCallSettings() {
    return false;
  }
}
