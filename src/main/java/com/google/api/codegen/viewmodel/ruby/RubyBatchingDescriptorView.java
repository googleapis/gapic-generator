/* Copyright 2018 Google LLC
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
package com.google.api.codegen.viewmodel.ruby;

import com.google.api.codegen.config.BatchingConfig;
import com.google.api.codegen.config.GenericFieldSelector;
import com.google.api.codegen.config.MethodConfig;
import com.google.common.collect.ImmutableList;

public class RubyBatchingDescriptorView {

  private final MethodConfig methodConfig;

  public RubyBatchingDescriptorView(MethodConfig methodConfig) {
    this.methodConfig = methodConfig;
  }

  public String methodName() {
    return methodConfig.getMethodModel().asName().toLowerUnderscore();
  }

  public String batchedFieldName() {
    return getBatching().getBatchedField().getNameAsParameterName().toLowerUnderscore();
  }

  public Iterable<String> discriminatorFieldNames() {
    ImmutableList.Builder<String> fieldNames = ImmutableList.builder();
    for (GenericFieldSelector fieldSelector : getBatching().getDiscriminatorFields()) {
      fieldNames.add(fieldSelector.getParamName());
    }
    return fieldNames.build();
  }

  public String subresponseFieldName() {
    return getBatching().hasSubresponseField()
        ? getBatching().getSubresponseField().getNameAsParameterName().toLowerUnderscore()
        : "";
  }

  private BatchingConfig getBatching() {
    return methodConfig.getBatching();
  }
}
