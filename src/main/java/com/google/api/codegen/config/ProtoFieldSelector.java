/* Copyright 2017 Google LLC
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

import com.google.api.tools.framework.model.FieldSelector;
import com.google.common.base.Preconditions;

/** Proto-based wrapper around FieldSelector. */
public final class ProtoFieldSelector implements GenericFieldSelector {
  private final FieldSelector fieldSelector;

  public ProtoFieldSelector(FieldSelector fieldSelector) {
    Preconditions.checkNotNull(fieldSelector);
    this.fieldSelector = fieldSelector;
  }

  @Override
  /* @return the type of source that this FieldModel is based on. */
  public ApiSource getApiSource() {
    return ApiSource.PROTO;
  }

  @Override
  public String getParamName() {
    return fieldSelector.getParamName();
  }

  @Override
  public FieldModel getLastField() {
    return new ProtoField(fieldSelector.getLastField());
  }
}
