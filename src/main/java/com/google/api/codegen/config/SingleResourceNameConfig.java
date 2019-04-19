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
import com.google.api.codegen.util.Name;
import com.google.api.pathtemplate.PathTemplate;
import com.google.api.pathtemplate.ValidationException;
import com.google.api.tools.framework.model.Diag;
import com.google.api.tools.framework.model.DiagCollector;
import com.google.api.tools.framework.model.ProtoFile;
import com.google.api.tools.framework.model.SimpleLocation;
import com.google.auto.value.AutoValue;
import com.google.common.base.Strings;
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
      DiagCollector diagCollector,
      CollectionConfigProto collectionConfigProto,
      @Nullable ProtoFile file,
      TargetLanguage language) {
    SingleResourceNameConfig.Builder builder = newBuilder();
    String namePattern = collectionConfigProto.getNamePattern();
    String entityId = collectionConfigProto.getEntityName();
    PathTemplate nameTemplate = null;
    if (!Strings.isNullOrEmpty(namePattern)) {
      try {
        nameTemplate = PathTemplate.create(namePattern);
      } catch (ValidationException e) {
        diagCollector.addDiag(Diag.error(SimpleLocation.TOPLEVEL, e.getMessage()));
        return null;
      }
    }

    builder.setNamePattern(namePattern);
    builder.setNameTemplate(nameTemplate);
    builder.setEntityId(entityId);
    builder.setAssignedProtoFile(file);

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
    builder.setCommonResourceName(commonResourceName);
    builder.setEntityName(ResourceNameMessageConfig.entityNameToName(entityName));

    return builder.build();
  }

  /** Returns the name pattern for the resource name config. */
  public abstract String getNamePattern();

  @Nullable
  /** Returns the name template for the resource name config. */
  public abstract PathTemplate getNameTemplate();

  /** Returns the name used for uniquely identifying the entity in config. */
  @Override
  public abstract String getEntityId();

  /** Returns the name used as a basis for generating methods. */
  @Override
  public abstract Name getEntityName();

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

  public static SingleResourceNameConfig.Builder newBuilder() {
    return new AutoValue_SingleResourceNameConfig.Builder();
  }

  public String getUnqualifiedEntityId() {
    return getEntityId().substring(getEntityId().lastIndexOf('.') + 1);
  }

  public abstract Builder toBuilder();

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setNamePattern(String val);

    public abstract Builder setNameTemplate(PathTemplate val);

    public abstract Builder setEntityId(String val);

    public abstract Builder setEntityName(Name val);

    public abstract Builder setCommonResourceName(String val);

    public abstract Builder setAssignedProtoFile(ProtoFile val);

    public abstract SingleResourceNameConfig build();
  }
}
