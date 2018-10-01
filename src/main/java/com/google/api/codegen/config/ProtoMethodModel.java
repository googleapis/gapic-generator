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
package com.google.api.codegen.config;

import com.google.api.codegen.configgen.CollectionPattern;
import com.google.api.codegen.gapic.ServiceMessages;
import com.google.api.codegen.transformer.ImportTypeTable;
import com.google.api.codegen.transformer.ModelTypeNameConverter;
import com.google.api.codegen.transformer.ModelTypeTable;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.transformer.TypeNameConverter;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.util.ProtoParser;
import com.google.api.codegen.util.TypeName;
import com.google.api.tools.framework.aspects.documentation.model.DocumentationUtil;
import com.google.api.tools.framework.aspects.http.model.HttpAttribute;
import com.google.api.tools.framework.aspects.http.model.MethodKind;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.FieldSelector;
import com.google.api.tools.framework.model.Method;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** A wrapper around the model of a protobuf-defined Method. */
public final class ProtoMethodModel implements MethodModel {
  private final Method method;
  private List<ProtoField> inputFields;
  private List<ProtoField> outputFields;
  private final TypeModel inputType;
  private final TypeModel outputType;
  private ProtoParser protoParser;

  /* Create a MethodModel object from a non-null Method object. */
  public ProtoMethodModel(Method method) {
    Preconditions.checkNotNull(method);
    this.method = method;
    this.inputType = ProtoTypeRef.create(method.getInputType());
    this.outputType = ProtoTypeRef.create(method.getOutputType());
    this.protoParser = new ProtoParser();
  }

  @VisibleForTesting
  public ProtoMethodModel(Method method, ProtoParser protoParser) {
    this(method);
    this.protoParser = protoParser;
  }

  @Override
  public ProtoField getInputField(String fieldName) {
    Field inputField = method.getInputType().getMessageType().lookupField(fieldName);
    return inputField == null ? null : new ProtoField(inputField);
  }

  @Override
  public ProtoField getOutputField(String fieldName) {
    Field outputField = method.getOutputType().getMessageType().lookupField(fieldName);
    return outputField == null ? null : new ProtoField(outputField);
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
  public TypeName getInputTypeName(ImportTypeTable typeTable) {
    return typeTable
        .getTypeTable()
        .getTypeName(((ModelTypeTable) typeTable).getFullNameFor(method.getInputType()));
  }

  @Override
  public TypeName getOutputTypeName(ImportTypeTable typeTable) {
    return typeTable
        .getTypeTable()
        .getTypeName(((ModelTypeTable) typeTable).getFullNameFor(method.getOutputType()));
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
  public String getOutputTypeSimpleName() {
    return method.getOutputType().getMessageType().getSimpleName();
  }

  @Override
  public boolean getResponseStreaming() {
    return method.getResponseStreaming();
  }

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
  public String getRawName() {
    return method.getSimpleName();
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
  public String getScopedDescription() {
    return DocumentationUtil.getScopedDescription(method);
  }

  @Override
  public List<ProtoField> getInputFields() {
    if (inputFields != null) {
      return inputFields;
    }

    ImmutableList.Builder<ProtoField> fieldsBuilder = ImmutableList.builder();
    for (Field field : method.getInputType().getMessageType().getFields()) {
      fieldsBuilder.add(new ProtoField(field));
    }
    inputFields = fieldsBuilder.build();
    return inputFields;
  }

  @Override
  public List<ProtoField> getInputFieldsForResourceNameMethod() {
    return getInputFields();
  }

  @Override
  public List<ProtoField> getOutputFields() {
    if (outputFields != null) {
      return outputFields;
    }

    ImmutableList.Builder<ProtoField> fieldsBuilder = ImmutableList.builder();
    for (Field field : method.getOutputType().getMessageType().getFields()) {
      fieldsBuilder.add(new ProtoField(field));
    }
    outputFields = fieldsBuilder.build();
    return outputFields;
  }

  @Override
  public List<ProtoField> getResourceNameInputFields() {
    if (getProtoMethod().getInputType().getMessageType().getFields() == null) {
      return new LinkedList<>();
    }
    return getProtoMethod()
        .getInputType()
        .getMessageType()
        .getFields()
        .stream()
        .filter(f -> !Strings.isNullOrEmpty(protoParser.getResourceMessage(f)))
        .map(ProtoField::new)
        .collect(Collectors.toList());
  }

  @Override
  public boolean isIdempotent() {
    HttpAttribute httpAttr = method.getAttribute(HttpAttribute.KEY);
    if (httpAttr == null) {
      return false;
    }
    MethodKind methodKind = httpAttr.getMethodKind();
    return methodKind.isIdempotent();
  }

  @Override
  public Map<String, String> getResourcePatternNameMap(Map<String, String> nameMap) {
    Map<String, String> resources = new LinkedHashMap<>();
    for (CollectionPattern collectionPattern :
        CollectionPattern.getCollectionPatternsFromMethod(method)) {
      String resourceNameString = collectionPattern.getTemplatizedResourcePath();
      resources.put(collectionPattern.getFieldPath(), nameMap.get(resourceNameString));
    }
    return resources;
  }

  @Override
  public TypeModel getInputType() {
    return inputType;
  }

  @Override
  public TypeModel getOutputType() {
    return outputType;
  }

  @Override
  public boolean hasExtraFieldMask() {
    return false;
  }
}
