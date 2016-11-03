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
package com.google.api.codegen.php;

import com.google.api.Service;
import com.google.api.codegen.ApiaryConfig;
import com.google.api.codegen.DiscoveryContext;
import com.google.api.codegen.util.php.PhpTypeTable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.protobuf.Field;
import com.google.protobuf.Method;
import com.google.protobuf.Type;
import java.util.List;

/** A subclass of DiscoveryContext which is specialized for PHP language. */
public class PhpDiscoveryContext extends DiscoveryContext implements PhpContext {

  private static final String GOOGLE_SERVICE_PREFIX = "Google_Service_";

  private PhpTypeTable phpTypeTable;

  /** A map from primitive type name to corresponding default value string in PHP. */
  private static final ImmutableMap<Field.Kind, String> DEFAULT_PRIMITIVE_VALUE =
      ImmutableMap.<Field.Kind, String>builder()
          .put(Field.Kind.TYPE_BOOL, "false")
          .put(Field.Kind.TYPE_INT32, "0")
          .put(Field.Kind.TYPE_UINT32, "0")
          .put(Field.Kind.TYPE_FLOAT, "0.0")
          .put(Field.Kind.TYPE_DOUBLE, "0.0")
          // As a work-around in Javascript(Javascript uses string to hold 64bit integers)
          // Disovery importer treats number format strings as TYPE_INT64 or TYPE_UINT64
          // depending on the string format.
          .put(Field.Kind.TYPE_INT64, "\'0\'")
          .put(Field.Kind.TYPE_UINT64, "\'0\'")
          .build();

  /**
   * A set that contains the method names that have extra suffix in the PHP client code. Some PHP
   * methods appends resource path in camel case, e.g. list -> listAppResources
   */
  private static final ImmutableSet<String> RENAMED_METHODS =
      ImmutableSet.<String>builder().add("list").add("clone").build();

  /**
   * A map that maps the original request class name to its renamed version used in PHP client code.
   */
  private static final ImmutableMap<String, String> RENAMED_REQUESTS =
      ImmutableMap.<String, String>builder()
          .put("Google_Service_Storage_Object", "Google_Service_Storage_StorageObject")
          .build();

  /** Constructs the PHP discovery context. */
  public PhpDiscoveryContext(Service service, ApiaryConfig apiaryConfig) {
    super(service, apiaryConfig);
  }

  @Override
  public void resetState(PhpTypeTable phpTypeTable) {
    this.phpTypeTable = phpTypeTable;
  }

  public PhpTypeTable php() {
    return this.phpTypeTable;
  }

  // Snippet Helpers
  // ===============

  @Override
  /** Returns the method names used in the PHP client code. */
  public String getMethodName(Method method) {
    StringBuilder builder = new StringBuilder(super.getMethodName(method));
    if (RENAMED_METHODS.contains(super.getMethodName(method))) {
      List<String> resources = getApiaryConfig().getResources(method.getName());
      for (String resource : resources) {
        builder.append(lowerCamelToUpperCamel(resource));
      }
    }
    return builder.toString();
  }

  /**
   * Returns the request class name of the given method in PHP. If the method does not have a
   * request returns an empty string.
   */
  public String getRequestClassName(Method method) {
    if (hasRequestField(method)) {
      String baseType = getRequestField(method).getTypeUrl();
      String requestName = getServiceClassName() + "_" + baseType;
      if (RENAMED_REQUESTS.containsKey(requestName)) {
        requestName = RENAMED_REQUESTS.get(requestName);
      }
      return requestName;
    }
    return "";
  }

  /** Returns the service class name in PHP. */
  public String getServiceClassName() {
    return GOOGLE_SERVICE_PREFIX + getServiceName();
  }

  @Override
  protected String mapTypeName(String keyName, String valueName) {
    return String.format("%s_to_%s_array", keyName, valueName);
  }

  /**
   * Generates placeholder value for field of type based on field kind and, for explicitly-formatted
   * strings, format type in {@link ApiaryConfig#stringFormat}.
   */
  public String typeDefaultValue(Type type, Field field) {
    if (field.getCardinality() == Field.Cardinality.CARDINALITY_REPEATED) {
      return "[]";
    } else {
      Field.Kind kind = field.getKind();
      String defaultPrimitiveValue = DEFAULT_PRIMITIVE_VALUE.get(kind);
      if (defaultPrimitiveValue != null) {
        return defaultPrimitiveValue;
      } else if (kind.equals(Field.Kind.TYPE_STRING) || kind.equals(Field.Kind.TYPE_ENUM)) {
        return getDefaultString(type, field).getDefine();
      }
    }
    return "null";
  }

  @Override
  public String stringLiteral(String value) {
    return "'" + value + "'";
  }
}
