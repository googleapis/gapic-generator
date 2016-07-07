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
package com.google.api.codegen.transformer;

import com.google.api.codegen.php.PhpTypeTable;
import com.google.api.tools.framework.model.TypeRef;

import java.util.List;

import org.apache.commons.lang3.NotImplementedException;

public class ModelToPhpTypeTable implements ModelTypeTable {
  private PhpTypeTable phpTypeTable;

  public ModelToPhpTypeTable() {
    phpTypeTable = new PhpTypeTable();
  }

  @Override
  public ModelTypeTable cloneEmpty() {
    return new ModelToPhpTypeTable();
  }

  @Override
  public void addImport(String longName) {
    importAndGetShortestName(longName);
  }

  @Override
  public String importAndGetShortestName(String longName) {
    return phpTypeTable.importAndGetShortestName(longName);
  }

  @Override
  public String importAndGetShortestName(TypeRef inputType) {
    throw new NotImplementedException("PhpIdentifierNamer.importAndGetShortestName(TypeRef)");
  }

  @Override
  public String importAndGetShortestNameForElementType(TypeRef type) {
    throw new NotImplementedException("PhpIdentifierNamer.importAndGetShortestNameForElementType");
  }

  @Override
  public String renderPrimitiveValue(TypeRef keyType, String key) {
    throw new NotImplementedException("PhpIdentifierNamer.renderPrimitiveValue");
  }

  @Override
  public String importAndGetZeroValue(TypeRef type) {
    throw new NotImplementedException("PhpIdentifierNamer.importAndGetZeroValue");
  }

  @Override
  public List<String> getImports() {
    return phpTypeTable.getImports();
  }
}
