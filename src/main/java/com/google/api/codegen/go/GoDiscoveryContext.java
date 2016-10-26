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
package com.google.api.codegen.go;

import com.google.api.Service;
import com.google.api.codegen.ApiaryConfig;
import com.google.api.codegen.DiscoveryContext;
import com.google.api.codegen.DiscoveryImporter;
import com.google.api.codegen.util.go.GoNameFormatter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.Field;
import com.google.protobuf.Method;
import com.google.protobuf.Type;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GoDiscoveryContext extends DiscoveryContext implements GoContext {
  public GoDiscoveryContext(Service service, ApiaryConfig apiaryConfig) {
    super(service, apiaryConfig);
  }

  private static final ImmutableMap<Field.Kind, String> DEFAULT_VALUES =
      ImmutableMap.<Field.Kind, String>builder()
          .put(Field.Kind.TYPE_BOOL, "false")
          .put(Field.Kind.TYPE_INT32, "int64(0)")
          .put(Field.Kind.TYPE_UINT32, "uint64(0)")
          .put(Field.Kind.TYPE_INT64, "int64(0)")
          .put(Field.Kind.TYPE_UINT64, "uint64(0)")
          .put(Field.Kind.TYPE_FLOAT, "float32(0.0)")
          .put(Field.Kind.TYPE_DOUBLE, "0.0")
          .build();

  @Override
  public String getMethodName(Method method) {
    return lowerCamelToUpperCamel(super.getMethodName(method));
  }

  public boolean isReserved(String name) {
    return GoNameFormatter.RESERVED_IDENTIFIER_SET.contains(name);
  }

  @Override
  protected String arrayTypeName(String elementName) {
    return String.format("%sArray", elementName);
  }

  @Override
  protected String mapTypeName(String keyName, String valueName) {
    return String.format("%sTo%sMap", keyName, valueName);
  }

  @Override
  protected String objectTypeName(String typeName) {
    return upperCamelToLowerCamel(getSimpleName(typeName));
  }

  public String typeDefaultValue(Type type, Field field) {
    if (field.getCardinality() == Field.Cardinality.CARDINALITY_REPEATED) {
      return typeName(type, field) + "{}";
    }
    if (DEFAULT_VALUES.containsKey(field.getKind())) {
      return DEFAULT_VALUES.get(field.getKind());
    }
    if (field.getKind() == Field.Kind.TYPE_STRING || field.getKind() == Field.Kind.TYPE_ENUM) {
      return getDefaultString(type, field).getDefine();
    }
    throw new IllegalArgumentException(
        String.format("not implemented: typeDefaultValue(%s, %s)", type, field));
  }

  private static final ImmutableMap<Field.Kind, String> PRIMITIVE_TYPE =
      ImmutableMap.<Field.Kind, String>builder()
          .put(Field.Kind.TYPE_BOOL, "bool")
          .put(Field.Kind.TYPE_INT32, "int64")
          .put(Field.Kind.TYPE_UINT32, "uint64")
          .put(Field.Kind.TYPE_INT64, "int64")
          .put(Field.Kind.TYPE_UINT64, "uint64")
          .put(Field.Kind.TYPE_FLOAT, "float32")
          .put(Field.Kind.TYPE_DOUBLE, "float64")
          .put(Field.Kind.TYPE_STRING, "string")
          .put(Field.Kind.TYPE_ENUM, "string")
          .build();

  /** Returns the Go representation of a type's field's type. */
  private String typeName(Type type, Field field) {
    String fieldName = field.getName();
    String fieldTypeName = field.getTypeUrl();
    String arrayPrefix = "";

    if (field.getCardinality() == Field.Cardinality.CARDINALITY_REPEATED) {
      Type items = getApiaryConfig().getType(fieldTypeName);
      if (isMapField(type, fieldName)) {
        return String.format(
            "map[%s]%s",
            typeName(items, getField(items, "key")), typeName(items, getField(items, "value")));
      }
      Field elements = getField(items, "elements");
      if (elements != null) {
        return "[]" + typeName(items, elements);
      }
      arrayPrefix = "[]";
    }
    if (field.getKind() == Field.Kind.TYPE_MESSAGE) {
      return arrayPrefix + fieldTypeName;
    }
    if (PRIMITIVE_TYPE.containsKey(field.getKind())) {
      return arrayPrefix + PRIMITIVE_TYPE.get(field.getKind());
    }
    throw new IllegalArgumentException(
        String.format("cannot find suitable type for %s %s", type.getName(), field.getName()));
  }

  @Override
  /**
   * Most languages ignore return type "Empty". However, Go cannot since the client library will
   * return an empty struct, and we have to assign it to something. Consequently, the only time the
   * response is truly "empty" for Go is when DiscoveryImporter says EMPTY_TYPE_NAME, which
   * signifies complete absence of return value.
   */
  public boolean isResponseEmpty(Method method) {
    return method.getResponseTypeUrl().equals(DiscoveryImporter.EMPTY_TYPE_NAME);
  }

  @Override
  public boolean isPageStreaming(Method method) {
    if (isResponseEmpty(method) || hasRequestField(method)) {
      return false;
    }
    boolean hasNextPageToken = false;
    for (Field field : getApiaryConfig().getType(method.getResponseTypeUrl()).getFieldsList()) {
      if (field.getName().equals("nextPageToken")) {
        hasNextPageToken = true;
        break;
      }
    }

    boolean hasPageToken = false;
    for (Field field : getApiaryConfig().getType(method.getRequestTypeUrl()).getFieldsList()) {
      if (field.getName().equals("pageToken")) {
        hasPageToken = true;
        break;
      }
    }
    return hasPageToken && hasNextPageToken;
  }

  // Pattern used to rename some Go package versions
  private static final Pattern SUB_VERSION = Pattern.compile("^(.+)_(v[0-9.]+)$");

  // Some Go package versions rename discovery doc versions:
  // https://github.com/google/google-api-go-client/blob/master/google-api-go-generator/gen.go#L320
  public String getApiVersion() {
    String version = getApi().getVersion();
    if (version.equals("alpha") || version.equals("beta")) {
      return "v0." + version;
    }
    Matcher subVersion = SUB_VERSION.matcher(version);
    if (subVersion.matches()) {
      return subVersion.group(1) + "/" + subVersion.group(2);
    }
    return version;
  }

  /*
   * Returns an empty or singleton list of auth scopes for the method. If the method has no scope,
   * returns an empty list; otherwise returns the first scope. We return an empty list instead of
   * null to denote absence of scope since the snippet cannot handle null values. If the scope
   * exists, it is stripped to its last path-element and converted to camel case, eg
   * "https://www.googleapis.com/auth/cloud-platform" becomes "CloudPlatform".
   */
  public ImmutableList<String> getFormattedAuthScopes(Method method) {
    if (!getApiaryConfig().getAuthScopes().containsKey(method.getName())) {
      return ImmutableList.<String>of();
    }
    String scope = getApiaryConfig().getAuthScopes().get(method.getName()).get(0);
    int slash = scope.lastIndexOf('/');
    if (slash < 0) {
      throw new IllegalArgumentException(
          String.format("malformed scope, cannot find slash: %s", scope));
    }
    scope = scope.substring(slash + 1);
    scope = scope.replace('.', '_');
    scope = scope.replace('-', '_');
    scope = lowerUnderscoreToUpperCamel(scope);
    return ImmutableList.<String>of(scope);
  }
}
