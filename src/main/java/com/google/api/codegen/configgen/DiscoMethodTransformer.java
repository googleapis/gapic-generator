/* Copyright 2017 Google LLC
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
package com.google.api.codegen.configgen;

import com.google.api.codegen.ResourceNameTreatment;
import com.google.api.codegen.config.MethodModel;

/** MethodTransformer implementation for discovery Methods. */
public class DiscoMethodTransformer implements MethodTransformer {
  private static final PagingParameters PAGING_PARAMETERS = new HttpPagingParameters();

  @Override
  public boolean isIgnoredParameter(String parameter) {
    return PAGING_PARAMETERS.getIgnoredParameters().contains(parameter);
  }

  @Override
  public String getTimeoutMillis(MethodModel method) {
    return "60000";
  }

  @Override
  public String getResourceNameTreatment() {
    return ResourceNameTreatment.STATIC_TYPES.toString();
  }
}
