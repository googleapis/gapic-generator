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

import java.util.List;

/**
 * Generates language specific names for types and package paths.
 *
 */
public interface TypeNameGenerator {

  /**
   * Returns the version of the API.
   *
   * Provided in case there is some common transformation on the API's version.
   * For example: "alpha" to "v0.alpha"
   */
  public String getApiVersion(String apiVersion);

  /**
   * Returns the package prefix for the API.
   */
  public String getPackagePrefix(String apiName, String apiVersion);

  /**
   * Returns the API type name.
   *
   * Not fully qualified.
   */
  public String getApiTypeName(String apiName);

  /**
   * Returns the request's type name.
   *
   * Not fully qualified.
   */
  public String getRequestTypeName(List<String> methodNameComponents);

  /**
   * Returns the message's type name.
   *
   * Not fully qualified.
   */
  public String getMessageTypeName(String messageTypeName);

  /**
   * Returns a message's subpackage depending on whether or not it's a request
   * type.
   */
  public String getSubpackage(boolean isRequest);

  /**
   * Returns an example demonstrating the given string format or an empty
   * string if format is unrecognized.
   *
   * If not the empty string, the returned value will be enclosed within the
   * correct language-specific quotes.
   */
  public String getStringFormatExample(String format);

  /**
   * Returns an example demonstrating the given field pattern or an empty
   * string if pattern is invalid.
   *
   * If not the empty string, the returned value will be enclosed within the
   * correct language-specific quotes.
   */
  public String getFieldPatternExample(String pattern);
}
