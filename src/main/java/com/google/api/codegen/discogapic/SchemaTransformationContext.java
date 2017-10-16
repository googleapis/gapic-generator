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

import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.codegen.discogapic.transformer.DiscoGapicNamer;
import com.google.api.codegen.transformer.DiscoGapicInterfaceContext;
import com.google.api.codegen.transformer.ImportTypeTable;
import com.google.api.codegen.transformer.SchemaTypeTable;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.transformer.TransformationContext;
import com.google.auto.value.AutoValue;
import java.util.Comparator;
/**
 * The context for transforming a single top-level schema from Discovery Doc API into a top-level
 * view for client library generation.
 *
 * <p>This context contains a reference to the parent Document context.
 */
@AutoValue
public abstract class SchemaTransformationContext implements TransformationContext {
  /**
   * Create a context for transforming a schema.
   *
   * @param id Any sort of unique identifier for this context. Used in sorting contexts.
   * @param typeTable Manages the imports for the schema view.
   * @param docContext The context for the parent Document.
   */
  public static SchemaTransformationContext create(
      String id, SchemaTypeTable typeTable, DiscoGapicInterfaceContext docContext) {
    return new AutoValue_SchemaTransformationContext(id, typeTable, docContext);
  }

  public abstract String id();

  public abstract SchemaTypeTable getSchemaTypeTable();

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
  public ImportTypeTable getImportTypeTable() {
    return getSchemaTypeTable();
  }

  @Override
  public SchemaTransformationContext withNewTypeTable() {
    return create(id(), getSchemaTypeTable().cloneEmpty(), getDocContext());
  }

  @Override
  public SchemaTransformationContext withNewTypeTable(String newPackageName) {
    return create(id(), getSchemaTypeTable().cloneEmpty(newPackageName), getDocContext());
  }

  public static Comparator<SchemaTransformationContext> comparator =
      new Comparator<SchemaTransformationContext>() {
        @Override
        public int compare(SchemaTransformationContext o1, SchemaTransformationContext o2) {
          return String.CASE_INSENSITIVE_ORDER.compare(o1.id(), o2.id());
        }
      };
}
