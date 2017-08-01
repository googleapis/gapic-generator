/* Copyright 2017 Google Inc
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

import com.google.api.codegen.config.FieldType.ApiSource;
import com.google.api.tools.framework.model.FieldSelector;
import com.google.api.tools.framework.model.Method;
import com.google.common.base.Preconditions;

/** Created by andrealin on 8/1/17. */
public final class ProtoMethodModel implements MethodModel {
  private final Method method;
  private final ApiSource apiSource = ApiSource.PROTO;

  /* Create a MethodModel object from a non-null Method object. */
  public ProtoMethodModel(Method method) {
    Preconditions.checkNotNull(method);
    this.method = method;
  }

  @Override
  public FieldType.ApiSource getApiSource() {
    return apiSource;
  }

  @Override
  public FieldType lookupInputField(String fieldName) {
    return new ProtoField(method.getInputType().getMessageType().lookupField(fieldName));
  }

  @Override
  public FieldType lookupOutputField(String fieldName) {
    return new ProtoField(method.getOutputType().getMessageType().lookupField(fieldName));
  }

  @Override
  public String getFullName() {
    return method.getFullName();
  }

  @Override
  public String getInputFullName() {
    return method.getInputType().getMessageType().getFullName();
  }

  @Override
  public GenericFieldSelector getInputFieldSelector(String fieldName) {
    return new ProtoFieldSelector(
        FieldSelector.resolve(method.getInputType().getMessageType(), fieldName));
  }

  @Override
  public boolean equals(Object o) {
    return o != null
        && o instanceof ProtoMethodModel
        && ((ProtoMethodModel) o).method.equals(method);
  }
}
