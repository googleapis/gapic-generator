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

import com.google.api.codegen.ResourceNameMessageConfigProto;
import com.google.api.codegen.util.ProtoParser;
import com.google.api.tools.framework.model.DiagCollector;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.MessageType;
import com.google.auto.value.AutoValue;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;

/** Configuration of the resource name types for fields of a single message. */
@AutoValue
public abstract class ResourceNameMessageConfig {

  // Fully qualified name of the message that this resource name represents.
  public abstract String messageName();

  // Maps the simple name of a field to the name of a resource entity (a resource entity
  // contains a resource URL).
  abstract ImmutableMap<String, String> fieldEntityMap();

  public static ResourceNameMessageConfig createResourceNameMessageConfig(
      DiagCollector diagCollector,
      ResourceNameMessageConfigProto messageResourceTypesProto,
      String defaultPackage) {
    String messageName = messageResourceTypesProto.getMessageName();
    String fullyQualifiedMessageName = getFullyQualifiedMessageName(defaultPackage, messageName);
    ImmutableMap<String, String> fieldEntityMap =
        ImmutableMap.copyOf(messageResourceTypesProto.getFieldEntityMap());

    return new AutoValue_ResourceNameMessageConfig(fullyQualifiedMessageName, fieldEntityMap);
  }

  public static ResourceNameMessageConfig createResourceNameMessageConfig(
      MessageType message, ProtoParser protoParser) {
    ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
    for (Field field : message.getFields()) {
      // TODO(andrealin): Can there be multiple fields be Resource[Set]s in a single message?
      String baseName = protoParser.getResourceOrSetEntityName(field);
      if (!Strings.isNullOrEmpty(baseName)) {
        builder.put(field.getSimpleName(), baseName);
        continue;
      }

      String resourceType = protoParser.getResourceTypeEntityName(field);
      if (!Strings.isNullOrEmpty(resourceType)) {
        builder.put(field.getSimpleName(), resourceType);
      }
    }

    ImmutableMap<String, String> fieldEntityMap = builder.build();
    if (fieldEntityMap.isEmpty()) {
      // Return a null config when no fields were resource types; this is so empty proto annotations
      // don't override the GAPIC config resource name configs.
      return null;
    }
    return new AutoValue_ResourceNameMessageConfig(message.getFullName(), fieldEntityMap);
  }

  public static String getFullyQualifiedMessageName(String defaultPackage, String messageName) {
    if (messageName.contains(".")) {
      return messageName;
    } else {
      return defaultPackage + "." + messageName;
    }
  }

  public String getEntityNameForField(String fieldSimpleName) {
    return fieldEntityMap().get(fieldSimpleName);
  }
}
