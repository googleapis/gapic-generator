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
package com.google.api.codegen.java;

import com.google.api.Service;
import com.google.api.client.util.DateTime;
import com.google.api.codegen.ApiaryConfig;
import com.google.api.codegen.DiscoveryContext;
import com.google.api.codegen.DiscoveryImporter;
import com.google.common.base.Defaults;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.protobuf.Api;
import com.google.protobuf.Field;
import com.google.protobuf.Method;
import com.google.protobuf.Type;

import java.util.List;

/**
 * A DiscoveryContext specialized for Java.
 */
public class JavaDiscoveryContext extends DiscoveryContext implements JavaContext {

  /**
   * The prefix for Java API client libraries.
   */
  private static final String JAVA_SERVICE_TYPE_PREFIX = "com.google.api.services.";

  /**
   * A map from primitive field types used by DiscoveryImporter to Java counterparts.
   */
  private static final ImmutableMap<Field.Kind, String> FIELD_TYPE_MAP =
      ImmutableMap.<Field.Kind, String>builder()
          .put(Field.Kind.TYPE_UNKNOWN, "java.lang.Object")
          // TODO(tcoffee): check validity on actual cases
          .put(Field.Kind.TYPE_BOOL, "boolean")
          .put(Field.Kind.TYPE_INT32, "int")
          .put(Field.Kind.TYPE_UINT32, "int")
          .put(Field.Kind.TYPE_INT64, "long")
          .put(Field.Kind.TYPE_UINT64, "long")
          .put(Field.Kind.TYPE_FLOAT, "float")
          .put(Field.Kind.TYPE_DOUBLE, "double")
          .put(Field.Kind.TYPE_STRING, "java.lang.String")
          .build();

  /**
   * A map from types to renamed counterparts in Java client libraries.
   */
  private static final ImmutableMap<String, String> RENAMED_TYPE_MAP =
      ImmutableMap.<String, String>builder()
          .put("storage.model.Object", "storage.model.StorageObject")
          .put("SQLAdmin.Instances.Import", "SQLAdmin.Instances.SQLAdminImport")
          .build();

  /**
   * A map from unboxed Java primitive type name to corresponding class reference.
   */
  private static final ImmutableMap<String, Class<?>> PRIMITIVE_CLASS_MAP =
      ImmutableMap.<String, Class<?>>builder()
          .put("boolean", boolean.class)
          .put("int", int.class)
          .put("long", long.class)
          .put("float", float.class)
          .put("double", double.class)
          .build();

  private static final ImmutableMap<String, String> RENAMED_METHOD_MAP =
      ImmutableMap.<String, String>builder()
          .put("sql.instances.import", "sql.instances.sqladminImport")
          .build();

  // TODO(tcoffee): revisit default capitalization behavior based on wider survey of APIs
  /**
   * A map from inferred API package names to renamed counterparts in Java client libraries.
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
   * A set of names of APIs whose package paths include their version number.
   */
  private static final ImmutableSet<String> VERSIONED_PACKAGE_SET =
      ImmutableSet.<String>builder()
          .add("clouddebugger")
          .add("logging")
          .add("cloudtrace")
          .add("storagetransfer")
          .add("vision")
          .build();

  private JavaContextCommon javaCommon;

  /**
   * Constructs an instance.
   */
  public JavaDiscoveryContext(Service service, ApiaryConfig apiaryConfig) {
    super(service, apiaryConfig);
  }

  @Override
  public void resetState(JavaSnippetSet<?> javaSnippetSet, JavaContextCommon javaCommon) {
    this.javaCommon = javaCommon;
  }

  public JavaContextCommon java() {
    return javaCommon;
  }

  // Snippet Helpers
  // ===============

  @Override
  public String getMethodName(Method method) {
    return getSimpleName(getRename(method.getName(), RENAMED_METHOD_MAP));
  }

  private String getTypeRename(String typeName) {
    boolean full = typeName.startsWith(JAVA_SERVICE_TYPE_PREFIX);
    if (full) {
      typeName = typeName.substring(JAVA_SERVICE_TYPE_PREFIX.length());
    }
    typeName = getRename(typeName, RENAMED_TYPE_MAP);
    if (full) {
      return JAVA_SERVICE_TYPE_PREFIX + typeName;
    } else {
      return typeName;
    }
  }

  /**
   * Takes a fully-qualified type name and returns its simple name, and also saves the type in the
   * import list.
   */
  public String getTypeName(String typeName) {
    int lastDotIndex = typeName.lastIndexOf('.');
    if (lastDotIndex < 0) {
      throw new IllegalArgumentException("expected fully qualified name");
    }
    typeName = getTypeRename(typeName);
    String shortTypeName = typeName.substring(lastDotIndex + 1);
    return javaCommon.getMinimallyQualifiedName(typeName, shortTypeName);
  }

