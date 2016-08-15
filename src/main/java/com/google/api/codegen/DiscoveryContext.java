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

import com.google.api.client.util.DateTime;
import com.google.api.codegen.discovery.DefaultString;
import com.google.api.Service;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.protobuf.Api;
import com.google.protobuf.Field;
import com.google.protobuf.Method;
import com.google.protobuf.Type;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

/**
 * A CodegenContext that provides helpers specific to the Discovery use case.
 */
public abstract class DiscoveryContext extends CodegenContext {

  private final Service service;
  private final ApiaryConfig apiaryConfig;

  /**
   * Constructs an abstract instance.
   */
  protected DiscoveryContext(Service service, ApiaryConfig apiaryConfig) {
    this.service = Preconditions.checkNotNull(service);
    this.apiaryConfig = Preconditions.checkNotNull(apiaryConfig);
  }

  /**
   * Returns the associated service.
   */
  public Service getService() {
    return service;
  }

  /**
   * Returns the associated config.
   */
  public ApiaryConfig getApiaryConfig() {
    return apiaryConfig;
  }

  // Helpers for Subclasses and Snippets
  // ===================================

  // Note the below methods are instance-based, even if they don't depend on instance state,
  // so they can be accessed by templates.

  public Api getApi() {
    return this.getService().getApis(0);
  }

  public String getApiRevision() {
    return service.getDocumentation().getOverview();
  }

  /**
   * Returns the simple name of the method with given ID.
   */
  public String getMethodName(Method method) {
    return getSimpleName(method.getName());
  }

  public String getSimpleName(String name) {
    return name.substring(name.lastIndexOf('.') + 1);
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
    // used to handle inconsistency in users list method for SQLAdmin API
    // remove if inconsistency is resolved
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
   * Returns language-specific syntax of line ending with given value.
   */
  public String lineEnding(String value) {
    return value + ";";
  }

  /*
   * Returns language-specific syntax of line commented with given comment string.
   */
  public String lineComment(String line, String comment) {
    return line + "  // " + comment;
  }

  /*
   * Returns default string (to end of line) for a type's string field. If ApiaryConfig specifies a
   * string format, returns corresponding default string with inline comment. Otherwise, if
   * ApiaryConfig specifies a pattern, returns corresponding default string. Otherwise, returns
   * empty string.
   */
  protected String getDefaultString(Type type, Field field) {
    String stringFormat = getApiaryConfig().getStringFormat(type.getName(), field.getName());
    if (stringFormat != null) {
      switch (stringFormat) {
        case "byte":
          return lineComment(
              lineEnding(stringLiteral("")),
              "base64-encoded string of bytes: see http://tools.ietf.org/html/rfc4648");
        case "date":
          return lineComment(
              lineEnding(stringLiteral("1969-12-31")),
              stringLiteral("YYYY-MM-DD") + ": see java.text.SimpleDateFormat");
        case "date-time":
          return lineComment(
              lineEnding(stringLiteral(new DateTime(0L).toStringRfc3339())),
              stringLiteral("YYYY-MM-DDThh:mm:ss.fffZ")
                  + " (UTC): see com.google.api.client.util.DateTime.toStringRfc3339()");
        default:
          return lineEnding(stringLiteral(""));
      }
    }
    String stringPattern = getApiaryConfig().getFieldPattern().get(type.getName(), field.getName());
    return lineEnding(stringLiteral(Strings.nullToEmpty(DefaultString.forPattern(stringPattern))));
  }

  // Line wrap `str`, returning a list of lines. Each line in the returned list is guaranteed to not
  // have new line characters. The first line begins with `firstLinePrefix` (defaults to list
  // bullet: "* "), while subsequent lines begin with a hanging indent of equal width.
  public List<String> lineWrapDoc(String str, int maxWidth, String firstLinePrefix) {
    return s_lineWrapDoc(str, maxWidth, firstLinePrefix);
  }

  public List<String> lineWrapDoc(String str, int maxWidth) {
    return lineWrapDoc(str, maxWidth, "* ");
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

  // used to handle inconsistency in list methods for Cloud Monitoring API
  // remove if inconsistency is resolved in discovery docs
  protected boolean isCloudMonitoringListMethod(Method method) {
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
  protected boolean isSQLAdminUsersListMethod(Method method) {
    Api api = getApi();
    return api.getName().equals("sqladmin")
        && api.getVersion().equals("v1beta4")
        && method.getName().equals("sql.users.list");
  }

  // used to handle inconsistency in language detections and translations methods for Translate API
  // remove if inconsistency is resolved
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
