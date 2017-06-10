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
package com.google.api.codegen.discogapic;

import com.google.api.codegen.config.GapicInterfaceConfig;
import com.google.api.codegen.config.GapicMethodConfig;
import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.codegen.config.ProductConfig;
import com.google.api.codegen.discogapic.transformer.DiscoGapicNamer;
import com.google.api.codegen.discovery.Document;
import com.google.api.codegen.discovery.Schema;
import com.google.api.codegen.transformer.FeatureConfig;
import com.google.api.codegen.transformer.InterfaceContext;
import com.google.api.codegen.transformer.SchemaTypeTable;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.util.SymbolTable;
import com.google.api.codegen.util.TypeTable;
import com.google.api.tools.framework.model.Method;
import com.google.auto.value.AutoValue;

/**
 * The context for transforming a single top-level schema from Discovery Doc API into a top-level
 * view model for client library generation.
 *
 * This context contains a reference to the parent Document context.
 */
@AutoValue
public abstract class SchemaInterfaceContext implements InterfaceContext {
  public static SchemaInterfaceContext create(
      Schema schema,
      SchemaTypeTable typeTable,
      SymbolTable symbolTable,
      DiscoGapicInterfaceContext docContext) {
    return new AutoValue_SchemaInterfaceContext(
        schema, typeTable, symbolTable, docContext);
  }

  public abstract Schema getSchema();

  public abstract SchemaTypeTable getSchemaTypeTable();

  public abstract SymbolTable getSymbolTable();

  /** @return the parent Document-level InterfaceContext. */
  public abstract DiscoGapicInterfaceContext getDocContext();

  public DiscoGapicNamer getDiscoGapicNamer() {
    return getDocContext().getDiscoGapicNamer();
  }

  @Override
  public GapicProductConfig getProductConfig() {
    return getDocContext().getProductConfig();
  }

  @Override
  public SurfaceNamer getNamer() {
    return getDocContext().getNamer();
  }

  @Override
  public TypeTable getTypeTable() {
    return getSchemaTypeTable().getTypeTable();
  }

  /** @return the SchemaTypeTable scoped at the Document level. */
  public SchemaTypeTable getDocumentTypeTable() {
    return getDocContext().getSchemaTypeTable();
  }

  public SchemaInterfaceContext withNewTypeTable() {
    return create(
        getSchema(),
        getSchemaTypeTable().cloneEmpty(),
        getSymbolTable(),
        getDocContext());
  }

  public DiscoGapicInterfaceConfig getInterfaceConfig() {
    return (DiscoGapicInterfaceConfig) getProductConfig().getInterfaceConfig(getSchema().id());
  }
}
