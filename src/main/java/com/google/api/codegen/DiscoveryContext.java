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
package com.google.api.codegen;

import com.google.api.Service;
import com.google.api.client.util.DateTime;
import com.google.api.codegen.discovery.DefaultString;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.protobuf.Api;
import com.google.protobuf.Field;
import com.google.protobuf.Method;
import com.google.protobuf.Type;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

/** A CodegenContext that provides helpers specific to the Discovery use case. */
public abstract class DiscoveryContext extends CodegenContext {

  private final Service service;
  private final ApiaryConfig apiaryConfig;

  /** Constructs an abstract instance. */
  protected DiscoveryContext(Service service, ApiaryConfig apiaryConfig) {
    this.service = Preconditions.checkNotNull(service);
    this.apiaryConfig = Preconditions.checkNotNull(apiaryConfig);
  }

  /** Returns the associated service. */
  public Service getService() {
    return service;
  }

  /** Returns the associated config. */
  public ApiaryConfig getApiaryConfig() {
    return apiaryConfig;
  }

  // Helpers for Subclasses and Snippets
  // ===================================

  // Note the below methods are instance-based, even if they don't depend on instance state,
  // so they can be accessed by templates.

  public Api getApi() {
    return getService().getApis(0);
  }

  public String getApiRevision() {
    return service.getDocumentation().getOverview();
  }

  /** Returns the simple name of the method with given ID. */
  public String getMethodName(Method method) {
    return getSimpleName(method.getName());
  }

  public String getSimpleName(String name) {
    return name.substring(name.lastIndexOf('.') + 1);
  }

  /**
   * Returns a sample identifier name for a variable of the given type name.
   *
   * <p>May be overridden by individual language contexts.
   */
  public String getSampleVarName(String typeName) {
    return upperCamelToLowerCamel(getSimpleName(typeName));
  }

  /** Returns a name for a type's field's type. */
  public String typeName(Type type, Field field, String name) {
    if (field.getCardinality() == Field.Cardinality.CARDINALITY_REPEATED) {
      if (isMapField(type, field.getName())) {
        return mapTypeName(field);
      } else {
        return arrayTypeName(field);
      }
    } else {
      if (field.getKind() == Field.Kind.TYPE_MESSAGE) {
        return objectTypeName(field);
      } else {
        return nativeTypeName(type, field, name);
      }
    }
  }

  /**
   * Returns a name for an array field's type.
   *
   * <p>May be overridden by individual language contexts.
   */
  protected String arrayTypeName(Field field) {
    return arrayTypeName(elementTypeName(field));
  }

  protected String arrayTypeName(String elementName) {
    return String.format("%s_array", elementName);
  }

  /**
   * Returns a name for a map field's type.
   *
   * <p>May be overridden by individual language contexts.
   */
  protected String mapTypeName(Field field) {
    return mapTypeName(keyTypeName(field), valueTypeName(field));
  }

  protected String mapTypeName(String keyName, String valueName) {
    return String.format("%s_to_%s_map", keyName, valueName);
  }

  /**
   * Returns a name for an object field's type.
   *
   * <p>May be overridden by individual language contexts.
   */
  public String objectTypeName(Field field) {
    return objectTypeName(field.getTypeUrl());
  }

  protected String objectTypeName(String typeName) {
    return upperCamelToLowerUnderscore(typeName);
  }

  /**
   * Returns a name for a natively-typed field's type.
   *
   * <p>May be overridden by individual language contexts.
   */
  protected String nativeTypeName(Type type, Field field, String name) {
    return name;
  }

  /** Returns a name for an array field element's type. */
  public String elementTypeName(Field field) {
    Type items = getApiaryConfig().getType(field.getTypeUrl());
    if (field.getKind() == Field.Kind.TYPE_MESSAGE) {
      Field elements = getField(items, DiscoveryImporter.ELEMENTS_FIELD_NAME);
      if (elements != null) {
        return typeName(items, elements, "item");
      } else {
        return objectTypeName(field);
      }
    }
    return nativeElementTypeName(field);
  }

  /**
   * Returns a name for a natively-typed array field element's type.
   *
   * <p>May be overridden by individual language contexts.
   */
  protected String nativeElementTypeName(Field field) {
    return "item";
  }

  /** Returns a name for a map field key's type. */
  public String keyTypeName(Field field) {
    Type items = getApiaryConfig().getType(field.getTypeUrl());
    return typeName(items, getField(items, "key"), "name");
  }

  /** Returns a name for a map field value's type. */
  public String valueTypeName(Field field) {
    Type items = getApiaryConfig().getType(field.getTypeUrl());
    return typeName(items, getField(items, "value"), "value");
  }