  /**
   * Adds the given type name to the import list. Returns an empty string so that the output is not
   * affected in snippets.
   */
  public String addImport(String typeName) {
    // used for its side effect of adding the type to the import list if the short name
    // hasn't been imported yet
    getTypeName(typeName);
    return "";
  }

  /*
   * Returns the root URL prefix for the API Java Client Library.
   */
  public String getApiRootUrl() {
    Api api = this.getApi();
    String apiName = api.getName();
    String apiRoot = JAVA_SERVICE_TYPE_PREFIX + apiName + ".";
    if (VERSIONED_PACKAGE_SET.contains(apiName)) {
      return apiRoot + api.getVersion() + ".";
    } else {
      return apiRoot;
    }
  }

  /*
   * Returns the simple name of the top-level API package, and also saves it
   * in the import list.
   */
  public String getApiPackage() {
    return getTypeName(
        getApiRootUrl()
            + getRename(lowerCamelToUpperCamel(this.getApi().getName()), RENAMED_PACKAGE_MAP));
  }

  /*
   * Returns the package-qualified name of the client request object type for the method
   * with given ID, and also saves the corresponding package in the import list.
   */
  public String getClientRequestType(Method method) {
    String methodName = method.getName();
    StringBuilder typeName = new StringBuilder(getApiPackage());
    for (String resourceName : getApiaryConfig().getResources(methodName)) {
      typeName.append("." + lowerCamelToUpperCamel(resourceName));
    }
    typeName.append("." + lowerCamelToUpperCamel(getSimpleName(methodName)));
    return getTypeRename(typeName.toString());
  }

  /*
   * Takes a model reference type name and returns its simple name, and also saves the
   * type in the import list.
   */
  public String getTypeUrl(String typeUrl) {
    return getTypeName(getApiRootUrl() + "model." + typeUrl);
  }

  /*
   * Takes a field and returns the simple name of its model reference type, and also saves the
   * type in the import list.
   */
  public String getTypeUrl(Field field) {
    return getTypeUrl(field.getTypeUrl());
  }

  /**
   * Returns the Java representation of the type of a field of a type.
   */
  public String typeName(Type type, Field field) {
    String fieldName = field.getName();
    String fieldTypeName = field.getTypeUrl();
    if (field.getCardinality() == Field.Cardinality.CARDINALITY_REPEATED) {
      Type items = this.getApiaryConfig().getType(fieldTypeName);
      if (isMapField(type, fieldName)) {
        return String.format(
            "%s<%s, %s>",
            getTypeName("java.util.Map"),
            typeName(items, this.getField(items, "key")),
            typeName(items, this.getField(items, "value")));
      } else {
        return String.format("%s<%s>", getTypeName("java.util.List"), elementTypeName(field));
      }
    } else {
      if (field.getKind() == Field.Kind.TYPE_MESSAGE) {
        return getTypeUrl(fieldTypeName);
      } else {
        return basicTypeName(field);
      }
    }
  }

  /**
   * Returns the Java representation of the type of an element of a repeated field of a type.
   */
  public String elementTypeName(Type type, Field field) {
    String fieldName = field.getName();
    String fieldTypeName = field.getTypeUrl();
    Type items = this.getApiaryConfig().getType(fieldTypeName);
    if (isMapField(type, fieldName)) {
      return String.format(
          "%s.Entry<%s, %s>",
          getTypeName("java.util.Map"),
          typeName(items, this.getField(items, "key")),
          typeName(items, this.getField(items, "value")));
    } else {
      return elementTypeName(field);
    }
  }

  /**
   * Returns the Java representation of the type of an element of an array field.
   */
  private String elementTypeName(Field field) {
    String fieldTypeName = field.getTypeUrl();
    Type items = this.getApiaryConfig().getType(fieldTypeName);
    String elementTypeName;
    if (field.getKind() == Field.Kind.TYPE_MESSAGE) {
      Field elements = this.getField(items, DiscoveryImporter.ELEMENTS_FIELD_NAME);
      if (elements != null) {
        elementTypeName = typeName(items, this.getField(items, "elements"));
      } else {
        elementTypeName = getTypeUrl(fieldTypeName);
      }
    } else {
      elementTypeName = basicTypeNameBoxed(field);
    }
    return elementTypeName;
  }

  /**
   * Returns the Java representation of the type of a basic-typed field, in boxed form.
   */
  public String basicTypeNameBoxed(Field field) {
    return javaCommon.boxedTypeName(basicTypeName(field));
  }

