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

import com.google.api.codegen.common.TargetLanguage;
import com.google.api.codegen.config.FieldConfig;
import com.google.api.codegen.transformer.csharp.CSharpFeatureConfig;
import com.google.api.codegen.transformer.java.JavaFeatureConfig;
import com.google.api.codegen.transformer.nodejs.NodeJSFeatureConfig;
import com.google.api.codegen.transformer.php.PhpFeatureConfig;
import com.google.api.codegen.transformer.ruby.RubyFeatureConfig;

public class DefaultFeatureConfig implements FeatureConfig {

  public static FeatureConfig getDefaultLanguageFeatureConfig(TargetLanguage targetLanguage) {
    switch (targetLanguage) {
      case JAVA:
        return JavaFeatureConfig.newBuilder().enableStringFormatFunctions(false).build();
      case CSHARP:
        return new CSharpFeatureConfig();
      case NODEJS:
        return new NodeJSFeatureConfig();
      case PHP:
        return new PhpFeatureConfig();
      case RUBY:
        return new RubyFeatureConfig();
      default:
        return new DefaultFeatureConfig();
    }
  }

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
    return resourceNameTypesEnabled()
        && fieldConfig != null
        && (fieldConfig.useResourceNameType() || fieldConfig.useResourceNameTypeInSampleOnly());
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

  @Override
  public boolean enableProtoAnnotations() {
    return true;
  };
}