  @Nullable
  public Field getFirstRepeatedField(Type type) {
    for (Field field : type.getFieldsList()) {
      if (field.getCardinality() == Field.Cardinality.CARDINALITY_REPEATED) {
        return field;
      }
    }
    return null;
  }

  @Nullable
  public Field getField(Type type, String fieldName) {
    return apiaryConfig.getField(type, fieldName);
  }

  public boolean isMapField(Type type, String fieldName) {
    return apiaryConfig.getAdditionalProperties(type.getName(), fieldName) != null;
  }

  public boolean isRequestField(String fieldName) {
    return fieldName.equals(DiscoveryImporter.REQUEST_FIELD_NAME);
  }

  public boolean hasRequestField(Method method) {
    List<String> params = apiaryConfig.getMethodParams(method.getName());
    return params.size() > 0 && isRequestField(getLast(params));
  }

  @Nullable
  public Field getRequestField(Method method) {
    return getField(
        apiaryConfig.getType(method.getRequestTypeUrl()), DiscoveryImporter.REQUEST_FIELD_NAME);
  }

  public boolean hasMediaUpload(Method method) {
    return apiaryConfig.getMediaUpload().contains(method.getName());
  }

  public boolean hasMediaDownload(Method method) {
    // ignore media download for methods supporting media upload, as Apiary cannot combine both in
    // single request, and no sensible use cases are known for download with a method supporting
    // upload
    if (hasMediaUpload(method)) {
      return false;
    }
    return apiaryConfig.getMediaDownload().contains(method.getName());
  }

  public boolean hasAuthScopes(Method method) {
    return apiaryConfig.hasAuthScopes(method.getName());
  }

  public List<String> getAuthScopes(Method method) {
    return apiaryConfig.getAuthScopes(method.getName());
  }

  public List<String> getMethodParams(Method method) {
    return apiaryConfig.getMethodParams(method.getName());
  }

  public List<String> getFlatMethodParams(Method method) {
    if (hasRequestField(method)) {
      return getMost(getMethodParams(method));
    } else {
      return getMethodParams(method);
    }
  }

  public boolean isResponseEmpty(Method method) {
    String typeUrl = method.getResponseTypeUrl();
    return typeUrl.equals(DiscoveryImporter.EMPTY_TYPE_NAME) || typeUrl.equals("Empty");
  }

  public boolean isPageStreaming(Method method) {
    // Used to handle inconsistency in users list method for SQLAdmin API.
    // Remove if inconsistency is resolved.
    if (isSQLAdminUsersListMethod(method)) {
      return false;
    }

    if (isResponseEmpty(method)) {
      return false;
    }
    for (Field field : apiaryConfig.getType(method.getResponseTypeUrl()).getFieldsList()) {
      if (field.getName().equals("nextPageToken")) {
        return true;
      }
    }
    return false;
  }

  public boolean isPatch(Method method) {
    return apiaryConfig.getHttpMethod(method.getName()).equals("PATCH");
  }

  /*
   * Returns language-specific syntax of string literal with given value.
   */
  public String stringLiteral(String value) {
    return "\"" + value + "\"";
  }

  /*
   * Returns DefaultString for a type's string field. If ApiaryConfig specifies a string format, it
   * includes a default comment. Otherwise, the comment is empty.
   */
  protected DefaultString getDefaultString(Type type, Field field) {
    String stringFormat = getApiaryConfig().getStringFormat(type.getName(), field.getName());
    if (stringFormat != null) {
      switch (stringFormat) {
        case "byte":
          return new DefaultString(
              stringLiteral(""),
              "base64-encoded string of bytes: see http://tools.ietf.org/html/rfc4648");
        case "date":
          return new DefaultString(
              stringLiteral("1969-12-31"),
              stringLiteral("YYYY-MM-DD") + ": see java.text.SimpleDateFormat");
        case "date-time":
          return new DefaultString(
              stringLiteral(new DateTime(0L).toStringRfc3339()),
              stringLiteral("YYYY-MM-DDThh:mm:ss.fffZ")
                  + " (UTC): see com.google.api.client.util.DateTime.toStringRfc3339()");
        default:
          return new DefaultString(stringLiteral(""), null);
      }
    }
    String stringPattern = getApiaryConfig().getFieldPattern().get(type.getName(), field.getName());
    String def = DefaultString.getPlaceholder(field.getName(), stringPattern);
    String sample = DefaultString.getSample(getApi().getName(), field.getName(), stringPattern);
    if (!Strings.isNullOrEmpty(sample)) {
      sample = stringLiteral(sample);
    }
    return new DefaultString(stringLiteral(def), sample);
  }

