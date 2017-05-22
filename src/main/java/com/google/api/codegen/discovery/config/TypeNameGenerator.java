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
package com.google.api.codegen.discovery.config;

import com.google.api.codegen.DiscoveryImporter;
import com.google.api.codegen.LanguageUtil;
import com.google.api.codegen.discovery.DefaultString;
import com.google.common.base.Strings;
import java.util.LinkedList;
import java.util.List;

/** Generates language-specific names for types and package paths. */
public class TypeNameGenerator {

  /** The API's canonical name with all spaces removed. */
  protected String apiCanonicalName;

  /** The API's version. */
  protected String apiVersion;

  /**
   * Returns the language-formatted method name components.
   *
   * <p>For example: "myapi.foo.get" to ["Foo", "Get"]
   */
  public List<String> getMethodNameComponents(List<String> nameComponents) {
    LinkedList<String> copy = new LinkedList<>(nameComponents);
    // Don't edit the original object.
    copy.removeFirst();
    return copy;
  }

  /** Sets the apiCanonicalName and apiVersion used for name lookups and disambiguation. */
  public void setApiCanonicalNameAndVersion(String apiCanonicalName, String apiVersion) {
    this.apiCanonicalName = apiCanonicalName.replaceAll(" ", "");
    this.apiVersion = apiVersion;
  }

  /** Returns language-specific delimiter used for string literals in samples. */
  public String stringDelimiter() {
    return "\"";
  }

  /** Returns string enclosed as language-specific string literal. */
  public String stringLiteral(String s) {
    return stringDelimiter() + s + stringDelimiter();
  }

  /**
   * Returns the version of the API.
   *
   * <p>Provided in case there is some common transformation on the API's version. For example:
   * "alpha" to "v0.alpha"
   */
  public String getApiVersion(String apiVersion) {
    return apiVersion;
  }

  /** Returns the package prefix for the API. */
  public String getPackagePrefix(String apiName, String apiCanonicalName, String apiVersion) {
    return "";
  }

  /**
   * Returns the API type name.
   *
   * <p>Not fully qualified.
   */
  public String getApiTypeName(String canonicalName) {
    return canonicalName.replace(" ", "");
  }

  /**
   * Returns the request's type name.
   *
   * <p>Not fully qualified.
   */
  public String getRequestTypeName(List<String> methodNameComponents) {
    return "";
  }

  /**
   * Returns the responseTypeUrl or an empty string if the response is empty.
   *
   * <p>Provided in case there is some common transformation on the responseTypeUrl.
   */
  public String getResponseTypeUrl(String responseTypeUrl) {
    if (responseTypeUrl.equals(DiscoveryImporter.EMPTY_TYPE_NAME)
        || responseTypeUrl.equals(DiscoveryImporter.EMPTY_TYPE_URL)) {
      return "";
    }
    return responseTypeUrl;
  }

  /**
   * Returns the message's type name.
   *
   * <p>Not fully qualified.
   */
  public String getMessageTypeName(String messageTypeName) {
    return messageTypeName;
  }

  /** Returns a message's subpackage depending on whether or not it's a request type. */
  public String getSubpackage(boolean isRequest) {
    return "";
  }

  /**
   * Returns an example demonstrating the given string format or an empty string if format is
   * unrecognized.
   *
   * <p>If not the empty string, the returned value will be enclosed within the correct
   * language-specific quotes.
   */
  public String getStringFormatExample(String format) {
    return "";
  }

  // Helper for language-specific overrides.
  public String getStringFormatExample(String format, String dateRef, String dateTimeRef) {
    if (Strings.isNullOrEmpty(format)) {
      return "";
    }
    switch (format) {
      case "byte":
        return "Base64-encoded string of bytes: see http://tools.ietf.org/html/rfc4648";
      case "date":
        return stringLiteral("YYYY-MM-DD") + ": see " + dateRef;
      case "date-time":
        return stringLiteral("YYYY-MM-DDThh:mm:ss.fff") + ": see " + dateTimeRef;
      default:
        return "";
    }
  }

  /**
   * Returns a string placeholder demonstrating the field or an empty string if none.
   *
   * <p>If pattern is valid, a path template style string is returned ("foo/my-bar"). If pattern is
   * invalid, and inPath is false, an empty string is returned. If pattern is invalid, and inPath is
   * true, a "my-bar" style string is returned.
   *
   * <p>If not the empty string, the returned value will be enclosed within the correct
   * language-specific quotes.
   */
  public String getStringFieldPlaceholder(String fieldName, String pattern, boolean inPath) {
    String format = "my-%s";
    String val = DefaultString.getNonTrivialPlaceholder(pattern, format);
    if (!Strings.isNullOrEmpty(val)) {
      return stringLiteral(val);
    }
    if (!inPath) {
      return "";
    }
    fieldName = LanguageUtil.lowerCamelToLowerUnderscore(fieldName);
    val = String.format(format, fieldName.replace('_', '-'));
    return stringLiteral(val);
  }

  /** Returns the URL of the discovery doc. */
  public String getDiscoveryDocUrl(String apiName, String apiVersion) {
    return "";
  }
}
