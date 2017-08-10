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

import com.google.api.codegen.discogapic.transformer.DiscoGapicNamer;
import com.google.api.codegen.discovery.Method;
import com.google.api.codegen.discovery.Schema;
import com.google.api.codegen.transformer.ImportTypeTable;
import com.google.api.codegen.transformer.SchemaTypeFormatter;
import com.google.api.codegen.transformer.SchemaTypeNameConverter;
import com.google.api.codegen.transformer.SchemaTypeTable;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.transformer.TypeFormatter;
import com.google.api.codegen.transformer.TypeNameConverter;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.util.TypeName;
import com.google.common.base.Preconditions;

/** A wrapper around the model of a Discovery Method. */
public final class DiscoveryMethodModel implements MethodModel {
  private final Method method;

  /* Create a DiscoveryMethodModel from a non-null Discovery Method object. */
  public DiscoveryMethodModel(Method method) {
    Preconditions.checkNotNull(method);
    this.method = method;
  }

  @Override
  public String getOutputTypeSimpleName() {
    return method.response() == null ? "none" : method.response().id();
  }

  @Override
  public ApiSource getApiSource() {
    return ApiSource.DISCOVERY;
  }

  @Override
  public FieldType getInputField(String fieldName) {
    Schema targetSchema = method.parameters().get(fieldName);
    if (targetSchema == null) {
      return null;
    }
    return new DiscoveryField(targetSchema);
  }

  @Override
  public FieldType getOutputField(String fieldName) {
    return null;
  }

  @Override
  public String getFullName() {
    return method.id();
  }

  @Override
  public String getInputFullName() {
    // TODO(andrealin): this could be wrong; it might require the discogapic namer
    return method.request().getIdentifier();
  }

  @Override
  public String getDescription() {
    return method.description();
  }

  @Override
  public String getOutputTypeFullName(TypeFormatter typeFormatter) {
    // Maybe use Discogapic namer for this?
    return ((SchemaTypeFormatter) typeFormatter).getFullNameFor(method.response());
  }

  @Override
  public String getInputTypeNickname(TypeFormatter typeFormatter) {
    return null;
  }

  @Override
  public String getOutputTypeFullName(ImportTypeTable typeTable) {
    return ((SchemaTypeTable) typeTable).getFullNameFor(method.response());
  }

  @Override
  public String getOutputFullName() {
    return method.response() == null ? "none" : method.response().getIdentifier();
  }

  @Override
  public String getInputTypeFullName(ImportTypeTable typeTable) {
    return ((SchemaTypeTable) typeTable).getFullNameFor(method.request());
  }

  @Override
  public TypeName getOutputTypeName(TypeNameConverter typeFormatter) {
    return ((SchemaTypeNameConverter) typeFormatter).getTypeName(method.response());
  }

  @Override
  public GenericFieldSelector getInputFieldSelector(String fieldName) {
    // TODO(andrealin): implement.
    return null;
  }

  @Override
  public boolean getRequestStreaming() {
    return false;
  }

  @Override
  public boolean getResponseStreaming() {
    return false;
  }

  @Override
  public Name asName() {
    return DiscoGapicNamer.methodAsName(method);
  }

  @Override
  public boolean isOutputTypeEmpty() {
    return method.response() == null;
  }

  @Override
  public boolean equals(Object o) {
    return o != null
        && o instanceof DiscoveryMethodModel
        && ((DiscoveryMethodModel) o).method.equals(method);
  }

  @Override
  public String getSimpleName() {
    return DiscoGapicNamer.methodAsName(method).toLowerCamel();
  }

  @Override
  public String getParentSimpleName() {
    return "getParentSimpleName() not implemented.";
  }

  @Override
  public String getParentNickname(TypeNameConverter typeNameConverter) {
    return null;
  }

  @Override
  public String getAndSaveRequestTypeName(ImportTypeTable typeTable, SurfaceNamer surfaceNamer) {
    return typeTable.getAndSaveNicknameFor(
        surfaceNamer.publicClassName(DiscoGapicNamer.getRequestName(method)));
  }

  @Override
  public String getAndSaveResponseTypeName(ImportTypeTable typeTable, SurfaceNamer surfaceNamer) {
    return typeTable.getAndSaveNicknameFor(
        surfaceNamer.publicClassName((DiscoGapicNamer.getResponseName(method))));
  }

  @Override
  public String getProtoMethodName() {
    return getSimpleName();
  }

  @Override
  public String getScopedDescription() {
    return method.description();
  }

  @Override
  public boolean hasReturnValue() {
    return method.response() != null;
  }
}
