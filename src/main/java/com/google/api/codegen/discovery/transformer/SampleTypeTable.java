/* Copyright 2016 Google Inc
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
package com.google.api.codegen.discovery.transformer;

import com.google.api.codegen.discovery.TypeInfo;
import com.google.api.codegen.util.TypeName;
import com.google.api.codegen.util.TypeTable;
import com.google.api.codegen.util.TypedValue;
import java.util.List;

/**
 * Manages the imports for a set of fully-qualified type names.
 */
public class SampleTypeTable implements ProtobufTypeNameConverter {

  private TypeTable typeTable;
  private ProtobufTypeNameConverter typeNameConverter;

  public SampleTypeTable(TypeTable typeTable, ProtobufTypeNameConverter typeNameConverter) {
    this.typeTable = typeTable;
    this.typeNameConverter = typeNameConverter;
  }

  @Override
  public TypeName getServiceTypeName() {
    return typeNameConverter.getServiceTypeName();
  }

  public String getAndSaveNicknameForServiceTypeName() {
    return typeTable.getAndSaveNicknameFor(typeNameConverter.getServiceTypeName());
  }

  @Override
  public TypeName getRequestTypeName(TypeInfo typeInfo) {
    return typeNameConverter.getRequestTypeName(typeInfo);
  }

  @Override
  public TypeName getTypeName(TypeInfo typeInfo) {
    return typeNameConverter.getTypeName(typeInfo);
  }

  public String getAndSaveNicknameFor(TypeInfo typeInfo, boolean isRequestType) {
    if (isRequestType) {
      return typeTable.getAndSaveNicknameFor(typeNameConverter.getRequestTypeName(typeInfo));
    }
    return typeTable.getAndSaveNicknameFor(typeNameConverter.getTypeName(typeInfo));
  }

  @Override
  public TypedValue getZeroValue(TypeInfo typeInfo) {
    return typeNameConverter.getZeroValue(typeInfo);
  }

  public String getZeroValueAndSaveNicknameFor(TypeInfo type) {
    return typeNameConverter.getZeroValue(type).getValueAndSaveTypeNicknameIn(typeTable);
  }

  public void saveNicknameFor(String fullName) {
    typeTable.getAndSaveNicknameFor(fullName);
  }

  public List<String> getImports() {
    return typeTable.getImports();
  }
}
