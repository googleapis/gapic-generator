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
package com.google.api.codegen.discogapic;

import com.google.api.codegen.config.ApiSource;
import com.google.api.codegen.config.FieldModel;
import com.google.api.codegen.config.OneofConfig;
import com.google.api.codegen.config.TypeModel;
import com.google.common.collect.ImmutableList;
import java.util.List;

public class EmptyTypeModel implements TypeModel {

  @Override
  public ApiSource getApiSource() {
    return ApiSource.EMPTY;
  }

  private final List<FieldModel> fields = ImmutableList.of();

  @Override
  public boolean isMap() {
    return false;
  }

  @Override
  public FieldModel getMapKeyField() {
    return null;
  }

  @Override
  public FieldModel getMapValueField() {
    return null;
  }

  @Override
  public boolean isMessage() {
    return true;
  }

  @Override
  public boolean isRepeated() {
    return false;
  }

  @Override
  public boolean isEnum() {
    return false;
  }

  @Override
  public boolean isPrimitive() {
    return false;
  }

  @Override
  public boolean isEmptyType() {
    return true;
  }

  @Override
  public void validateValue(String value) {}

  @Override
  public List<? extends FieldModel> getFields() {
    return fields;
  }

  @Override
  public FieldModel getField(String targetName) {
    return null;
  }

  @Override
  public TypeModel makeOptional() {
    return this;
  }

  @Override
  public String getPrimitiveTypeName() {
    return "";
  }

  @Override
  public boolean isBooleanType() {
    return false;
  }

  @Override
  public boolean isStringType() {
    return false;
  }

  @Override
  public boolean isFloatType() {
    return false;
  }

  @Override
  public boolean isBytesType() {
    return false;
  }

  @Override
  public boolean isDoubleType() {
    return false;
  }

  @Override
  public String getTypeName() {
    return "EmptyTypeModel";
  }

  @Override
  public OneofConfig getOneOfConfig(String fieldName) {
    return null;
  }
}