  /**
   * Returns the Java representation of the type of a basic-typed field. If the type is a Java
   * primitive, basicTypeName returns it in unboxed form.
   */
  public String basicTypeName(Field field) {
    String result = FIELD_TYPE_MAP.get(field.getKind());
    if (result != null) {
      if (result.contains(".")) {
        // Fully qualified type name, use regular type name resolver. Can skip boxing logic
        // because those types are already boxed.
        return getTypeName(result);
      }
      return result;
    }
    throw new IllegalArgumentException("unknown type kind: " + field.getKind());
  }

  /**
   * Generates placeholder assignment (to end of line) for field of type based on field kind and,
   * for explicitly-formatted strings, format type in {@link ApiaryConfig#stringFormat}.
   */
  public String typeDefaultValue(Type type, Field field) {
    if (field.getCardinality() == Field.Cardinality.CARDINALITY_REPEATED) {
      String fieldTypeName = field.getTypeUrl();
      Type items = this.getApiaryConfig().getType(fieldTypeName);
      if (isMapField(type, field.getName())) {
        return String.format(
            "new %s<%s, %s>();",
            getTypeName("java.util.HashMap"),
            typeName(items, this.getField(items, "key")),
            typeName(items, this.getField(items, "value")));
      } else {
        return String.format(
            "new %s<%s>();", getTypeName("java.util.ArrayList"), elementTypeName(field));
      }
    } else {
      String typeName = FIELD_TYPE_MAP.get(field.getKind());
      if (typeName != null) {
        Class<?> primitiveClass = PRIMITIVE_CLASS_MAP.get(typeName);
        if (primitiveClass != null) {
          return String.valueOf(Defaults.defaultValue(primitiveClass)) + ";";
        }
        if (typeName.equals("java.lang.String")) {
          String stringFormat = getApiaryConfig().getStringFormat(type.getName(), field.getName());
          if (stringFormat != null) {
            switch (stringFormat) {
              case "byte":
                return "\"\"; "
                    + "  // base64-encoded string of bytes: see http://tools.ietf.org/html/rfc4648 ";
              case "date":
                // TODO(tcoffee): does new DateTime(new Date(0L)).toStringRfc3339() work?
                return "\"1969-12-31\";"
                    + "  // \"YYYY-MM-DD\": "
                    + "see java.text.SimpleDateFormat";
              case "date-time":
                return "\""
                    + new DateTime(0L).toStringRfc3339()
                    + "\";"
                    + "  // \"YYYY-MM-DDThh:mm:ss.fffZ\" (UTC): "
                    + "see com.google.api.client.util.DateTime.toStringRfc3339()";
              default:
                // fall through
            }
          }
          return "\"\";";
        }
      }
      return "null;";
    }
  }

  @Override
  public boolean isResponseEmpty(Method method) {
    return super.isResponseEmpty(method) || method.getResponseTypeUrl().equals("Empty");
  }

  // Flaggers for Exceptional Inconsistencies
  // ========================================

  // used to handle inconsistency in list methods for Cloud Monitoring API
  // remove if inconsistency is resolved in discovery docs
  private boolean isCloudMonitoringListMethod(Method method) {
    Api api = getApi();
    return api.getName().equals("cloudmonitoring")
        && api.getVersion().equals("v2beta2")
        && isPageStreaming(method);
  }

  // used to handle inconsistency in log entries list method for Logging API
  // remove if inconsistency is resolved
  public boolean isLogEntriesListMethod(Method method) {
    Api api = getApi();
    return api.getName().equals("logging")
        && api.getVersion().equals("v2beta1")
        && method.getName().equals("logging.entries.list");
  }

  // used to handle inconsistency in users list method for SQLAdmin API
  // remove if inconsistency is resolved
  private boolean isSQLAdminUsersListMethod(Method method) {
    Api api = getApi();
    return api.getName().equals("sqladmin")
        && api.getVersion().equals("v1beta4")
        && method.getName().equals("sql.users.list");
  }

  @Override
  public boolean hasRequestField(Method method) {
    // used to handle inconsistency in list methods for Cloud Monitoring API
    // remove if inconsistency is resolved in discovery docs
    if (isCloudMonitoringListMethod(method)) {
      return false;
    }
    return super.hasRequestField(method);
  }

  @Override
  public List<String> getMethodParams(Method method) {
    // used to handle inconsistency in list methods for Cloud Monitoring API
    // remove if inconsistency is resolved in discovery docs
    if (isCloudMonitoringListMethod(method)) {
      return getMost(getApiaryConfig().getMethodParams(method.getName()));
    }

    return super.getMethodParams(method);
  }

  @Override
  public boolean isPageStreaming(Method method) {
    // used to handle inconsistency in users list method for SQLAdmin API
    // remove if inconsistency is resolved
    if (isSQLAdminUsersListMethod(method)) {
      return false;
    }
    return super.isPageStreaming(method);
  }
}
