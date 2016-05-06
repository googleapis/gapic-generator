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
import com.google.api.client.util.DateTime;
import com.google.api.codegen.ApiaryConfig;
import com.google.api.codegen.DiscoveryContext;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.Field;
import com.google.protobuf.Method;
import com.google.protobuf.Type;

import java.util.List;

/**
 * A subclass of DiscoveryContext which is specialized for PHP language.
 */
public class PhpDiscoveryContext extends DiscoveryContext implements PhpContext {

  private static final String GOOGLE_SERVICE_PREFIX = "Google_Service_";

  /**
   * A map from inferred API package names to renamed counterparts in PHP client libraries.
   */
  private static final ImmutableMap<String, String> RENAMED_PACKAGE_MAP =
      ImmutableMap.<String, String>builder()
          .put("Cloudmonitoring", "CloudMonitoring")
          .put("Cloudresourcemanager", "CloudResourceManager")
          .put("Clouduseraccounts", "CloudUserAccounts")
          .put("Deploymentmanager", "DeploymentManager")
          .put("Sqladmin", "SQLAdmin")
          .build();

  /**
   * A map from primitive type name to corresponding default value string in PHP.
   */
  private static final ImmutableMap<Field.Kind, String> DEFAULT_PRIMITIVE_VALUE =
      ImmutableMap.<Field.Kind, String>builder()
          .put(Field.Kind.TYPE_BOOL, "false")
          .put(Field.Kind.TYPE_INT32, "0")
          .put(Field.Kind.TYPE_UINT32, "0")
          .put(Field.Kind.TYPE_INT64, "0")
          .put(Field.Kind.TYPE_UINT64, "0")
          .put(Field.Kind.TYPE_FLOAT, "0.0")
          .put(Field.Kind.TYPE_DOUBLE, "0.0")
          .build();

  /**
   * Constructs the PHP discovery context.
   */
  public PhpDiscoveryContext(Service service, ApiaryConfig apiaryConfig) {
    super(service, apiaryConfig);
  }

  @Override
  public void resetState(PhpSnippetSet<?> phpSnippetSet, PhpContextCommon phpCommon) {
    // TODO implement when necessary
  }

  // Snippet Helpers
  // ===============

  @Override
  public String getMethodName(Method method) {
    String name = super.getMethodName(method);
    List<String> resources = getApiaryConfig().getResources(method.getName());
    if (resources != null && resources.size() > 0) {
      name = name + lowerCamelToUpperCamel(resources.get(resources.size() -1));
    }
    return name;
  }

  /**
   * Returns a simple name represents the API in camel case.
   */
  public String getSimpleApiName() {
    return getRename(lowerCamelToUpperCamel(this.getApi().getName()), RENAMED_PACKAGE_MAP);
  }

  /**
   * Returns the service class name in PHP.
   */
  public String getServiceClassName() {
    return GOOGLE_SERVICE_PREFIX + getSimpleApiName();
  }

  /**
   * Generates placeholder value for field of type based on field kind and,
   * for explicitly-formatted strings, format type in {@link ApiaryConfig#stringFormat}.
   */
  public String typeDefaultValue(Type type, Field field) {
    if (field.getCardinality() == Field.Cardinality.CARDINALITY_REPEATED) {
      return "[]";
    } else {
      Field.Kind kind = field.getKind();
      String defaultPrimitiveValue = DEFAULT_PRIMITIVE_VALUE.get(kind);
      if (defaultPrimitiveValue != null) {
        return defaultPrimitiveValue;
      } else if (kind.equals(Field.Kind.TYPE_STRING)) {
        String stringFormat = getApiaryConfig().getStringFormat(type.getName(), field.getName());
        if (stringFormat != null) {
          switch (stringFormat) {
            case "date":
              return "\'1969-12-31\'";
            case "date-time":
              return "\'"
                  + new DateTime(0L).toStringRfc3339()
                  + "\'";
            default:
              // Fall through
          }
        }
        return "\'\'";
      }
    }
    return "null";
  }
}
