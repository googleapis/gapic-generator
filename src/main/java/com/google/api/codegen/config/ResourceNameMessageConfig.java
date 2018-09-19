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
import com.google.api.tools.framework.model.DiagCollector;
import com.google.api.tools.framework.model.Field;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;
import javax.annotation.Nullable;

/** Configuration of the resource name types for fields of a single message. */
@AutoValue
public abstract class ResourceNameMessageConfig {

  public abstract String messageName();

  abstract ImmutableMap<String, String> fieldEntityMap();

  @Nullable
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

  @Nullable
  public static ResourceNameMessageConfig createResourceNameMessageConfig(Field field) {
    String messageName = field.getParent().getFullName();
    ImmutableMap<String, String> fieldEntityMap =
        // Assume that each MessageType will only have one Field that has a resource name,
        // And that that Field is the name of the MessageType.
        ImmutableMap.of(field.getSimpleName(), field.getParent().getSimpleName().toLowerCase());

    return new AutoValue_ResourceNameMessageConfig(messageName, fieldEntityMap);
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
