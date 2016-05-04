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
package com.google.api.codegen.ruby;

import com.google.api.client.util.DateTime;
import com.google.api.Service;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.Field;
import com.google.protobuf.Method;
import com.google.protobuf.Type;

import com.google.api.codegen.ApiaryConfig;
import com.google.api.codegen.DiscoveryContext;

import java.net.URL;

/**
 * A DiscoveryContext specialized for Ruby.
 */
public class RubyDiscoveryContext extends DiscoveryContext {

  private static final RubyNameProvider nameProvider = new RubyNameProvider();

  public RubyDiscoveryContext(Service service, ApiaryConfig apiaryConfig) {
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

  /**
   * Generates placeholder assignment (to end of line) for field of type based on field kind and,
   * for explicitly-formatted strings, format type in {@link ApiaryConfig#stringFormat}.
   */
  public String typeDefaultValue(Type type, Field field, Method method) {
    if (field.getCardinality() == Field.Cardinality.CARDINALITY_REPEATED) {
      return isMapField(type, field.getName()) ? "{}" : "[]";
    }
    if (DEFAULT_VALUES.containsKey(field.getKind())) {
      return DEFAULT_VALUES.get(field.getKind());
    }
    if (field.getKind() == Field.Kind.TYPE_STRING) {
      String stringFormat = getApiaryConfig().getStringFormat(type.getName(), field.getName());
      if (stringFormat != null) {
        switch (stringFormat) {
          case "byte":
            return "'' # base64-encoded string of bytes: see http://tools.ietf.org/html/rfc4648";
          case "date":
            return "'1969-12-31' # 'YYYY-MM-DD'";
          case "date-time":
            return String.format(
                "'%s' // 'YYYY-MM-DDThh:mm:ss.fffZ' (UTC)", new DateTime(0L).toStringRfc3339());
        }
      }
      return "''";
    }
    return "nil";
  }

  public String getRequestTypeName(Method method) {
    String type = getRequestField(method).getTypeUrl();
    String name = getNameFromProvider(type);
    if (name == null) {
      throw new IllegalArgumentException(
          String.format(
              "Ruby name not found: %s::%s::%s", getApi().getName(), getApi().getVersion(), type));
    }
    return name;
  }

  public String getParamName(Method method, String param) {
    String rename = getNameFromProvider(method.getName() + "/" + param);
    if (rename == null) {
      throw new IllegalArgumentException(
          String.format(
              "Ruby name not found: %s::%s::%s::%s",
              getApi().getName(),
              getApi().getVersion(),
              method.getName(),
              param));
    }
    return rename;
  }

  public String getMethodName(Method method) {
    String name = getNameFromProvider(method.getName());
    if (name == null) {
      throw new IllegalArgumentException(
          String.format(
              "Ruby name not found: %s::%s::%s",
              getApi().getName(),
              getApi().getVersion(),
              method.getName()));
    }
    return name;
  }

  private String getNameFromProvider(String resourceName) {
    return nameProvider.getName(getApi().getName(), getApi().getVersion(), resourceName);
  }

  public String getApiVersion() {
    return getApi().getVersion().replace('.', '_');
  }

  private static final ImmutableMap<String, String> SERVICE_RENAME =
      ImmutableMap.<String, String>builder()
          .put("cloudmonitoring", "CloudMonitoring")
          .put("cloudresourcemanager", "CloudResourceManager")
          .put("clouduseraccounts", "CloudUserAccounts")
          .put("deploymentmanager", "DeploymentManager")
          .put("sqladmin", "SQLAdmin")
          .build();

  public String getServiceName() {
    String name = getApi().getName();
    if (SERVICE_RENAME.containsKey(name)) {
      name = SERVICE_RENAME.get(name);
    } else {
      name = lowerCamelToUpperCamel(name);
    }
    return name + "Service";
  }
}
