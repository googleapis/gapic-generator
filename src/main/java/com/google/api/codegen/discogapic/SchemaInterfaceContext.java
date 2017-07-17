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

import com.google.api.codegen.config.DiscoGapicInterfaceConfig;
import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.codegen.discogapic.transformer.DiscoGapicNamer;
import com.google.api.codegen.transformer.DiscoGapicInterfaceContext;
import com.google.api.codegen.transformer.ImportTypeTable;
import com.google.api.codegen.transformer.InterfaceContext;
import com.google.api.codegen.transformer.SchemaTypeTable;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.util.TypeTable;
import com.google.api.tools.framework.model.Interface;
import com.google.auto.value.AutoValue;
import java.util.Comparator;
import javax.annotation.Nullable;

/**
 * The context for transforming a single top-level schema from Discovery Doc API into a top-level
 * view for client library generation.
 *
 * <p>This context contains a reference to the parent Document context.
 */
@AutoValue
public abstract class SchemaInterfaceContext implements InterfaceContext {
  public static SchemaInterfaceContext create(
      String id, SchemaTypeTable typeTable, DiscoGapicInterfaceContext docContext) {
    return new AutoValue_SchemaInterfaceContext(id, typeTable, docContext);
  }

  @Override
  @Nullable
  /* @return null, because a Discovery Doc schema has no proto-based Interface. */
  public Interface getInterface() {
    return null;
  }

  /* Any sort of identifier for this context. Used in sorting contexts. */
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
  public TypeTable getTypeTable() {
    return getSchemaTypeTable().getTypeTable();
  }

  @Override
  public ImportTypeTable getModelTypeTable() {
    return getSchemaTypeTable();
  }

  @Override
  @Nullable
  public DiscoGapicInterfaceConfig getInterfaceConfig() {
    return null;
  }

  public static Comparator<SchemaInterfaceContext> comparator =
      new Comparator<SchemaInterfaceContext>() {
        @Override
        public int compare(SchemaInterfaceContext o1, SchemaInterfaceContext o2) {
          return String.CASE_INSENSITIVE_ORDER.compare(o1.id(), o2.id());
        }
      };
}
