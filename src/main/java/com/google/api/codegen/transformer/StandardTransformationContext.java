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
package com.google.api.codegen.transformer;

import com.google.api.codegen.config.GapicProductConfig;
import com.google.auto.value.AutoValue;

/**
 * The context for transforming some API entity into a top-level view for client library generation.
 */
@AutoValue
public abstract class StandardTransformationContext implements TransformationContext {
  /**
   * Create a context for transforming a schema.
   *
   * @param productConfig Contains client configuration.
   * @param namer Names entities according to language-specific conventions.
   * @param typeTable Manages the imports for the API entity view.
   */
  public static StandardTransformationContext create(
      GapicProductConfig productConfig, SurfaceNamer namer, ImportTypeTable typeTable) {
    return new AutoValue_StandardTransformationContext(namer, typeTable, productConfig);
  }

  @Override
  public abstract SurfaceNamer getNamer();

  @Override
  public abstract ImportTypeTable getImportTypeTable();

  @Override
  public abstract GapicProductConfig getProductConfig();

  @Override
  public StandardTransformationContext withNewTypeTable() {
    return create(getProductConfig(), getNamer(), getImportTypeTable().cloneEmpty());
  }

  @Override
  public StandardTransformationContext withNewTypeTable(String newPackageName) {
    return create(getProductConfig(), getNamer(), getImportTypeTable().cloneEmpty(newPackageName));
  }
}
