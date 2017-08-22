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
import com.google.api.codegen.transformer.SchemaTypeTable;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.transformer.TypeNameConverter;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.util.TypeName;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;

/** A wrapper around the model of a Discovery Method. */
public final class DiscoveryMethodModel implements MethodModel {
  private final Method method;
  private Iterable<FieldModel> inputFields;
  private final DiscoGapicNamer discoGapicNamer;

  /* Create a DiscoveryMethodModel from a non-null Discovery Method object. */
  public DiscoveryMethodModel(Method method, DiscoGapicNamer discoGapicNamer) {
    Preconditions.checkNotNull(method);
    this.method = method;
    this.discoGapicNamer = discoGapicNamer;
  }

  public Method getDiscoMethod() {
    return method;
  }

  @Override
  public String getOutputTypeSimpleName() {
    return method.response() == null ? "none" : method.response().id();
  }

  @Override
  public ApiSource getApiSource() {
    return ApiSource.DISCOVERY;
  }

  /**
   * Returns the parameter with the fieldName if it exists, otherwise returns the request object
   * with name fieldName, if it exists.
   */
  @Override
  public FieldModel getInputField(String fieldName) {
    Schema targetSchema = method.parameters().get(fieldName);
    if (targetSchema != null) {
      return new DiscoveryField(targetSchema, discoGapicNamer);
    }
    if (method.request() != null
        && !Strings.isNullOrEmpty(method.request().reference())
        && method.request().reference().toLowerCase().equals(fieldName.toLowerCase())) {
      return new DiscoveryField(method.request().dereference(), discoGapicNamer);
    }
    return null;
  }

  @Override
  public FieldModel getOutputField(String fieldName) {
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
  public TypeName getOutputTypeName(ImportTypeTable typeTable) {
    // Maybe use Discogapic namer for this?
    return typeTable
        .getTypeTable()
        .getTypeName(((SchemaTypeTable) typeTable).getFullNameFor(method.response()));
  }

  @Override
  public String getOutputFullName() {
    return method.response() == null ? "none" : method.response().getIdentifier();
  }

  @Override
  public TypeName getInputTypeName(ImportTypeTable typeTable) {
    return typeTable
        .getTypeTable()
        .getTypeName(((SchemaTypeTable) typeTable).getFullNameFor(method.request()));
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
    TypeName fullName =
        typeTable
            .getTypeTable()
            .getTypeNameInImplicitPackage(
                surfaceNamer.publicClassName(DiscoGapicNamer.getRequestName(method)));
    return typeTable.getAndSaveNicknameFor(fullName.getFullName());
  }

  @Override
  public String getAndSaveResponseTypeName(ImportTypeTable typeTable, SurfaceNamer surfaceNamer) {
    TypeName fullName =
        typeTable
            .getTypeTable()
            .getTypeNameInImplicitPackage(
                surfaceNamer.publicClassName(DiscoGapicNamer.getResponseName(method)));
    return typeTable.getAndSaveNicknameFor(fullName.getFullName());
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

  @Override
  public Iterable<FieldModel> getInputFields() {
    if (inputFields != null) {
      return inputFields;
    }

    ImmutableList.Builder<FieldModel> fieldsBuilder = ImmutableList.builder();
    for (Schema field : method.parameters().values()) {
      fieldsBuilder.add(new DiscoveryField(field, discoGapicNamer));
    }
    if (method.request() != null && !Strings.isNullOrEmpty(method.request().reference())) {
      fieldsBuilder.add(new DiscoveryField(method.request().dereference(), discoGapicNamer));
    }
    inputFields = fieldsBuilder.build();
    return inputFields;
  }
}
