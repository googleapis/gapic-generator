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
package com.google.api.codegen;

import com.google.api.gax.protobuf.PathTemplate;
import com.google.api.gax.protobuf.ValidationException;
import com.google.api.tools.framework.model.Diag;
import com.google.api.tools.framework.model.DiagCollector;
import com.google.api.tools.framework.model.SimpleLocation;

import javax.annotation.Nullable;

/**
 * CollectionConfig represents the collection configuration for a method.
 */
public class CollectionConfig {
  private final String namePattern;
  private final PathTemplate nameTemplate;
  private final String entityName;

  /**
   * Creates an instance of CollectionConfig based on CollectionConfigProto. On errors, null will be
   * returned, and diagnostics are reported to the diag collector.
   */
  @Nullable
  public static CollectionConfig createCollection(
      DiagCollector diagCollector, CollectionConfigProto collectionConfigProto) {
    String namePattern = collectionConfigProto.getNamePattern();
    PathTemplate nameTemplate;
    try {
      nameTemplate = PathTemplate.create(namePattern);
    } catch (ValidationException e) {
      diagCollector.addDiag(Diag.error(SimpleLocation.TOPLEVEL, e.getMessage()));
      return null;
    }
    String entityName = collectionConfigProto.getEntityName();
    return new CollectionConfig(namePattern, nameTemplate, entityName);
  }

  private CollectionConfig(String namePattern, PathTemplate nameTemplate, String entityName) {
    this.namePattern = namePattern;
    this.nameTemplate = nameTemplate;
    this.entityName = entityName;
  }

  /**
   * Returns the name pattern for resources in the collection.
   */
  public String getNamePattern() {
    return namePattern;
  }

  /**
   * Returns the name template for resources in the collection.
   */
  public PathTemplate getNameTemplate() {
    return nameTemplate;
  }

  /**
   * Returns the name used as a basis for generating methods.
   */
  public String getEntityName() {
    return entityName;
  }
}
