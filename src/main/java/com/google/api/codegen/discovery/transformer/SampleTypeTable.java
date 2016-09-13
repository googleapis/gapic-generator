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

import com.google.api.codegen.discovery.config.SampleConfig;
import com.google.api.codegen.discovery.config.TypeInfo;
import com.google.api.codegen.util.TypeName;
import com.google.api.codegen.util.TypeTable;
import com.google.api.codegen.util.TypedValue;
import java.util.List;

/**
 * Manages the imports for a set of fully-qualified type names.
 */
public class SampleTypeTable implements SampleTypeNameConverter {

  private TypeTable typeTable;
  private SampleTypeNameConverter typeNameConverter;

  public SampleTypeTable(TypeTable typeTable, SampleTypeNameConverter typeNameConverter) {
    this.typeTable = typeTable;
    this.typeNameConverter = typeNameConverter;
  }

  @Override
  public TypeName getServiceTypeName(SampleConfig sampleConfig) {
    return typeNameConverter.getServiceTypeName(sampleConfig);
  }

  public String getAndSaveNicknameFor(SampleConfig sampleConfig) {
    return typeTable.getAndSaveNicknameFor(getServiceTypeName(sampleConfig));
  }

  @Override
  public TypeName getTypeName(TypeInfo typeInfo) {
    return typeNameConverter.getTypeName(typeInfo);
  }

  public String getAndSaveNicknameFor(TypeInfo typeInfo) {
    return typeTable.getAndSaveNicknameFor(getTypeName(typeInfo));
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
