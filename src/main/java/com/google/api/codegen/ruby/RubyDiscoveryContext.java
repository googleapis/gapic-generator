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

import com.google.api.Service;
import com.google.api.codegen.ApiaryConfig;
import com.google.api.codegen.DiscoveryContext;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.Field;
import com.google.protobuf.Method;
import com.google.protobuf.Type;

/** A DiscoveryContext specialized for Ruby. */
public class RubyDiscoveryContext extends DiscoveryContext implements RubyContext {

  private static final RubyApiaryNameMap apiaryNameMap = new RubyApiaryNameMap();

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

  @Override
  protected String mapTypeName(String keyName, String valueName) {
    return String.format("%s_to_%s_hash", keyName, valueName);
  }

  /**
   * Generates placeholder assignment (to end of line) for field of type based on field kind and,
   * for explicitly-formatted strings, format type in {@link ApiaryConfig#stringFormat}.
   */
  public String typeDefaultValue(Type type, Field field) {
    if (field.getCardinality() == Field.Cardinality.CARDINALITY_REPEATED) {
      return isMapField(type, field.getName()) ? "{}" : "[]";
    }
    if (DEFAULT_VALUES.containsKey(field.getKind())) {
      return DEFAULT_VALUES.get(field.getKind());
    }
    if (field.getKind() == Field.Kind.TYPE_STRING || field.getKind() == Field.Kind.TYPE_ENUM) {
      return getDefaultString(type, field).getDefine();
    }
    return "nil";
  }

  @Override
  public String stringLiteral(String value) {
    return "'" + value + "'";
  }

  /**
   * Returns Ruby user-friendly name for the request type of method. Eg, analytics.v3's
   * AnalyticsDataimportDeleteUploadDataRequest becomes delete_upload_data_request
   */
  public String getRequestTypeName(Method method) {
    String type = getRequestField(method).getTypeUrl();
    String name = getNameFromMap(type);
    if (name == null) {
      throw new IllegalArgumentException(
          String.format(
              "Ruby name not found: %s::%s::%s", getApi().getName(), getApi().getVersion(), type));
    }
    return name;
  }

  /**
   * Returns Ruby user-friendly name for the given param. Eg, storage.v1's
   * Bucket/cors/cors_configuration/method becomes http_method
   */
  public String getParamName(Method method, String param) {
    String rename = getNameFromMap(method.getName() + "/" + param);
    if (rename == null) {
      throw new IllegalArgumentException(
          String.format(
              "Ruby name not found: %s::%s::%s::%s",
              getApi().getName(), getApi().getVersion(), method.getName(), param));
    }
    return rename;
  }

  /**
   * Returns Ruby user-friendly name for the given method. Eg, analytics.v3's
   * analytics.management.accountSummaries.list becomes list_account_summaries
   */
  public String getMethodName(Method method) {
    String name = getNameFromMap(method.getName());
    if (name == null) {
      throw new IllegalArgumentException(
          String.format(
              "Ruby name not found: %s::%s::%s",
              getApi().getName(), getApi().getVersion(), method.getName()));
    }
    return name;
  }

  private String getNameFromMap(String resourceName) {
    return apiaryNameMap.getName(getApi().getName(), getApi().getVersion(), resourceName);
  }

  public String getApiVersion() {
    return getApi().getVersion().replace('.', '_');
  }

  // Services in Discovery Doc are named in all lowercase. For multi-word names, it is not
  // generally possible for us to capitalize properly. We store the properly capitalized names here
  // so we can generate samples properly.
  private static final ImmutableMap<String, String> SERVICE_RENAME =
      ImmutableMap.<String, String>builder()
          .put("clouddebugger", "CloudDebugger")
          .put("cloudmonitoring", "CloudMonitoring")
          .put("cloudresourcemanager", "CloudResourceManager")
          .put("cloudtrace", "CloudTrace")
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
