/* Copyright 2016 Google LLC
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

import com.google.api.codegen.CollectionConfigProto;
import com.google.api.pathtemplate.PathTemplate;
import com.google.api.pathtemplate.ValidationException;
import com.google.api.tools.framework.model.Diag;
import com.google.api.tools.framework.model.DiagCollector;
import com.google.api.tools.framework.model.ProtoFile;
import com.google.api.tools.framework.model.SimpleLocation;
import com.google.auto.value.AutoValue;
import javax.annotation.Nullable;

/** SingleResourceNameConfig represents the collection configuration for a method. */
@AutoValue
public abstract class SingleResourceNameConfig implements ResourceNameConfig {

  /**
   * Creates an instance of SingleResourceNameConfig based on CollectionConfigProto. On errors, null
   * will be returned, and diagnostics are reported to the diag collector.
   */
  @Nullable
  public static SingleResourceNameConfig createSingleResourceName(
      DiagCollector diagCollector, CollectionConfigProto collectionConfigProto, ProtoFile file) {
    String namePattern = collectionConfigProto.getNamePattern();
    PathTemplate nameTemplate;
    try {
      nameTemplate = PathTemplate.create(namePattern);
    } catch (ValidationException e) {
      diagCollector.addDiag(Diag.error(SimpleLocation.TOPLEVEL, e.getMessage()));
      return null;
    }
    String entityName = collectionConfigProto.getEntityName();
    return new AutoValue_SingleResourceNameConfig(namePattern, nameTemplate, entityName, file);
  }

  /** Returns the name pattern for the resource name config. */
  public abstract String getNamePattern();

  /** Returns the name template for the resource name config. */
  public abstract PathTemplate getNameTemplate();

  /** Returns the name used as a basis for generating methods. */
  @Override
  public abstract String getEntityName();

  @Override
  @Nullable
  public abstract ProtoFile getAssignedProtoFile();

  @Override
  public ResourceNameType getResourceNameType() {
    return ResourceNameType.SINGLE;
  }
}
