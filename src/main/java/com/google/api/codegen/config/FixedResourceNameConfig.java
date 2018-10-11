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

import com.google.api.Resource;
import com.google.api.codegen.FixedResourceNameValueProto;
import com.google.api.codegen.util.ProtoParser;
import com.google.api.tools.framework.model.Diag;
import com.google.api.tools.framework.model.DiagCollector;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.ProtoFile;
import com.google.api.tools.framework.model.SimpleLocation;
import com.google.auto.value.AutoValue;
import com.google.common.base.Strings;
import javax.annotation.Nullable;

/**
 * FixedResourceNameConfig represents a resource name configuration that accepts a particular fixed
 * value.
 */
@AutoValue
public abstract class FixedResourceNameConfig implements ResourceNameConfig {

  public abstract String getFixedValue();

  @Override
  public abstract ProtoFile getAssignedProtoFile();

  @Override
  @Nullable
  public String getCommonResourceName() {
    return null;
  }

  @Override
  public ResourceNameType getResourceNameType() {
    return ResourceNameType.FIXED;
  }

  @Nullable
  public static FixedResourceNameConfig createFixedResourceNameConfig(
      DiagCollector diagCollector,
      FixedResourceNameValueProto fixedResourceNameValueProto,
      @Nullable ProtoFile file) {

    String entityName = fixedResourceNameValueProto.getEntityName();
    String fixedValue = fixedResourceNameValueProto.getFixedValue();

    if (entityName == null || fixedValue == null) {
      diagCollector.addDiag(
          Diag.error(
              SimpleLocation.TOPLEVEL,
              "incorrectly configured FixedResourceNameConfig: name: "
                  + entityName
                  + ", value: "
                  + fixedValue));
      return null;
    }

    return new AutoValue_FixedResourceNameConfig(entityName, entityName, fixedValue, file);
  }

  @Nullable
  static FixedResourceNameConfig createFixedResourceNameConfig(
      DiagCollector diagCollector, Field field, Resource resource, ProtoParser protoParser) {
    String entityName = resource.getBaseName();
    if (Strings.isNullOrEmpty(entityName)) {
      entityName = protoParser.getResourceEntityName(field);
    }
    String fixedValue = resource.getPath();

    if (entityName == null || fixedValue == null) {
      diagCollector.addDiag(
          Diag.error(
              SimpleLocation.TOPLEVEL,
              "incorrectly configured FixedResourceNameConfig: name: "
                  + entityName
                  + ", value: "
                  + fixedValue));
      return null;
    }

    return new AutoValue_FixedResourceNameConfig(
        entityName, entityName, fixedValue, field.getFile());
  }
}
