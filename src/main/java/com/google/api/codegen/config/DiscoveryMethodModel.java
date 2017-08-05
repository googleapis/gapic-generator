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
import com.google.api.codegen.discogapic.transformer.DiscoGapicNamer;
import com.google.api.codegen.discovery.Method;
import com.google.api.codegen.transformer.ImportTypeTable;
import com.google.api.codegen.transformer.SchemaTypeFormatter;
import com.google.api.codegen.transformer.SchemaTypeNameConverter;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.transformer.TypeFormatter;
import com.google.api.codegen.transformer.TypeNameConverter;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.util.TypeName;
import com.google.common.base.Preconditions;

/** Created by andrealin on 8/1/17. */
public final class DiscoveryMethodModel implements MethodModel {
  private final Method method;
  private final ApiSource apiSource = ApiSource.DISCOVERY;

  /* Create a DiscoveryMethodModel from a non-null Discovery Method object. */
  public DiscoveryMethodModel(Method method) {
    Preconditions.checkNotNull(method);
    this.method = method;
  }

  /* Package private for internal use. */
  Method getDiscoveryMethod() {
    return method;
  }

  @Override
  public String getOutputTypeSimpleName() {
    return method.response() == null ? "none" : method.response().id();
  }

  @Override
  public FieldType.ApiSource getApiSource() {
    return apiSource;
  }

  @Override
  public FieldType lookupInputField(String fieldName) {
    return new DiscoveryField(method.parameters().get(fieldName));
  }

  @Override
  public FieldType lookupOutputField(String fieldName) {
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
  public String getOutputTypeNickname(TypeFormatter typeFormatter) {
    return null;
  }

  @Override
  public String getInputTypeNickName(TypeFormatter typeFormatter) {
    return null;
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
    return Name.anyCamel(DiscoGapicNamer.getMethodNamePieces(method.id()));
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
    return Name.from(DiscoGapicNamer.getMethodNamePieces(method.id())).toLowerCamel();
  }

  @Override
  public String getParentSimpleName() {
    return "parent?????!!?!?!?";
  }

  @Override
  public String getParentNickname(TypeNameConverter typeNameConverter) {
    return null;
  }

  @Override
  public String getAndSaveRequestTypeName(ImportTypeTable typeTable, SurfaceNamer discoGapicNamer) {
    return typeTable.getAndSaveNicknameFor(
        ((DiscoGapicNamer) discoGapicNamer).getRequestName(method));
  }

  @Override
  public String getAndSaveResponseTypeName(
      ImportTypeTable typeTable, SurfaceNamer discoGapicNamer) {
    return typeTable.getAndSaveNicknameFor(
        ((DiscoGapicNamer) discoGapicNamer).getResponseName(method));
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
