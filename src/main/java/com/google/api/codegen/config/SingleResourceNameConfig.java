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
import com.google.api.codegen.CollectionLanguageOverridesProto;
import com.google.api.codegen.common.TargetLanguage;
import com.google.api.codegen.util.Inflector;
import com.google.api.codegen.util.ProtoParser;
import com.google.api.pathtemplate.PathTemplate;
import com.google.api.pathtemplate.ValidationException;
import com.google.api.tools.framework.model.Diag;
import com.google.api.tools.framework.model.DiagCollector;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.ProtoFile;
import com.google.api.tools.framework.model.SimpleLocation;
import com.google.auto.value.AutoValue;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/** SingleResourceNameConfig represents the collection configuration for a method. */
@AutoValue
public abstract class SingleResourceNameConfig implements ResourceNameConfig {

  /**
   * Creates an instance of SingleResourceNameConfig based on CollectionConfigProto. On errors, null
   * will be returned, and diagnostics are reported to the diag collector.
   */
  @Nullable
  public static SingleResourceNameConfig createSingleResourceName(
      DiagCollector diagCollector,
      CollectionConfigProto collectionConfigProto,
      ProtoFile file,
      TargetLanguage language) {
    String namePattern = collectionConfigProto.getNamePattern();
    PathTemplate nameTemplate;
    try {
      String nameTemplateString = escapePathTemplate(namePattern);
      nameTemplate = PathTemplate.create(nameTemplateString);
    } catch (ValidationException e) {
      diagCollector.addDiag(Diag.error(SimpleLocation.TOPLEVEL, e.getMessage()));
      return null;
    }
    String entityId = collectionConfigProto.getEntityName();
    String entityName = entityId;
    String commonResourceName = null;
    if (language != null) {
      String languageStr = language.toString().toLowerCase();
      for (CollectionLanguageOverridesProto override :
          collectionConfigProto.getLanguageOverridesList()) {
        if (languageStr.equals(override.getLanguage())) {
          if (!Strings.isNullOrEmpty(override.getEntityName())) {
            entityName = override.getEntityName();
          }
          if (!Strings.isNullOrEmpty(override.getCommonResourceName())) {
            commonResourceName = override.getCommonResourceName();
          }
        }
      }
    }
    return new AutoValue_SingleResourceNameConfig(
        namePattern, nameTemplate, entityId, entityName, commonResourceName, file);
  }

  // Wrapper for PathTemplate.create().
  // If there are literal '*' wildcards, replace them with an appropriate string representing a resource.
  // e.g. createPathTemplate("bookShelves/*/books/{book}") returns
  // PathTemplate.create("bookShelves/{bookShelf}/books/{book}")
  @VisibleForTesting
  // TODO(andrealin): Test this.
  static String escapePathTemplate(String template) {
    String[] pieces = template.split("/");
    List<String> newPieces = Arrays.asList(pieces);
    // Iterate only over wildcard pieces.
    for (int i = 1; i < pieces.length; i = i+2) {
      String piece = pieces[i];
      String prevPiece = pieces[i-1];
      if (piece.equals("*")) {
        piece = String.format("{%s}", Inflector.singularize(prevPiece));
        newPieces.set(i, piece);
      }
    }

    return String.join("/", newPieces);
  }

  /**
   * Creates an instance of SingleResourceNameConfig based on a field. On errors, null will be
   * returned, and diagnostics are reported to the diag collector.
   */
  @Nullable
  public static SingleResourceNameConfig createSingleResourceName(
      DiagCollector diagCollector, Field resourceField, ProtoFile file) {
    String namePattern = ProtoParser.getResourcePath(resourceField);
    PathTemplate nameTemplate;
    try {
      String nameTemplateString = escapePathTemplate(namePattern);
      nameTemplate = PathTemplate.create(nameTemplateString);
    } catch (ValidationException e) {
      diagCollector.addDiag(Diag.error(SimpleLocation.TOPLEVEL, e.getMessage()));
      return null;
    }
    String entityId = ProtoParser.getResourceEntityName(resourceField);
    String entityName = entityId;
    String commonResourceName = null;
    return new AutoValue_SingleResourceNameConfig(
        namePattern, nameTemplate, entityId, entityName, commonResourceName, file);
  }

  /** Returns the name pattern for the resource name config. */
  public abstract String getNamePattern();

  /** Returns the name template for the resource name config. */
  public abstract PathTemplate getNameTemplate();

  /** Returns the name used for uniquely identifying the entity in config. */
  @Override
  public abstract String getEntityId();

  /** Returns the name used as a basis for generating methods. */
  @Override
  public abstract String getEntityName();

  @Override
  @Nullable
  public abstract String getCommonResourceName();

  @Override
  @Nullable
  public abstract ProtoFile getAssignedProtoFile();

  @Override
  public ResourceNameType getResourceNameType() {
    return ResourceNameType.SINGLE;
  }
}
