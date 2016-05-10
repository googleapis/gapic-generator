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
package io.gapi.vgen.go;

import com.google.api.Service;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableTable;
import com.google.protobuf.Field;
import com.google.protobuf.Method;
import com.google.protobuf.Type;

import io.gapi.vgen.ApiaryConfig;
import io.gapi.vgen.DiscoveryContext;

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

  public String typeDefaultValue(Type type, Field field) {
    if (field.getCardinality() == Field.Cardinality.CARDINALITY_REPEATED) {
      return typeName(type, field) + "{}";
    }
    if (DEFAULT_VALUES.containsKey(field.getKind())) {
      return DEFAULT_VALUES.get(field.getKind());
    }
    if (field.getKind() == Field.Kind.TYPE_STRING) {
      String stringFormat = getApiaryConfig().getStringFormat(type.getName(), field.getName());
      if (stringFormat == null) {
        return "\"\"";
      }
      switch (stringFormat) {
        case "byte":
          return "\"\" // base64-encoded string of bytes: see http://tools.ietf.org/html/rfc4648";
        case "date":
          return "\"2006-01-02\" // YYYY-MM-DD";
        case "date-time":
          return "\"2006-01-02T15:04:05Z07:00\" // YYYY-MM-DDThh:mm:ss see time.RFC3339";
      }
      throw new IllegalArgumentException("unknown string format: " + stringFormat);
    }
    return field.getTypeUrl();
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
          .build();

  /**
   * Returns the Go representation of a type's field's type.
   */
  private String typeName(Type type, Field field) {
    String fieldName = field.getName();
    String fieldTypeName = field.getTypeUrl();
    String arrayPrefix = "";

    if (field.getCardinality() == Field.Cardinality.CARDINALITY_REPEATED) {
      Type items = this.getApiaryConfig().getType(fieldTypeName);
      if (isMapField(type, fieldName)) {
        return String.format(
            "map[%s]%s",
            typeName(items, this.getField(items, "key")),
            typeName(items, this.getField(items, "value")));
      }
      Field elements = this.getField(items, "elements");
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

  private static final ImmutableTable<String, String, String> API_VERSION_RENAME =
      ImmutableTable.<String, String, String>builder()
          .put("clouduseraccounts", "beta", "v0.beta")
          .build();

  /**
   * We need this because in some cases there is a mismatch between discovery doc and import path
   * version numbers. API_VERSION_RENAME is a table of API name and versions as found in discovery
   * doc to the renamed versions as found in the import path.
   *
   * TODO(pongad): Find a more sustainable solution to this.
   */
  public String getApiVersion() {
    String rename = API_VERSION_RENAME.get(getApi().getName(), getApi().getVersion());
    return rename == null ? getApi().getVersion() : rename;
  }
}
