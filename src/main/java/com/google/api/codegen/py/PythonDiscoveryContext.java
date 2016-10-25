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
package com.google.api.codegen.py;

import com.google.api.Service;
import com.google.api.codegen.ApiaryConfig;
import com.google.api.codegen.DiscoveryContext;
import com.google.api.codegen.DiscoveryImporter;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.Field;
import com.google.protobuf.Method;
import com.google.protobuf.Type;
import java.util.List;

/** A DiscoveryContext specialized for Python. */
public class PythonDiscoveryContext extends DiscoveryContext {

  /** A map from primitive field types used by DiscoveryImporter to Python counterparts. */
  private static final ImmutableMap<Field.Kind, String> FIELD_TYPE_MAP =
      ImmutableMap.<Field.Kind, String>builder()
          .put(Field.Kind.TYPE_UNKNOWN, "dict")
          // TODO(tcoffee): check validity on actual cases
          .put(Field.Kind.TYPE_BOOL, "bool")
          .put(Field.Kind.TYPE_INT32, "int")
          .put(Field.Kind.TYPE_UINT32, "int")
          .put(Field.Kind.TYPE_INT64, "long")
          .put(Field.Kind.TYPE_UINT64, "long")
          .put(Field.Kind.TYPE_FLOAT, "float")
          .put(Field.Kind.TYPE_DOUBLE, "float")
          .put(Field.Kind.TYPE_STRING, "str")
          .put(Field.Kind.TYPE_ENUM, "str")
          .build();

  /** A map from Python native type name to corresponding default value. */
  private static final ImmutableMap<String, String> NATIVE_DEFAULT_MAP =
      ImmutableMap.<String, String>builder()
          .put("dict", "{}")
          .put("bool", "False")
          .put("int", "0")
          .put("long", "str(0L)")
          .put("float", "0.0")
          .build();

  private static final ImmutableMap<String, String> RENAMED_METHOD_MAP =
      ImmutableMap.<String, String>builder().build();

  private final PythonContextCommon pythonCommon;

  /** Constructs the Python discovery context. */
  public PythonDiscoveryContext(Service service, ApiaryConfig apiaryConfig) {
    super(service, apiaryConfig);
    pythonCommon = new PythonContextCommon();
  }

  public PythonContextCommon python() {
    return pythonCommon;
  }

  // Snippet Helpers
  // ===============

  public String silent(String any) {
    return "";
  }

  @Override
  public String getMethodName(Method method) {
    return getSimpleName(getRename(method.getName(), RENAMED_METHOD_MAP));
  }

  @Override
  protected String arrayTypeName(String elementName) {
    return String.format("%s_list", elementName);
  }

  @Override
  protected String mapTypeName(String keyName, String valueName) {
    return String.format("%s_to_%s_dict", keyName, valueName);
  }

  @Override
  protected String objectTypeName(String typeName) {
    return pythonCommon.wrapIfKeywordOrBuiltIn(upperCamelToLowerUnderscore(typeName));
  }

  /**
   * Generates placeholder assignment (to end of line) for a type's field based on field kind and,
   * for explicitly-formatted strings, format type in {@link ApiaryConfig#stringFormat}.
   */
  public String typeDefaultValue(Type type, Field field, Method method) {
    // Used to handle inconsistency in language detections and translations methods for Translate
    // API. Remove if inconsistency is resolved in discovery docs.
    if (isTranslateLanguageDetectionsOrTranslationsField(method, field)) {
      return stringLiteral("");
    }

    if (field.getCardinality() == Field.Cardinality.CARDINALITY_REPEATED) {
      String fieldTypeName = field.getTypeUrl();
      Type items = getApiaryConfig().getType(fieldTypeName);
      if (isMapField(type, field.getName())) {
        return String.format(
            "{ %s: %s }",
            typeDefaultValue(items, getField(items, "key")),
            typeDefaultValue(items, getField(items, "value")));
      } else {
        return String.format("[ %s ]", elementDefaultValue(type, field));
      }
    }
    return nativeDefaultValue(type, field);
  }

  public String typeDefaultValue(Type type, Field field) {
    return typeDefaultValue(type, field, null);
  }

  private String elementDefaultValue(Type type, Field field) {
    String fieldTypeName = field.getTypeUrl();
    Type items = getApiaryConfig().getType(fieldTypeName);
    if (field.getKind() == Field.Kind.TYPE_MESSAGE) {
      Field elements = getField(items, DiscoveryImporter.ELEMENTS_FIELD_NAME);
      if (elements != null) {
        return typeDefaultValue(items, elements);
      } else {
        return "{}";
      }
    }
    return nativeDefaultValue(type, field);
  }

  private String nativeDefaultValue(Type type, Field field) {
    String typeName = FIELD_TYPE_MAP.get(field.getKind());
    if (typeName != null) {
      String nativeDefault = NATIVE_DEFAULT_MAP.get(typeName);
      if (nativeDefault != null) {
        return nativeDefault;
      }
      if (typeName.equals("str")) {
        return getDefaultString(type, field).getDefine();
      }
    }
    return "None";
  }

  @Override
  public String stringLiteral(String value) {
    return "'" + value + "'";
  }

  // Handlers for Exceptional Inconsistencies
  // ========================================

  @Override
  public boolean hasRequestField(Method method) {
    // Used to handle inconsistency in list methods for Cloud Monitoring API.
    // Remove if inconsistency is resolved in discovery docs.
    if (isCloudMonitoringListMethod(method)) {
      return false;
    }
    return super.hasRequestField(method);
  }

  @Override
  public List<String> getMethodParams(Method method) {
    // Used to handle inconsistency in list methods for Cloud Monitoring API.
    // Remove if inconsistency is resolved in discovery docs.
    if (isCloudMonitoringListMethod(method)) {
      return getMost(getApiaryConfig().getMethodParams(method.getName()));
    }
    return super.getMethodParams(method);
  }
}
