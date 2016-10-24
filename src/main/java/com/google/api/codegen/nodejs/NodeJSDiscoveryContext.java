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
package com.google.api.codegen.nodejs;

import com.google.api.Service;
import com.google.api.codegen.ApiaryConfig;
import com.google.api.codegen.DiscoveryContext;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.Field;
import com.google.protobuf.Method;
import com.google.protobuf.Type;

/** A DiscoveryContext specialized for NodeJS. */
public class NodeJSDiscoveryContext extends DiscoveryContext implements NodeJSContext {

  public NodeJSDiscoveryContext(Service service, ApiaryConfig apiaryConfig) {
    super(service, apiaryConfig);
  }

  private static final ImmutableMap<Field.Kind, String> DEFAULT_VALUES =
      ImmutableMap.<Field.Kind, String>builder()
          .put(Field.Kind.TYPE_UNKNOWN, "{}")
          .put(Field.Kind.TYPE_BOOL, "false")
          .put(Field.Kind.TYPE_INT32, "0")
          .put(Field.Kind.TYPE_UINT32, "0")
          .put(Field.Kind.TYPE_INT64, "''")
          .put(Field.Kind.TYPE_UINT64, "''")
          .put(Field.Kind.TYPE_FLOAT, "0.0")
          .put(Field.Kind.TYPE_DOUBLE, "0.0")
          .build();

  @Override
  protected String arrayTypeName(String elementName) {
    return String.format("%sArray", elementName);
  }

  @Override
  protected String mapTypeName(String keyName, String valueName) {
    return String.format("%sTo%sObject", keyName, valueName);
  }

  @Override
  protected String objectTypeName(String typeName) {
    return upperCamelToLowerCamel(typeName);
  }

  /**
   * Generates placeholder assignment (to end of line) for field of type based on field kind and,
   * for explicitly-formatted strings, format type in {@link ApiaryConfig#stringFormat}.
   */
  public String typeDefaultValue(Type type, Field field, Method method) {
    // Used to handle inconsistency in language detections and translations methods for Translate
    // API. Remove if inconsistency is resolved in discovery docs.
    if (isTranslateLanguageDetectionsOrTranslationsField(method, field)) {
      return stringLiteral("");
    }

    if (field.getCardinality() == Field.Cardinality.CARDINALITY_REPEATED) {
      return isMapField(type, field.getName()) ? "{}" : "[]";
    }
    if (DEFAULT_VALUES.containsKey(field.getKind())) {
      return DEFAULT_VALUES.get(field.getKind());
    }
    if (field.getKind() == Field.Kind.TYPE_STRING || field.getKind() == Field.Kind.TYPE_ENUM) {
      return getDefaultString(type, field).getDefine();
    }
    return "null";
  }

  @Override
  public String stringLiteral(String value) {
    return "'" + value + "'";
  }

  private static final ImmutableMap<String, String> MAP_PARAM_NAME =
      ImmutableMap.<String, String>builder().put("resource", "resource_").build();

  public String mapParamName(String p) {
    return MAP_PARAM_NAME.containsKey(p) ? MAP_PARAM_NAME.get(p) : p;
  }
}