  /** Returns the sample string for the given type and field. */
  public String getDefaultSample(Type type, Field field) {
    String sample = getDefaultString(type, field).getComment();
    if (Strings.isNullOrEmpty(sample)) {
      return "";
    }
    return "ex: " + sample;
  }

  /**
   * Returns description of type's field from {@link ApiaryConfig}, or field's name if no
   * description is available.
   */
  public String getDescription(String typeName, String fieldName) {
    String description = apiaryConfig.getDescription(typeName, fieldName);
    if (description != null) {
      return description;
    } else {
      return fieldName;
    }
  }

  /**
   * Line wrap `str`, returning a list of lines. Each line in the returned list is guaranteed to not
   * have new line characters. The first line begins with `firstLinePrefix` (defaults to empty),
   * while subsequent lines begin with a hanging indent of equal width.
   */
  public List<String> lineWrapDoc(String str, int maxWidth, String firstLinePrefix) {
    return s_lineWrapDoc(str, maxWidth, firstLinePrefix);
  }

  public List<String> lineWrapDoc(String str, int maxWidth) {
    return lineWrapDoc(str, maxWidth, "");
  }

  public List<String> lineWrapDoc(String str) {
    return lineWrapDoc(str, 100);
  }

  // For testing.
  public static List<String> s_lineWrapDoc(String str, int maxWidth, String firstLinePrefix) {
    int indentWidth = firstLinePrefix.length();
    String indent = Strings.repeat(" ", indentWidth);
    maxWidth = maxWidth - indentWidth;

    List<String> lines = new ArrayList<>();
    String prefix = firstLinePrefix;

    for (String line : str.trim().split("\n")) {
      line = line.trim();

      while (line.length() > maxWidth) {
        int split = lineWrapIndex(line, maxWidth);
        lines.add(prefix + line.substring(0, split).trim());
        line = line.substring(split).trim();
        prefix = indent;
      }

      if (!line.isEmpty()) {
        lines.add(prefix + line);
      }
      prefix = indent;
    }
    return lines;
  }

  public static List<String> s_lineWrapDoc(String str, int maxWidth) {
    return s_lineWrapDoc(str, maxWidth, "* ");
  }

  private static int lineWrapIndex(String line, int maxWidth) {
    for (int i = maxWidth; i > 0; i--) {
      if (isLineWrapChar(line.charAt(i))) {
        return i;
      }
    }
    for (int i = maxWidth + 1; i < line.length(); i++) {
      if (isLineWrapChar(line.charAt(i))) {
        return i;
      }
    }
    return line.length();
  }

  private static boolean isLineWrapChar(char c) {
    return Character.isWhitespace(c) || "([".indexOf(c) >= 0;
  }

  // Handlers for Exceptional Inconsistencies
  // ========================================

  // Used to handle inconsistency in list methods for Cloud Monitoring API.
  // Remove if inconsistency is resolved in discovery docs.
  protected boolean isCloudMonitoringListMethod(Method method) {
    Api api = getApi();
    return api.getName().equals("cloudmonitoring")
        && api.getVersion().equals("v2beta2")
        && isPageStreaming(method);
  }

  // Used to handle inconsistency in log entries list method for Logging API
  // and organizations search method for CloudResourceManager API.
  // Remove if inconsistency is resolved.
  public boolean isPageTokenInRequestBody(Method method) {
    Api api = getApi();
    return api.getName().equals("logging")
            && api.getVersion().equals("v2beta1")
            && method.getName().equals("logging.entries.list")
        || api.getName().equals("cloudresourcemanager")
            && api.getVersion().equals("v1")
            && method.getName().equals("cloudresourcemanager.organizations.search");
  }

  // Used to handle inconsistency in users list method for SQLAdmin API.
  // Remove if inconsistency is resolved.
  protected boolean isSQLAdminUsersListMethod(Method method) {
    Api api = getApi();
    return api.getName().equals("sqladmin")
        && api.getVersion().equals("v1beta4")
        && method.getName().equals("sql.users.list");
  }

  // Used to handle inconsistency in language detections and translations methods for Translate API.
  // Remove if inconsistency is resolved.
  protected boolean isTranslateLanguageDetectionsOrTranslationsField(Method method, Field field) {
    if (method == null) {
      return false;
    }
    Api api = getApi();
    return (api.getName().equals("translate")
        && api.getVersion().equals("v2")
        && (method.getName().equals("language.detections.list")
            || method.getName().equals("language.translations.list"))
        && field.getName().equals("q"));
  }
}
