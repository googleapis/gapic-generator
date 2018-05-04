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

import com.google.api.codegen.discogapic.EmptyTypeModel;
import com.google.api.codegen.discogapic.transformer.DiscoGapicParser;
import com.google.api.codegen.discovery.Method;
import com.google.api.codegen.discovery.Schema;
import com.google.api.codegen.transformer.ImportTypeTable;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.transformer.TypeNameConverter;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.util.TypeName;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.ImmutableSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/** A wrapper around the model of a Discovery Method. */
public final class DiscoveryMethodModel implements MethodModel {
  private ImmutableSet<String> IDEMPOTENT_HTTP_METHODS =
      ImmutableSet.of("GET", "HEAD", "PUT", "DELETE");
  private final Method method;
  private final DiscoveryRequestType inputType;
  private final TypeModel outputType;
  private List<DiscoveryField> inputFields;
  private List<DiscoveryField> outputFields;
  private List<DiscoveryField> resourceNameInputFields;
  private final DiscoApiModel apiModel;

  /* Create a DiscoveryMethodModel from a non-null Discovery Method object. */
  public DiscoveryMethodModel(Method method, DiscoApiModel apiModel) {
    Preconditions.checkNotNull(method);
    this.method = method;
    this.apiModel = apiModel;
    this.inputType = DiscoveryRequestType.create(this);
    if (method.response() != null) {
      this.outputType = DiscoveryField.create(method.response(), apiModel);
    } else {
      this.outputType = EmptyTypeModel.getInstance();
    }
  }

  public Method getDiscoMethod() {
    return method;
  }

  @Override
  public String getOutputTypeSimpleName() {
    return outputType.getTypeName();
  }

  /**
   * Returns the parameter with the fieldName if it exists, otherwise returns the request object
   * with name fieldName, if it exists.
   */
  @Override
  public DiscoveryField getInputField(String fieldName) {
    Schema targetSchema = method.parameters().get(fieldName);
    if (targetSchema != null) {
      return DiscoveryField.create(targetSchema, apiModel);
    }
    if (method.request() != null
        && DiscoGapicParser.getMethodInputName(method).toLowerCamel().equals(fieldName)) {
      return DiscoveryField.create(method.request(), apiModel);
    }
    return null;
  }

  @Override
  public DiscoveryField getOutputField(String fieldName) {
    if (outputType.isEmptyType() || outputType.isPrimitive()) {
      return null;
    }
    return ((DiscoveryField) outputType).getField(fieldName);
  }

  @Override
  public String getFullName() {
    return method.id();
  }

  @Override
  public String getRawName() {
    return method.id();
  }

  @Override
  public String getInputFullName() {
    return method.request().getIdentifier();
  }

  @Override
  public String getDescription() {
    return method.description();
  }

  @Override
  public TypeName getOutputTypeName(ImportTypeTable typeTable) {
    return typeTable.getTypeTable().getTypeName(typeTable.getFullNameFor(outputType));
  }

  @Override
  public String getOutputFullName() {
    return outputType.getTypeName();
  }

  @Override
  public TypeName getInputTypeName(ImportTypeTable typeTable) {
    return typeTable.getTypeTable().getTypeName(typeTable.getFullNameFor(inputType));
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
    return DiscoGapicParser.methodAsName(method);
  }

  @Override
  public boolean isOutputTypeEmpty() {
    return outputType == null || outputType.isEmptyType();
  }

  @Override
  public boolean equals(Object o) {
    return o != null
        && o instanceof DiscoveryMethodModel
        && ((DiscoveryMethodModel) o).method.equals(method);
  }

  @Override
  public String getSimpleName() {
    return DiscoGapicParser.methodAsName(method).toLowerCamel();
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
    return typeTable.getAndSaveNicknameFor(inputType);
  }

  @Override
  public String getAndSaveResponseTypeName(ImportTypeTable typeTable, SurfaceNamer surfaceNamer) {
    return typeTable.getAndSaveNicknameFor(outputType);
  }

  @Override
  public String getScopedDescription() {
    return method.description();
  }

  @Override
  public List<DiscoveryField> getResourceNameInputFields() {
    if (resourceNameInputFields != null) {
      return resourceNameInputFields;
    }

    ImmutableList.Builder<DiscoveryField> params = ImmutableList.builder();
    for (DiscoveryField field : inputFields) {
      if (field.getDiscoveryField().isPathParam()) {
        params.add(field);
      }
    }
    resourceNameInputFields = params.build();
    return resourceNameInputFields;
  }

  @Override
  public List<DiscoveryField> getInputFieldsForResourceNameMethod() {
    List<DiscoveryField> fields = new LinkedList<>();
    for (DiscoveryField field : getInputFields()) {
      if (!getResourceNameInputFields().contains(field)) {
        // Only add fields that aren't part of the ResourceName.
        fields.add(field);
      }
    }

    // Add the field that represents the ResourceName.
    String resourceName = DiscoGapicParser.getResourceIdentifier(method.flatPath()).toLowerCamel();
    for (DiscoveryField field : getInputFields()) {
      if (field.getNameAsParameterName().toLowerCamel().equals(resourceName)) {
        fields.add(field);
        break;
      }
    }
    return fields;
  }

  @Override
  public List<DiscoveryField> getInputFields() {
    if (inputFields != null) {
      return inputFields;
    }

    ImmutableList.Builder<DiscoveryField> fieldsBuilder = ImmutableList.builder();
    for (Schema field : method.parameters().values()) {
      fieldsBuilder.add(DiscoveryField.create(field, apiModel));
    }
    if (method.request() != null) {
      fieldsBuilder.add(DiscoveryField.create(method.request(), apiModel));
    }
    inputFields = fieldsBuilder.build();
    return inputFields;
  }

  /**
   * Returns a list containing the response schema as the sole element; or returns an empty list if
   * this method has no response schema.
   */
  @Override
  public List<DiscoveryField> getOutputFields() {
    if (outputFields != null) {
      return outputFields;
    }

    ImmutableList.Builder<DiscoveryField> outputField = new Builder<>();
    if (method.response() != null) {
      DiscoveryField fieldModel = DiscoveryField.create(method.response(), apiModel);
      outputField.add(fieldModel);
    }
    outputFields = outputField.build();
    return outputFields;
  }

  /**
   * Return if this method, as an HTTP method, is idempotent. Based off {@link
   * com.google.api.tools.framework.aspects.http.model.MethodKind}.
   */
  @Override
  public boolean isIdempotent() {
    String httpMethod = method.httpMethod().toUpperCase();
    return IDEMPOTENT_HTTP_METHODS.contains(httpMethod);
  }

  @Override
  public Map<String, String> getResourcePatternNameMap(Map<String, String> nameMap) {
    Map<String, String> resources = new LinkedHashMap<>();
    for (Map.Entry<String, String> entry : nameMap.entrySet()) {
      if (DiscoGapicParser.getCanonicalPath(method.flatPath()).equals(entry.getKey())) {
        String resourceNameString =
            DiscoGapicParser.getResourceIdentifier(entry.getKey()).toLowerCamel();
        resources.put(resourceNameString, entry.getValue());
        break;
      }
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
}
