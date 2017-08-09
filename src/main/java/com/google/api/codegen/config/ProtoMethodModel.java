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

import com.google.api.codegen.ServiceMessages;
import com.google.api.codegen.transformer.ImportTypeTable;
import com.google.api.codegen.transformer.ModelTypeFormatter;
import com.google.api.codegen.transformer.ModelTypeNameConverter;
import com.google.api.codegen.transformer.ModelTypeTable;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.transformer.TypeFormatter;
import com.google.api.codegen.transformer.TypeNameConverter;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.util.TypeName;
import com.google.api.tools.framework.aspects.documentation.model.DocumentationUtil;
import com.google.api.tools.framework.model.FieldSelector;
import com.google.api.tools.framework.model.Method;
import com.google.common.base.Preconditions;

/** Created by andrealin on 8/1/17. */
public final class ProtoMethodModel implements MethodModel {
  private final Method method;

  /* Create a MethodModel object from a non-null Method object. */
  public ProtoMethodModel(Method method) {
    Preconditions.checkNotNull(method);
    this.method = method;
  }

  @Override
  public ApiSource getApiSource() {
    return ApiSource.PROTO;
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
  public String getOutputFullName() {
    return method.getOutputType().getMessageType().getFullName();
  }

  @Override
  public String getInputTypeFullName(ImportTypeTable typeTable) {
    return ((ModelTypeTable) typeTable).getFullNameFor(method.getInputType());
  }

  @Override
  public String getOutputTypeFullName(ImportTypeTable typeTable) {
    return ((ModelTypeTable) typeTable).getFullNameFor(method.getOutputType());
  }

  @Override
  public String getParentSimpleName() {
    return method.getParent().getSimpleName();
  }

  @Override
  public String getDescription() {
    return DocumentationUtil.getDescription(method);
  }

  @Override
  public String getOutputTypeFullName(TypeFormatter typeFormatter) {
    return ((ModelTypeFormatter) typeFormatter).getFullNameFor(method.getOutputType());
  }

  @Override
  public String getInputTypeNickname(TypeFormatter typeFormatter) {
    return ((ModelTypeFormatter) typeFormatter).getNicknameFor(method.getInputType());
  }

  @Override
  public TypeName getOutputTypeName(TypeNameConverter typeFormatter) {
    return ((ModelTypeNameConverter) typeFormatter).getTypeName(method.getOutputType());
  }

  @Override
  public GenericFieldSelector getInputFieldSelector(String fieldName) {
    return new ProtoFieldSelector(
        FieldSelector.resolve(method.getInputType().getMessageType(), fieldName));
  }

  @Override
  public boolean getRequestStreaming() {
    return method.getRequestStreaming();
  }

  @Override
  public String getParentNickname(TypeNameConverter converter) {
    return ((ModelTypeNameConverter) converter).getTypeName(method.getParent()).getNickname();
  }

  @Override
  public String getOutputTypeNickname(ImportTypeTable typeTable) {
    return ((ModelTypeTable) typeTable).getAndSaveNicknameFor(method.getOutputType());
  }

  @Override
  public String getOutputTypeSimpleName() {
    return method.getOutputType().getMessageType().getSimpleName();
  }

  @Override
  public boolean getResponseStreaming() {
    return method.getResponseStreaming();
  }

  // TODO(andrealin): Eliminate all uses of this function.
  @Deprecated
  public Method getProtoMethod() {
    return method;
  }

  @Override
  public boolean equals(Object o) {
    return o != null
        && o instanceof ProtoMethodModel
        && ((ProtoMethodModel) o).method.equals(method);
  }

  @Override
  public Name asName() {
    return Name.upperCamel(method.getSimpleName());
  }

  @Override
  public String getSimpleName() {
    return method.getSimpleName();
  }

  @Override
  public boolean isOutputTypeEmpty() {
    return new ServiceMessages().isEmptyType(method.getOutputType());
  }

  @Override
  public String getAndSaveRequestTypeName(ImportTypeTable typeTable, SurfaceNamer surfaceNamer) {
    return ((ModelTypeTable) typeTable).getAndSaveNicknameFor(method.getInputType());
  }

  @Override
  public String getAndSaveResponseTypeName(ImportTypeTable typeTable, SurfaceNamer surfaceNamer) {
    return ((ModelTypeTable) typeTable).getAndSaveNicknameFor(method.getOutputType());
  }

  @Override
  public String getProtoMethodName() {
    return method.getSimpleName();
  }

  @Override
  public String getScopedDescription() {
    return DocumentationUtil.getScopedDescription(method);
  }

  @Override
  public boolean hasReturnValue() {
    return !(new ServiceMessages()).isEmptyType(method.getOutputType());
  }
}
